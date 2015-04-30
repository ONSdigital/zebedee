package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.DirectoryListing;
import com.github.onsdigital.zebedee.json.Session;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class Collections {

    public final Path path;
    Zebedee zebedee;

    public Collections(Path path, Zebedee zebedee) {
        this.path = path;
        this.zebedee = zebedee;
    }


    /**
     * @return A list of all {@link Collection}s.
     * @throws IOException If a filesystem error occurs.
     */
    public CollectionList list() throws IOException {
        CollectionList result = new CollectionList();
        try (DirectoryStream<Path> stream = Files
                .newDirectoryStream(path)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    result.add(new Collection(path, zebedee));
                }
            }
        }
        return result;
    }


    public boolean approve(Collection collection, Session session) throws IOException, UnauthorizedException, BadRequestException, ConflictException {

        // Collection exists
        if (collection == null) {
            throw new BadRequestException("Please provide a valid collection.");
        }

        // User has permission
        if (session == null || !zebedee.permissions.canEdit(session.email)) {
            throw new UnauthorizedException(session);
        }

        // Everything is completed
        if (!collection.inProgressUris().isEmpty() || !collection.completeUris().isEmpty()) {
            throw new ConflictException("This collection can't be approved because it's not empty");
        }

        // Go ahead
        collection.description.approvedStatus = true;
        return collection.save();
    }

    public DirectoryListing listDirectory(Collection collection, String uri, Session session) throws NotFoundException, UnauthorizedException, IOException, BadRequestException {


        if (collection == null) {
            throw new NotFoundException("Please provide a valid collection.");
        }

        // Check view permissions
        if (Root.zebedee.permissions.canView(session.email, collection.description) == false) {
            throw new UnauthorizedException(session);
        }

        // Locate the path:
        Path path = collection.find(session.email, uri);
        if (path == null) {
            throw new NotFoundException("URI not found in collection: " + uri);
        }

        // Check we're requesting a directory:
        if (!Files.isDirectory(path)) {
            throw new BadRequestException("Please provide a URI to a directory: " + uri);
        }

        return listDirectory(path);
    }


    private DirectoryListing listDirectory(java.nio.file.Path path)
            throws IOException {


        // Get the directory listing:
        DirectoryListing listing = new DirectoryListing();
        try (DirectoryStream<java.nio.file.Path> stream = Files
                .newDirectoryStream(path)) {
            for (java.nio.file.Path directory : stream) {
                // Recursively delete directories only:
                if (Files.isDirectory(directory)) {
                    listing.folders.put(directory.getFileName().toString(),
                            directory.toString());
                } else {
                    listing.files.put(directory.getFileName().toString(),
                            directory.toString());
                }
            }
        }
        Serialiser.getBuilder().setPrettyPrinting();
        return listing;
    }

    /**
     * Represents the list of all collections currently in the system.
     * This adds a couple of utility methods to {@link ArrayList}.
     */
    public static class CollectionList extends ArrayList<Collection> {

        /**
         * Retrieves a collection with the given id.
         *
         * @param id The id to look for.
         * @return If a {@link Collection} matching the given name exists,
         * (according to {@link PathUtils#toFilename(String)}) the collection.
         * Otherwise null.
         */
        public Collection getCollection(String id) {
            Collection result = null;

            if (StringUtils.isNotBlank(id)) {
                for (Collection collection : this) {
                    if (StringUtils.equalsIgnoreCase(collection.description.id, id)) {
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
         * (according to {@link PathUtils#toFilename(String)}) the collection.
         * Otherwise null.
         */
        public Collection getCollectionByName(String name) {
            Collection result = null;

            if (StringUtils.isNotBlank(name)) {

                String filename = PathUtils.toFilename(name);
                for (Collection collection : this) {
                    String collectionFilename = collection.path.getFileName().toString();
                    if (StringUtils.equalsIgnoreCase(collectionFilename, filename)) {
                        result = collection;
                        break;
                    }
                }
            }

            return result;
        }

        public boolean transfer(String email, String uri, Collection source, Collection destination) throws IOException {
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
