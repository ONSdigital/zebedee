package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.encryptedfileupload.EncryptedFileItemFactory;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.data.DataPublisher;
import com.github.onsdigital.zebedee.data.json.DirectoryListing;
import com.github.onsdigital.zebedee.exceptions.*;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.publishing.Publisher;
import com.github.onsdigital.zebedee.util.EncryptionUtils;
import com.github.onsdigital.zebedee.util.Log;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;

public class Collections {
    public final Path path;
    Zebedee zebedee;

    public Collections(Path path, Zebedee zebedee) {
        this.path = path;
        this.zebedee = zebedee;
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
                    listing.folders.put(directory.getFileName().toString(),
                            directory.toString());
                } else {
                    listing.files.put(directory.getFileName().toString(),
                            directory.toString());
                }
            }
        }
        return listing;
    }

    /**
     * Mark a file in a collection as 'complete'
     *
     * @param collection
     * @param uri
     * @param session
     * @throws IOException
     * @throws NotFoundException
     * @throws UnauthorizedException
     * @throws BadRequestException
     */
    public void complete(Collection collection, String uri,
                         Session session) throws IOException, NotFoundException,
            UnauthorizedException, BadRequestException {

        // Check the collection
        if (collection == null) {
            throw new BadRequestException("Please specify a valid collection.");
        }

        // Check authorisation
        if (!zebedee.permissions.canEdit(session)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // Locate the path:
        Path path = collection.inProgress.get(uri);
        if (path == null) {
            throw new NotFoundException("URI not in progress.");
        }

        // Check we're requesting a file:
        if (java.nio.file.Files.isDirectory(path)) {
            throw new BadRequestException("URI does not represent a file.");
        }

        // Attempt to complete:
        if (collection.complete(session.email, uri)) {
            collection.save();
        } else {
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
                        result.add(new Collection(path, zebedee));
                    } catch (CollectionNotFoundException e) {
                        Log.print(e, "Failed to deserialise collection with path %s", path.toString());
                    }
                }
            }
        }

        return result;
    }

    /**
     * Approve the given collection.
     *
     * @param collection
     * @param session
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     * @throws BadRequestException
     * @throws ConflictException
     */
    public boolean approve(Collection collection, Session session)
            throws IOException, ZebedeeException {

        // Collection exists
        if (collection == null) {
            throw new BadRequestException("Please provide a valid collection.");
        }

        // User has permission
        if (session == null || !zebedee.permissions.canEdit(session.email)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // Everything is completed
        if (!collection.inProgressUris().isEmpty()
                || !collection.completeUris().isEmpty()) {
            throw new ConflictException(
                    "This collection can't be approved because it's not empty");
        }

        ZebedeeCollectionReader collectionReader = new ZebedeeCollectionReader(zebedee, collection, session);

        // if the collection is release related - get the release page and add links to other pages in release
        if (collection.isRelease()) {
            Log.print("Release identified for collection %s, populating the page links...", collection.description.name);
            try {
                collection.populateRelease(collectionReader);
            } catch (ZebedeeException e) {
                Log.print(e, "Failed to populate release page for collection %s", collection.description.name);
            }
        }

        // Do any processing of data files
        try {
            new DataPublisher().preprocessCollection(collectionReader, zebedee, collection, session);
        } catch (URISyntaxException e) {
            throw new BadRequestException("Brian could not process this collection");
        }

        // Go ahead
        collection.description.approvedStatus = true;
        collection.description.AddEvent(new Event(new Date(), EventType.APPROVED, session.email));

        return collection.save();
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
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException {

        // Collection exists
        if (collection == null) {
            throw new BadRequestException("Please provide a valid collection.");
        }

        // User has permission
        if (session == null || !zebedee.permissions.canEdit(session.email)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // nothing to do if the approved status is already false.
        if (!collection.description.approvedStatus) {
            return true;
        }

        // Go ahead
        collection.description.approvedStatus = false;
        collection.description.AddEvent(new Event(new Date(), EventType.UNLOCKED, session.email));

        return collection.save();
    }

    /**
     * Publish the files
     *
     * @param collection the collection to publish
     * @param session    a session with editor priviledges
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
        if (session == null || !zebedee.permissions.canEdit(session.email)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // Check approved status
        if (!collection.description.approvedStatus) {
            throw new ConflictException("This collection cannot be published because it is not approved");
        }

        // Break before transfer allows us to run tests on the prepublish-hook without messing up the content
        if (breakBeforePublish) {
            System.out.println("Breaking before publish");
            return true;
        }
        System.out.println("Going ahead with publish");

        boolean publishComplete = Publisher.Publish(zebedee, collection, session.email, skipVerification);

        return publishComplete;
    }

    public DirectoryListing listDirectory(Collection collection, String uri,
                                          Session session) throws NotFoundException, UnauthorizedException,
            IOException, BadRequestException {

        if (collection == null) {
            throw new BadRequestException("Please specify a valid collection.");
        }

        // Check view permissions
        if (!zebedee.permissions.canView(session,
                collection.description)) {
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
     * @param session    the session (used to determine user permissions)
     * @return a DirectoryListing object with system content overlaying master content
     * @throws NotFoundException
     * @throws UnauthorizedException
     * @throws IOException
     * @throws BadRequestException
     */
    public DirectoryListing listDirectoryOverlayed(Collection collection, String uri,
                                                   Session session) throws NotFoundException, UnauthorizedException,
            IOException, BadRequestException {

        DirectoryListing listing = listDirectory(collection, uri, session);

        Path publishedPath = zebedee.published.get(uri);
        DirectoryListing publishedListing = listDirectory(publishedPath);

        listing.files.putAll(publishedListing.files);
        listing.folders.putAll(publishedListing.folders);

        return listing;
    }

    public void delete(Collection collection, Session session)
            throws IOException, NotFoundException, UnauthorizedException,
            BadRequestException {

        // Collection exists
        if (collection == null) {
            throw new BadRequestException("Please specify a valid collection");
        }

        // User has permission
        if (!zebedee.permissions.canEdit(session)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // Collection is empty
        if (!collection.isEmpty()) {
            throw new BadRequestException("The collection is not empty.");
        }

        // Go ahead
        collection.delete();
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
    public void createContent(Collection collection, String uri, Session session, HttpServletRequest request, InputStream requestBody) throws ConflictException, NotFoundException, BadRequestException, UnauthorizedException, IOException {

        if (zebedee.published.exists(uri) || zebedee.isBeingEdited(uri) > 0) {
            throw new ConflictException("This URI already exists");
        }

        writeContent(collection, uri, session, request, requestBody);
    }

    public void writeContent(Collection collection, String uri,
                             Session session, HttpServletRequest request, InputStream requestBody)
            throws IOException, BadRequestException, UnauthorizedException,
            ConflictException, NotFoundException {

        // Collection (null check before authorisation check)
        if (collection == null) {
            throw new BadRequestException("Please specify a collection");
        }

        // Authorisation
        if (session == null || !zebedee.permissions.canEdit(session, collection.description)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
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
            if (!collection.create(session.email, uri)) {
                // file may be being edited in a different collection
                throw new ConflictException(
                        "It could be this URI is being edited in another collection");
            }
        } else {
            // edit the file
            boolean result = collection.edit(session.email, uri);
            if (!result) {
                // file may be being edited in a different collection
                throw new ConflictException(
                        "It could be this URI is being edited in another collection");
            }
        }

        // Save collection metadata
        collection.save();

        path = collection.getInProgressPath(uri);
        if (!Files.exists(path)) {
            throw new NotFoundException(
                    "Somehow we weren't able to edit the requested URI");
        }


        // Detect whether this is a multipart request
        if (ServletFileUpload.isMultipartContent(request)) {
            // If it is we're doing an xls/csv file upload
            try {
                if (collection.description.isEncrypted) {
                    postDataFile(request, path, zebedee.keyringCache.get(session).get(collection.description.id));
                } else {
                    postDataFile(request, path);
                }
            } catch (Exception e) {

            }
        } else {
            // Otherwise we're doing a json content update
            if (collection.description.isEncrypted) {
                SecretKey key = zebedee.keyringCache.get(session).get(collection.description.id);
                try (OutputStream output = EncryptionUtils.encryptionOutputStream(path, key)) {
                    org.apache.commons.io.IOUtils.copy(requestBody, output);
                }
            } else {
                try (OutputStream output = Files.newOutputStream(path)) {
                    org.apache.commons.io.IOUtils.copy(requestBody, output);
                }
            }
        }
    }

    public boolean deleteContent(Collection collection, String uri,
                                 Session session) throws IOException, BadRequestException,
            UnauthorizedException, NotFoundException {

        // Collection (null check before authorisation check)
        if (collection == null) {
            throw new BadRequestException("Please specify a collection");
        }

        // Authorisation
        if (session == null || !zebedee.permissions.canEdit(session.email)) {
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
        if (Files.isDirectory(path)) {
            deleted = collection.deleteContent(session.email, uri);
        } else {
            deleted = collection.deleteFile(uri);
        }

        collection.save();

        return deleted;
    }

    /**
     * Save a data file from a servlet (no encryption)
     *
     * @param request
     * @param path
     * @throws FileUploadException
     * @throws IOException
     */
    private void postDataFile(HttpServletRequest request, Path path)
            throws FileUploadException, IOException {

        ServletFileUpload upload = getServletFileUpload();

        try {
            // Read the items - this will save the values to temp files
            for (FileItem item : upload.parseRequest(request)) {
                item.write(path.toFile());
            }
        } catch (Exception e) {
            // item.write throws Exception
            throw new IOException("Error processing uploaded file", e);
        }
    }

    /**
     * Save a data file from a servlet using encryption
     *
     * @param request
     * @param path
     * @param key
     * @throws FileUploadException
     * @throws IOException
     */
    private void postDataFile(HttpServletRequest request, Path path, SecretKey key)
            throws FileUploadException, IOException {

        ServletFileUpload upload = getServletFileUpload();

        try {
            // Read the items - this will save the values to temp files
            for (FileItem item : upload.parseRequest(request)) {
                try(InputStream inputStream = item.getInputStream(); OutputStream outputStream = EncryptionUtils.encryptionOutputStream(path, key)) {
                    IOUtils.copy(inputStream, outputStream);
                }
            }
        } catch (Exception e) {
            // item.write throws Exception
            throw new IOException("Error processing uploaded file", e);
        }
    }

    /**
     * get a file upload object with progress listener
     *
     * @return an upload object
     */
    private ServletFileUpload getServletFileUpload() {
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
    public void moveContent(Session session, Collection collection, String uri, String newUri) throws BadRequestException, IOException, UnauthorizedException {
        // Collection (null check before authorisation check)
        if (collection == null) {
            throw new BadRequestException("Please specify a collection");
        }

        // Authorisation
        if (session == null || !zebedee.permissions.canEdit(session.email)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        if (StringUtils.isBlank(uri)) {
            throw new BadRequestException("Please provide a URI");
        }

        if (StringUtils.isBlank(newUri)) {
            throw new BadRequestException("Please provide a new URI");
        }

        // Find the file if it exists
        Path path = collection.find(uri);

        // Check we're writing a file:
        if (path != null && Files.isDirectory(path)) {
            throw new BadRequestException("Please provide a URI to a file");
        }

        collection.moveContent(session.email, uri, newUri);
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
                    if (StringUtils.equalsIgnoreCase(collection.description.id,
                            id)) {
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
