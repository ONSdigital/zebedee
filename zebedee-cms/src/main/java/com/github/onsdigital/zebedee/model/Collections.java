package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.content.DirectoryListing;
import com.github.onsdigital.content.page.base.Page;
import com.github.onsdigital.content.service.ContentNotFoundException;
import com.github.onsdigital.content.util.ContentUtil;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.data.DataPublisher;
import com.github.onsdigital.zebedee.data.DataReader;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.ContentEvent;
import com.github.onsdigital.zebedee.json.ContentEventType;
import com.github.onsdigital.zebedee.json.Session;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Collections {

    public final Path path;
    Zebedee zebedee;

    public Collections(Path path, Zebedee zebedee) {
        this.path = path;
        this.zebedee = zebedee;
    }


    public void complete(Collection collection, String uri,
                         Session session) throws IOException, NotFoundException,
            UnauthorizedException, BadRequestException {

        // Check the collection
        if (collection == null) {
            throw new BadRequestException("Please specify a valid collection.");
        }

        // Check authorisation
        if (!zebedee.permissions.canEdit(session)) {
            throw new UnauthorizedException(session);
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

        // Attempt to review:
        if (collection.complete(session.email, uri)) {
            collection.save();
        } else {
            throw new BadRequestException("URI was not reviewed.");
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
                    result.add(new Collection(path, zebedee));
                }
            }
        }
        return result;
    }

    public boolean approve(Collection collection, Session session)
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException {

        // Collection exists
        if (collection == null) {
            throw new BadRequestException("Please provide a valid collection.");
        }

        // User has permission
        if (session == null || !zebedee.permissions.canEdit(session.email)) {
            throw new UnauthorizedException(session);
        }

        // Everything is completed
        if (!collection.inProgressUris().isEmpty()
                || !collection.completeUris().isEmpty()) {
            throw new ConflictException(
                    "This collection can't be approved because it's not empty");
        }

        // Go ahead
        collection.description.approvedStatus = true;
        return collection.save();
    }

    /**
     * Publish the files
     *
     * @param collection the collection to publish
     * @param session    a session with editor priviledges
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     * @throws BadRequestException
     * @throws ConflictException     - If there
     */
    public boolean publish(Collection collection, Session session)
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Collection exists
        if (collection == null) {
            throw new BadRequestException("Please provide a valid collection.");
        }

        // User has permission
        if (session == null || !zebedee.permissions.canEdit(session.email)) {
            throw new UnauthorizedException(session);
        }

        // Go ahead
        if (collection.description.approvedStatus == false) {
            throw new ConflictException("This collection cannot be published because it is not approved");
        }

        // Do any processing of data files
        DataPublisher.preprocessCollection(collection, session);

        // Move each item of content:
        for (String uri : collection.reviewed.uris()) {

            Path source = collection.reviewed.get(uri);
            if (source != null) {
                Path destination = zebedee.launchpad.toPath(uri);
                PathUtils.moveFilesInDirectory(source, destination);
            }

            // Add an event to the event log
            collection.AddEvent(uri, new ContentEvent(new Date(), ContentEventType.PUBLISHED, session.email));
        }


        // Save a published collections log
        collection.save();
        String filename = PathUtils.toFilename(collection.description.name);
        Path collectionDescriptionPath = this.path.resolve(filename + ".json");
        Path logPath = this.zebedee.path.resolve("publish-log");
        if(Files.exists(logPath) == false) { Files.createDirectory(logPath); }

        Date date = new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH-mm");
        logPath = logPath.resolve(format.format(date) + " " + filename + ".json");

        Files.copy(collectionDescriptionPath, logPath);

        // Delete the folders:
        delete(collection, session);

        return true;
    }

    public DirectoryListing listDirectory(Collection collection, String uri,
                                          Session session) throws NotFoundException, UnauthorizedException,
            IOException, BadRequestException {

        if (collection == null) {
            throw new BadRequestException("Please specify a valid collection.");
        }

        // Check view permissions
        if (zebedee.permissions.canView(session,
                collection.description) == false) {
            throw new UnauthorizedException(session);
        }

        // Locate the path:
        Path path = collection.find(session.email, uri);
        if (path == null) {
            throw new NotFoundException("URI not found in collection: " + uri);
        }

        // Check we're requesting a directory:
        if (!Files.isDirectory(path)) {
            throw new BadRequestException(
                    "Please provide a URI to a directory: " + uri);
        }

        Serialiser.getBuilder().setPrettyPrinting();
        return ContentUtil.listDirectory(path);
    }

    /**
     * List the given directory of a collection including the files that have already been published.
     *
     * @param collection
     * @param uri
     * @param session
     * @return
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
        DirectoryListing publishedListing = ContentUtil.listDirectory(publishedPath);

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
            throw new UnauthorizedException(session);
        }

        // Collection is empty
        if (!collection.isEmpty()) {
            throw new BadRequestException("The collection is not empty.");
        }

        // Go ahead
        collection.delete();
    }

    public void readContent(Collection collection, String uri, boolean resolveReferences, Session session,
                            HttpServletResponse response) throws IOException,
            UnauthorizedException, BadRequestException, NotFoundException {

        // Collection (null check before authorisation check)
        if (collection == null) {
            throw new BadRequestException("Please specify a collection");
        }

        // Authorisation
        if (session == null
                || !zebedee.permissions.canView(session.email,
                collection.description)) {
            throw new UnauthorizedException(session);
        }

        // Requested path
        if (StringUtils.isBlank(uri)) {
            throw new BadRequestException("Please provide a URI");
        }

        // Path
        Path path = collection.find(session.email, uri);
        if (path == null) {
            throw new NotFoundException("URI not found in collection: " + uri);
        }

        // Check we're requesting a file:
        if (Files.isDirectory(path)) {
            throw new BadRequestException("URI does not specify a file");
        }

        // Guess the MIME type
        if (StringUtils.equalsIgnoreCase("json", FilenameUtils.getExtension(path.toString()))) {
            response.setContentType("application/json");
        } else {
            String contentType = Files.probeContentType(path);
            response.setContentType(contentType);
        }

        try (InputStream input = Files.newInputStream(path)) {
            if (resolveReferences) {
                Page page = ContentUtil.deserialisePage(input);
                page.loadReferences(new DataReader(session, collection));
                // Write the file to the response
                org.apache.commons.io.IOUtils.copy(new StringReader(page.toJson()),
                        response.getOutputStream());
            } else {
                // Write the file to the response
                org.apache.commons.io.IOUtils.copy(input,
                        response.getOutputStream());
            }
        } catch (ContentNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }

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
        if (session == null || !zebedee.permissions.canEdit(session.email)) {
            throw new UnauthorizedException(session);
        }

        // Requested path
        if (StringUtils.isBlank(uri)) {
            throw new BadRequestException("Please provide a URI");
        }

        // Find the file if it exists
        Path path = collection.find(session.email, uri);

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
                postDataFile(request, path);
            } catch (Exception e) {

            }
        } else {
            // Otherwise we're doing a json content update
            try (OutputStream output = Files.newOutputStream(path)) {
                org.apache.commons.io.IOUtils.copy(requestBody, output);
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
            throw new UnauthorizedException(session);
        }

        // Requested path
        if (StringUtils.isBlank(uri)) {
            throw new BadRequestException("Please provide a URI");
        }

        // Find the file if it exists
        Path path = collection.find(session.email, uri);

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

    private void postDataFile(HttpServletRequest request, Path path)
            throws FileUploadException, IOException {

        // Set up the objects that do all the heavy lifting
        // PrintWriter out = response.getWriter();
        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);

        // Set up a progress listener that we can use to power a progress bar
        ProgressListener progressListener = new ProgressListener() {
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
        upload.setProgressListener(progressListener);

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

        public boolean transfer(String email, String uri, Collection source,
                                Collection destination) throws IOException {
            boolean result = false;

            // Move the file
            Path sourcePath = source.find(email, uri);
            Path destinationPath = destination.getInProgressPath(uri);

            PathUtils.moveFilesInDirectory(sourcePath, destinationPath);
            result = true;

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
