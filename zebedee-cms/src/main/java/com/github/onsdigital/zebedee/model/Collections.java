package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.encryptedfileupload.EncryptedFileItemFactory;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.data.json.DirectoryListing;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.DeleteContentRequestDeniedException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.model.approval.ApprovalQueue;
import com.github.onsdigital.zebedee.model.approval.ApproveTask;
import com.github.onsdigital.zebedee.model.publishing.PostPublisher;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.model.publishing.Publisher;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.model.Content.isDataVisualisationFile;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_APPROVED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_COMPLETED_ERROR;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_CONTENT_DELETED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_CONTENT_MOVED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_CONTENT_RENAMED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_DELETED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_ITEM_COMPLETED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_UNLOCKED;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.contentMoved;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.contentRenamed;

public class Collections {

    public final Path path;
    private PermissionsService permissionsService;
    private Content published;
    private Supplier<Zebedee> zebedeeSupplier = () -> Root.zebedee;
    private CollectionReaderWriterFactory collectionReaderWriterFactory;
    private Function<ApproveTask, Future<Boolean>> addTaskToQueue = ApprovalQueue::add;
    private BiConsumer<Collection, EventType> publishingNotificationConsumer = (c, e) -> new PublishNotification(c).sendNotification(e);
    private Function<Path, ContentReader> contentReaderFactory = FileSystemContentReader::new;
    private Supplier<CollectionHistoryDao> collectionHistoryDaoSupplier = CollectionHistoryDaoFactory::getCollectionHistoryDao;

    public Collections(Path path, PermissionsService permissionsService, Content published) {
        this.path = path;
        this.permissionsService = permissionsService;
        this.published = published;
        this.collectionReaderWriterFactory = new CollectionReaderWriterFactory();
    }

    /**
     * Populate a list of files / folders for a given path.
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static DirectoryListing listDirectory(Path path) throws IOException {
        DirectoryListing listing = new DirectoryListing();
        try (DirectoryStream<Path> stream = Files
                .newDirectoryStream(path)) {
            for (Path directory : stream) {
                if (Files.isDirectory(directory)) {
                    listing.getFolders().put(directory.getFileName().toString(),
                            directory.toString());
                } else {
                    listing.getFiles().put(directory.getFileName().toString(),
                            directory.toString());
                }
            }
        }
        return listing;
    }

    /**
     * Trim the guid part of the collection ID to infer the name
     *
     * @return
     */
    private static String getCollectionNameFromId(String id) {
        try {
            int guidLength = 65; // length of GUID plus the hyphen.
            return id.substring(0, id.length() - guidLength);
        } catch (StringIndexOutOfBoundsException e) {
            return id;
        }
    }

