package com.github.onsdigital.zebedee;

import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.model.*;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Zebedee {


    public static final String PUBLISHED = "published";
    public static final String COLLECTIONS = "collections";
    static final String ZEBEDEE = "zebedee";
    static final String USERS = "users";
    static final String SESSIONS = "sessions";
    static final String PERMISSIONS = "permissions";
    static final String TEAMS = "teams";

    public final Path path;
    public final Content published;
    public final Path collections;
    public final Users users;
    public final Sessions sessions;
    public final Permissions permissions;

    public Zebedee(Path path) {

        // Validate the directory:
        this.path = path;
        Path published = path.resolve(PUBLISHED);
        Path collections = path.resolve(COLLECTIONS);
        Path users = path.resolve(USERS);
        Path sessions = path.resolve(SESSIONS);
        Path permissions = path.resolve(PERMISSIONS);
        Path teams = path.resolve(PERMISSIONS).resolve(TEAMS);
        if (!Files.exists(published) || !Files.exists(collections) || !Files.exists(users) || !Files.exists(sessions) || !Files.exists(permissions) || !Files.exists(teams)) {
            throw new IllegalArgumentException(
                    "This folder doesn't look like a zebedee folder: "
                            + path.toAbsolutePath());
        }
        this.published = new Content(published);
        this.collections = collections;
        this.users = new Users(users, this);
        this.sessions = new Sessions(sessions);
        this.permissions = new Permissions(permissions, teams);
    }

    /**
     * Creates a new Zebedee folder in the specified parent Path.
     *
     * @param parent The directory in which the folder will be created.
     * @return A {@link Zebedee} instance representing the newly created folder.
     * @throws IOException If a filesystem error occurs.
     */
    public static Zebedee create(Path parent) throws IOException {

        // Create the folder structure
        Path path = Files.createDirectory(parent.resolve(ZEBEDEE));
        Files.createDirectory(path.resolve(PUBLISHED));
        Files.createDirectory(path.resolve(COLLECTIONS));
        Files.createDirectory(path.resolve(USERS));
        Files.createDirectory(path.resolve(SESSIONS));
        Files.createDirectory(path.resolve(PERMISSIONS));
        Files.createDirectory(path.resolve(PERMISSIONS).resolve(TEAMS));

        Zebedee zebedee = new Zebedee(path);

        // Create the initial user
        User user = new User();
        user.email = "florence@magicroundabout.ons.gov.uk";
        user.name = "Florence";
        String password = "Doug4l";
        Users.createSystemUser(zebedee, user, password);
        Session session = zebedee.sessions.create("florence@magicroundabout.ons.gov.uk");

        // todo - remove these once access functionality is available.
        user = new User();
        user.email = "p1@t.com";
        user.name = "p1";
        password = "Doug4l";
        Users.createAdmin(zebedee, user, password, session);

        user = new User();
        user.email = "p2@t.com";
        user.name = "p2";
        password = "Doug4l";
        Users.createAdmin(zebedee, user, password, session);

        user = new User();
        user.email = "p3@t.com";
        user.name = "p3";
        password = "Doug4l";
        Users.createAdmin(zebedee, user, password, session);

        user = new User();
        user.email = "p4@t.com";
        user.name = "p4";
        password = "Doug4l";
        Users.createAdmin(zebedee, user, password, session);

        user = new User();
        user.email = "p5@t.com";
        user.name = "p5";
        password = "Doug4l";
        Users.createAdmin(zebedee, user, password, session);

        return zebedee;
    }

    /**
     * This method works out how many {@link com.github.onsdigital.zebedee.model.Collection}s contain the given URI.
     * The intention is to allow double-checking in case of concurrent editing.
     * This should be 0 in order for someone to be allowed to edit a URI and
     * should be 1 after editing is initiated. If this returns more than 1 after
     * initiating editing then the current attempt to edit should be reverted -
     * presumably a race condition.
     *
     * @param uri The URI to check.
     * @return The number of {@link com.github.onsdigital.zebedee.model.Collection}s containing the given URI.
     * @throws IOException
     */
    public int isBeingEdited(String uri) throws IOException {
        int result = 0;

        // Is this URI present anywhere else?
        for (Collection collection : getCollections()) {
            if (collection.isInCollection(uri)) {
                result++;
            }
        }

        return result;
    }

    /**
     * @return A list of all {@link Collection}s.
     * @throws IOException If a filesystem error occurs.
     */
    public Collections getCollections() throws IOException {
        Collections result = new Collections();
        try (DirectoryStream<Path> stream = Files
                .newDirectoryStream(collections)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    result.add(new Collection(path, this));
                }
            }
        }
        return result;
    }

    public Path find(String uri) throws IOException {

        // There's currently only one place to look for content.
        // We may add one or more staging layers later.
        return published.get(uri);
    }

    public boolean publish(Collection collection) throws IOException {

        // Check everything has been reviewed:
        if (collection.inProgress.uris().size() > 0) {
            return false;
        }

        // Move each item of content:
        for (String uri : collection.reviewed.uris()) {
            Path source = collection.reviewed.get(uri);
            Path destination = published.toPath(uri);
            PathUtils.moveFilesInDirectory(source, destination);
        }

        // Delete the folders:
        delete(collection.path);

        return true;
    }

    /**
     * Deletes a collection folder structure. This method only deletes folders
     * and will throw an exception if any of the folders aren't empty. This
     * ensures that only a release that has been published can be deleted.
     *
     * @param path The {@link Path} to the collection folder.
     * @throws IOException If any of the subfolders is not empty or if a filesystem
     *                     error occurs.
     */
    public void delete(Path path) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path directory : stream) {
                // Recursively delete directories only - added .DS_Store files:
                if (Files.isDirectory(directory) || directory.endsWith(".DS_Store")) {
                    delete(directory);
                }
            }
        }
        Files.delete(path);
    }
}
