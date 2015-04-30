package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
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
            throw new UnauthorizedException("Session does not have edit permission: " + session);
        }

        // Everything is completed
        if (!collection.inProgressUris().isEmpty() || !collection.completeUris().isEmpty()) {
            throw new ConflictException("This collection can't be approved because it's not empty");
        }

        // Go ahead
        collection.description.approvedStatus = true;
        return collection.save();
    }

    public static class CollectionList extends ArrayList<Collection> {

        /**
         * Retrieves a collection with the given name.
         *
         * @param name The name to look for.
         * @return If a {@link Collection} matching the given name exists,
         * (according to {@link PathUtils#toFilename(String)}) the collection.
         * Otherwise null.
         */
        public Collection getCollection(String name) {
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
            return getCollection(name) != null;
        }

    }
}