    public static void removeEmptyCollectionDirectories(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            path = path.getParent();
        }
        if (isEmpty(path)) {
            Path temp = path;
            while (!isCollectionRoot(temp.getFileName())) {
                if (isEmpty(temp)) {
                    Files.deleteIfExists(temp);
                }
                temp = temp.getParent();
            }
        }
    }

    private static boolean isCollectionRoot(Path path) {
        return Collection.IN_PROGRESS.equals(path.toString())
                || Collection.REVIEWED.equals(path.toString())
                || Collection.COMPLETE.equals(path.toString());
    }

    private static boolean isEmpty(Path path) {
        File[] files = path.toFile().listFiles();

        if (files == null)
            return true;

        return Arrays.asList(files).isEmpty();
    }

    /**
     * Mark a file in a collection as 'complete'
     *
     * @param collection
     * @param uri
     * @param session
     * @param recursive
     * @throws IOException
     * @throws NotFoundException
     * @throws UnauthorizedException
     * @throws BadRequestException
     */
    public void complete(
            Collection collection, String uri,
            Session session,
            boolean recursive
    ) throws IOException, ZebedeeException {

        // Check the collection
        if (collection == null) {
            throw new BadRequestException("Please specify a valid collection.");
        }

        // Check authorisation
        if (!permissionsService.canEdit(session)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // Locate the path:
        Path path = collection.getInProgress().get(uri);
        if (path == null) {
            throw new NotFoundException("URI not in progress.");
        }

        // Check we're requesting a file:
        if (java.nio.file.Files.isDirectory(path)) {
            throw new BadRequestException("URI does not represent a file.");
        }

        CollectionHistoryEvent historyEvent = new CollectionHistoryEvent(collection, session, null, uri);
        // Attempt to complete:
        if (collection.complete(session.getEmail(), uri, recursive)) {
            removeEmptyCollectionDirectories(path);
            collection.save();
            collectionHistoryDaoSupplier.get().saveCollectionHistoryEvent(historyEvent.eventType
                    (COLLECTION_ITEM_COMPLETED));
        } else {
            collectionHistoryDaoSupplier.get().saveCollectionHistoryEvent(historyEvent.eventType(COLLECTION_COMPLETED_ERROR));
            throw new BadRequestException("URI was not completed.");
        }
    }

    /**
     * @return A list of all {@link Collection}s.
     * @throws IOException If a filesystem error occurs.
     */
    public CollectionList list() throws IOException {

        CollectionList result = new CollectionList();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    try {
                        result.add(new Collection(path, zebedeeSupplier.get()));
                    } catch (CollectionNotFoundException e) {
                        logError(e, "Failed to deserialise collection")
                                .addParameter("collectionPath", path.toString())
                                .log();
                    }
                }
            }
        }

        return result;
    }

    public Map<String, Collection> mapByID() throws IOException {
        return list().stream().collect(Collectors.toMap(
                collection -> collection.getDescription().getId(), collection -> collection));
    }

    /**
     * Get the collection with the given collection ID.
     *
     * @param collectionId
     * @return
     * @throws IOException
     */
    public Collection getCollection(String collectionId)
            throws IOException {
        try {
            String collectionName = getCollectionNameFromId(collectionId);
            Collection collection = getCollectionByName(collectionName);
            return collection;
        } catch (IOException | CollectionNotFoundException e) {
            return list().getCollection(collectionId);
        }
    }

    public Collection getCollectionByName(String collectionName) throws IOException, CollectionNotFoundException {
        return new Collection(this.path.resolve(collectionName), zebedeeSupplier.get());
    }

    /**
     * Approve the given collection.
     * <p>
     * Uses the environment variable use_beta_publisher to choose publisher
     *
     * @param collection
     * @param session
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     * @throws BadRequestException
     * @throws ConflictException
     */
    public Future<Boolean> approve(Collection collection, Session session)
            throws IOException, ZebedeeException {

        // Collection exists
        if (collection == null) {
            throw new BadRequestException("Please provide a valid collection.");
        }

        // User has permission
        if (session == null || !permissionsService.canEdit(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // Everything is completed
        if (!collection.inProgressUris().isEmpty()
                || !collection.completeUris().isEmpty()) {
            throw new ConflictException(
                    "This collection can't be approved because it's not empty");
        }

        CollectionReader collectionReader = collectionReaderWriterFactory.getReader(zebedeeSupplier.get(), collection, session);
        CollectionWriter collectionWriter = collectionReaderWriterFactory.getWriter(zebedeeSupplier.get(), collection, session);
        ContentReader publishedReader = contentReaderFactory.apply(this.published.path);

        collection.getDescription().setApprovalStatus(ApprovalStatus.IN_PROGRESS);
        collection.save();

        Future<Boolean> future = addTaskToQueue.apply(
                new ApproveTask(collection, session, collectionReader, collectionWriter, publishedReader,
                        zebedeeSupplier.get().getDataIndex()));

        collectionHistoryDaoSupplier.get().saveCollectionHistoryEvent(collection, session, COLLECTION_APPROVED);
        return future;
    }

    /**
     * Unlock the given collection.
     *
     * @param collection
     * @param session
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     * @throws BadRequestException
     * @throws ConflictException
     */
    public boolean unlock(Collection collection, Session session)
            throws IOException, ZebedeeException {

        // Collection exists
        if (collection == null) {
            throw new BadRequestException("Please provide a valid collection.");
        }

        // User has permission
        if (session == null || !permissionsService.canEdit(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // don't do anything if the approval status is not complete.
        if (collection.getDescription().getApprovalStatus() != ApprovalStatus.COMPLETE) {
            return true;
        }

        // Go ahead
        collection.getDescription().setApprovalStatus(ApprovalStatus.NOT_STARTED);
        collection.getDescription().addEvent(new Event(new Date(), EventType.UNLOCKED, session.getEmail()));
        collectionHistoryDaoSupplier.get().saveCollectionHistoryEvent(collection, session, COLLECTION_UNLOCKED);

        publishingNotificationConsumer.accept(collection, EventType.UNLOCKED);
        return collection.save();
    }

    /**
     * Publish the files
     *
     * @param collection       the collection to publish
     * @param session          a session with editor priviledges
     * @param skipVerification
     * @return success
     * @throws IOException
     * @throws UnauthorizedException
     * @throws BadRequestException
     * @throws ConflictException     - If there
     */
    public boolean publish(Collection collection, Session session, boolean breakBeforePublish, boolean skipVerification)
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Collection exists
        if (collection == null) {
            throw new BadRequestException("Please provide a valid collection.");
        }

        // User has permission
        if (session == null || !permissionsService.canEdit(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // Check approval status
        if (collection.description.approvalStatus != ApprovalStatus.COMPLETE) {
            throw new ConflictException("This collection cannot be published because it is not approved");
        }

        // Break before transfer allows us to run tests on the prepublish-hook without messing up the content
        if (breakBeforePublish) {
            logInfo("Breaking before publish").log();
            return true;
        }

        Keyring keyring = zebedeeSupplier.get().getKeyringCache().get(session);
        if (keyring == null) throw new UnauthorizedException("No keyring is available for " + session.getEmail());

        ZebedeeCollectionReader collectionReader = new ZebedeeCollectionReader(zebedeeSupplier.get(), collection, session);
        long publishStart = System.currentTimeMillis();
        boolean publishComplete = Publisher.Publish(collection, session.getEmail(), collectionReader);

        if (publishComplete) {
            long onPublishCompleteStart = System.currentTimeMillis();

            new PublishNotification(collection).sendNotification(EventType.PUBLISHED);

            PostPublisher.postPublish(zebedeeSupplier.get(), collection, skipVerification, collectionReader);

            logInfo("collection post publish process completed")
                    .collectionName(collection)
                    .collectionId(collection)
                    .timeTaken((System.currentTimeMillis() - onPublishCompleteStart))
                    .log();
            logInfo("collection publish complete")
                    .collectionName(collection)
                    .collectionId(collection)
                    .timeTaken((System.currentTimeMillis() - publishStart))
                    .log();
        }

        return publishComplete;
    }

    public DirectoryListing listDirectory(
            Collection collection, String uri,
            Session session
    ) throws NotFoundException, UnauthorizedException,
            IOException, BadRequestException {

        if (collection == null) {
            throw new BadRequestException("Please specify a valid collection.");
        }

        // Check view permissionsServiceImpl
        if (!permissionsService.canView(session,
                collection.getDescription())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // Locate the path:
        Path path = collection.find(uri);
        if (path == null) {
            throw new NotFoundException("URI not found in collection: " + uri);
        }

        // Check we're requesting a directory:
        if (!Files.isDirectory(path)) {
            throw new BadRequestException(
                    "Please provide a URI to a directory: " + uri);
        }

        return listDirectory(path);
    }

    /**
     * List the given directory of a collection including the files that have already been published.
     *
     * @param collection the collection to overlay on master content
     * @param uri        the uri of the directory
     * @param session    the session (used to determine user permissionsServiceImpl)
     * @return a DirectoryListing object with system content overlaying master content
     * @throws NotFoundException
     * @throws UnauthorizedException
     * @throws IOException
     * @throws BadRequestException
     */
    public DirectoryListing listDirectoryOverlayed(
            Collection collection, String uri,
            Session session
    ) throws NotFoundException, UnauthorizedException,
            IOException, BadRequestException {

        DirectoryListing listing = listDirectory(collection, uri, session);

        Path publishedPath = this.published.get(uri);
        DirectoryListing publishedListing = listDirectory(publishedPath);

        listing.getFiles().putAll(publishedListing.getFiles());
        listing.getFolders().putAll(publishedListing.getFolders());

        return listing;
    }

    public void delete(Collection collection, Session session)
            throws IOException, ZebedeeException {

        // Collection exists
        if (collection == null) {
            throw new BadRequestException("Please specify a valid collection");
        }

        // User has permission
        if (!permissionsService.canEdit(session)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // Collection is empty
        if (!collection.isEmpty()) {
            throw new BadRequestException("The collection is not empty.");
        }

        // Go ahead
        collection.delete();
        collectionHistoryDaoSupplier.get().saveCollectionHistoryEvent(collection, session, COLLECTION_DELETED);
    }

    /**
     * Create new content if it does not already exist.
     *
     * @param collection
     * @param uri
     * @param session
     * @param request
     * @param requestBody
     * @throws ConflictException
     * @throws NotFoundException
     * @throws BadRequestException
     * @throws UnauthorizedException
     * @throws IOException
     */
    public void createContent(
            Collection collection, String uri, Session session, HttpServletRequest request,
            InputStream requestBody, CollectionEventType eventType, boolean validateJson
    ) throws ZebedeeException, IOException,
            FileUploadException {

        if (this.published.exists(uri)) {
            throw new ConflictException("This URI already exists");
        }

        Optional<Collection> blockingCollection = zebedeeSupplier.get().checkForCollectionBlockingChange(collection, uri);
        if (blockingCollection.isPresent()) {
            Collection blocker = blockingCollection.get();
            logInfo("Cannot create content as it existings in another collection.")
                    .saveOrEditConflict(collection, blocker, uri)
                    .user(session.getEmail())
                    .log();
            throw new ConflictException("This URI exists in another collection", blocker.getDescription().getName());
        }

        try {
            zebedeeSupplier.get().checkAllCollectionsForDeleteMarker(uri);
        } catch (DeleteContentRequestDeniedException ex) {
            throw new ConflictException("This URI is marked for deletion in another collection", ex.getCollectionName());
        }

        writeContent(collection, uri, session, request, requestBody, false, eventType, validateJson);
    }

    public void writeContent(
            Collection collection, String uri,
            Session session, HttpServletRequest request, InputStream requestBody, Boolean recursive,
            CollectionEventType eventType,
            boolean validateJson
    ) throws IOException, ZebedeeException, FileUploadException {

        CollectionWriter collectionWriter = collectionReaderWriterFactory.getWriter(zebedeeSupplier.get(), collection, session);

        logInfo("Attempting to write content.")
                .collectionName(collection)
                .path(uri)
                .user(session.getEmail())
                .log();

        if (collection.getDescription().getApprovalStatus() == ApprovalStatus.COMPLETE) {
            throw new BadRequestException("This collection has been approved and cannot be saved to.");
        }

        // Requested path
        if (StringUtils.isBlank(uri)) {
            throw new BadRequestException("Please provide a URI");
        }

        // Find the file if it exists
        Path path = collection.find(uri);

        // Check we're writing a file:
        if (path != null && Files.isDirectory(path)) {
            throw new BadRequestException("Please provide a URI to a file");
        }

        // Create / edit
        if (path == null) {
            // create the file
            if (!collection.create(session.getEmail(), uri)) {
                // file may be being edited in a different collection
                Optional<Collection> otherCollection = zebedeeSupplier.get().checkForCollectionBlockingChange(uri);
                if(otherCollection.isPresent()) {
                    throw new ConflictException(
                            "This URI is being edited in another collection", otherCollection.get().getDescription().getName());
                }
                throw new ConflictException(
                        "It could be this URI is being edited in another collection");
            }
        } else {
            // edit the file
            boolean result = collection.edit(session.getEmail(), uri, collectionWriter, recursive);
            if (!result) {
                // file may be being edited in a different collection
                Optional<Collection> otherCollection = zebedeeSupplier.get().checkForCollectionBlockingChange(uri);
                if(otherCollection.isPresent()) {
                    throw new ConflictException(
                            "This URI is being edited in another collection", otherCollection.get().getDescription().getName());
                }
                throw new ConflictException(
                        "It could be this URI is being edited in another collection");
            }
        }

        collection.save();
        logInfo("content save successful.").collectionName(collection).path(uri).user(session.getEmail()).log();

        path = collection.getInProgressPath(uri);
        if (!Files.exists(path)) {
            throw new NotFoundException(
                    "Somehow we weren't able to edit the requested URI");
        }

        CollectionHistoryEvent historyEvent = new CollectionHistoryEvent(collection, session, eventType, uri);

        // Detect whether this is a multipart request
        if (ServletFileUpload.isMultipartContent(request)) {
            postDataFile(request, uri, collectionWriter, historyEvent);

        } else {
            if (validateJson) {
                try (InputStream inputStream = validateJsonStream(requestBody)) {
                    collectionWriter.getInProgress().write(inputStream, uri);
                }
            } else {
                collectionWriter.getInProgress().write(requestBody, uri);
            }
            if (eventType != null) {
                collectionHistoryDaoSupplier.get().saveCollectionHistoryEvent(historyEvent);
            }
        }
    }

    /**
     * Take an input stream that contains json content and ensure its valid.
     *
     * @param inputStream
     * @return
     */
    public InputStream validateJsonStream(InputStream inputStream) throws BadRequestException {
        try {
            byte[] bytes = IOUtils.toByteArray(inputStream);

            try (ByteArrayInputStream validationInputStream = new ByteArrayInputStream(bytes)) {
                ContentUtil.deserialiseContent(validationInputStream);
            }

            return new ByteArrayInputStream(bytes);
        } catch (Exception e) {
            throw new BadRequestException("Validation of page content failed. Please try again");
        }
    }

    public boolean deleteContent(
            Collection collection, String uri,
            Session session
    ) throws IOException, ZebedeeException {

        // Collection (null check before authorisation check)
        if (collection == null) {
            throw new BadRequestException("Please specify a collection");
        }

        // Authorisation
        if (session == null || !permissionsService.canEdit(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // Requested path
        if (StringUtils.isBlank(uri)) {
            throw new BadRequestException("Please provide a URI");
        }

        // Find the file if it exists
        Path path = collection.find(uri);

        // Check the user has access to the given file
        if (path == null || !collection.isInCollection(uri)) {
            throw new NotFoundException(
                    "This URI cannot be found in the collection");
        }

        // Go ahead
        boolean deleted;
        CollectionEventType eventType;

        if (isDataVisualisationFile(path)) {
            deleted = collection.deleteDataVisContent(session, Paths.get(uri));
        } else if (Files.isDirectory(path)) {
            deleted = collection.deleteContentDirectory(session.getEmail(), uri);
        } else {
            deleted = collection.deleteFile(uri);
        }

        eventType = COLLECTION_CONTENT_DELETED;

        collection.save();
        if (deleted) {
            removeEmptyCollectionDirectories(path);
            collectionHistoryDaoSupplier.get().saveCollectionHistoryEvent(new CollectionHistoryEvent(collection, session,
                    eventType, uri));
        }
        return deleted;
    }

    /**
     * Save uploaded files.
     *
     * @param request
     * @param uri
     * @param collectionWriter
     * @throws FileUploadException
     * @throws IOException
     */
    private void postDataFile(
            HttpServletRequest request, String uri, CollectionWriter collectionWriter,
            CollectionHistoryEvent historyEvent
    )
            throws FileUploadException, IOException {

        ServletFileUpload upload = getServletFileUpload();

        try {
            for (FileItem item : upload.parseRequest(request)) {
                try (InputStream inputStream = item.getInputStream()) {
                    collectionWriter.getInProgress().write(inputStream, uri);
                }
                collectionHistoryDaoSupplier.get().saveCollectionHistoryEvent(historyEvent);
            }
        } catch (Exception e) {
            throw new IOException("Error processing uploaded file", e);
        }
    }

    /**
     * get a file upload object with progress listener
     *
     * @return an upload object
     */
    public ServletFileUpload getServletFileUpload() {
        // Set up the objects that do all the heavy lifting
        // PrintWriter out = response.getWriter();
        EncryptedFileItemFactory factory = new EncryptedFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);

        ProgressListener progressListener = getProgressListener();
        upload.setProgressListener(progressListener);
        return upload;
    }

    /**
     * get a progress listener
     *
     * @return a ProgressListener object
     */
    private ProgressListener getProgressListener() {
        // Set up a progress listener that we can use to power a progress bar
        return new ProgressListener() {
            private long megaBytes = -1;

            @Override
            public void update(long pBytesRead, long pContentLength, int pItems) {
                long mBytes = pBytesRead / 1000000;
                if (megaBytes == mBytes) {
                    return;
                }
                megaBytes = mBytes;
            }
        };
    }

    /**
     * Move or rename content within a collection
     *
     * @param session
     * @param collection
     * @param uri
     * @param newUri
     * @throws BadRequestException
     * @throws IOException
     * @throws UnauthorizedException
     */
    public void moveContent(Session session, Collection collection, String uri, String newUri) throws ZebedeeException, IOException {

        if (collection == null) {
            throw new BadRequestException("Please specify a collection");
        }

        // Authorisation
        if (session == null || !permissionsService.canEdit(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        if (StringUtils.isBlank(uri)) {
            throw new BadRequestException("Please provide a URI");
        }

        if (StringUtils.isBlank(newUri)) {
            throw new BadRequestException("Please provide a new URI");
        }

        if (this.published.exists(uri)) {
            throw new BadRequestException("You cannot move or rename a file that is already published.");
        }

        collection.moveContent(session, uri, newUri);
        collectionHistoryDaoSupplier.get().saveCollectionHistoryEvent(collection, session, COLLECTION_CONTENT_MOVED,
                contentMoved(uri, newUri));
        collection.save();
    }

    public void renameContent(Session session, Collection collection, String uri, String toUri) throws IOException,
            ZebedeeException {

        if (collection == null) {
            throw new BadRequestException("Please specify a collection");
        }

        // Authorisation
        if (session == null || !permissionsService.canEdit(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        if (StringUtils.isBlank(uri)) {
            throw new BadRequestException("Please provide a URI");
        }

        if (StringUtils.isBlank(toUri)) {
            throw new BadRequestException("Please provide a new URI");
        }

        if (this.published.exists(uri)) {
            throw new BadRequestException("You cannot move or rename a file that is already published.");
        }

        collection.renameContent(session.getEmail(), uri, toUri);
        collectionHistoryDaoSupplier.get().saveCollectionHistoryEvent(collection, session, COLLECTION_CONTENT_RENAMED,
                contentRenamed(uri, toUri));
        collection.save();
    }

    /**
     * Represents the list of all collections currently in the system. This adds
     * a couple of utility methods to {@link ArrayList}.
     */
    public static class CollectionList extends ArrayList<Collection> {

        /**
         * Retrieves a collection with the given id.
         *
         * @param id The id to look for.
         * @return If a {@link Collection} matching the given name exists,
         * (according to {@link PathUtils#toFilename(String)}) the
         * collection. Otherwise null.
         */
        public Collection getCollection(String id) {
            Collection result = null;

            if (StringUtils.isNotBlank(id)) {
                for (Collection collection : this) {
                    if (StringUtils.equalsIgnoreCase(collection.getDescription().getId(), id)) {
                        result = collection;
                        break;
                    }
                }
            }

            return result;
        }

        /**
         * Retrieves a collection with the given name.
         *
         * @param name The name to look for.
         * @return If a {@link Collection} matching the given name exists,
         * (according to {@link PathUtils#toFilename(String)}) the
         * collection. Otherwise null.
         */
        public Collection getCollectionByName(String name) {
            Collection result = null;

            if (StringUtils.isNotBlank(name)) {

                String filename = PathUtils.toFilename(name);
                for (Collection collection : this) {
                    String collectionFilename = collection.path.getFileName()
                            .toString();
                    if (StringUtils.equalsIgnoreCase(collectionFilename,
                            filename)) {
                        result = collection;
                        break;
                    }
                }
            }

            return result;
        }

        /**
         * Determines whether a collection with the given name exists.
         *
         * @param name The name to check for.
         * @return If {@link #getCollection(String)} returns non-null, true.
         */
        public boolean hasCollection(String name) {
            return getCollectionByName(name) != null;
        }

    }
}
