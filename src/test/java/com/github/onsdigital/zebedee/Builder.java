package com.github.onsdigital.zebedee;

import com.github.davidcarboni.cryptolite.Password;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.AccessMapping;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.model.PathUtils;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This is a utility class to build a known {@link Zebedee} structure for
 * testing.
 *
 * @author david
 */
public class Builder {

    public String[] collectionNames = {"Inflation Q2 2015", "Labour Market Q2 2015"};
    public Path parent;
    public Path zebedee;
    public List<Path> collections;
    public List<String> contentUris;

    public Builder(Class<?> name) throws IOException {

        // Create the structure:
        parent = Files.createTempDirectory(name.getSimpleName());
        zebedee = createZebedee(parent);

        // Create the collections:
        collections = new ArrayList<>();
        for (String collectionName : collectionNames) {
            Path collection = createCollection(collectionName, zebedee);
            collections.add(collection);
        }

        // Create some published content:
        Path folder = zebedee.resolve(Zebedee.PUBLISHED);
        contentUris = new ArrayList<>();
        String contentUri;
        Path contentPath;

        // Something for Economy:
        contentUri = "/economy/inflationandpriceindices/bulletins/consumerpriceinflationjune2014.html";
        contentPath = folder.resolve(contentUri.substring(1));
        Files.createDirectories(contentPath.getParent());
        Files.createFile(contentPath);
        contentUris.add(contentUri);

        // Something for Labour market:
        contentUri = "/employmentandlabourmarket/peopleinwork/earningsandworkinghours/bulletins/uklabourmarketjuly2014.html";
        contentPath = folder.resolve(contentUri.substring(1));
        Files.createDirectories(contentPath.getParent());
        Files.createFile(contentPath);
        contentUris.add(contentUri);

        // A couple of users:
        Path users = zebedee.resolve(Zebedee.USERS);
        Files.createDirectories(users);

        User jukesie = new User();
        jukesie.name = "Matt Jukes";
        jukesie.email = "jukesie@example.com";
        jukesie.passwordHash = Password.hash("twitter");
        jukesie.inactive = false;
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(jukesie.email) + ".json"))) {
            Serialiser.serialise(outputStream, jukesie);
        }

        User patricia = new User();
        patricia.name = "Patricia Pumpkin";
        patricia.email = "patricia@example.com";
        patricia.passwordHash = Password.hash("password");
        patricia.inactive = false;
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(patricia.email) + ".json"))) {
            Serialiser.serialise(outputStream, patricia);
        }

        User ronny = new User();
        ronny.name = "Ronny Roller";
        ronny.email = "ronny@example.com";
        ronny.passwordHash = Password.hash("secret");
        ronny.inactive = false;
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(ronny.email) + ".json"))) {
            Serialiser.serialise(outputStream, ronny);
        }

        Path sessions = zebedee.resolve(Zebedee.SESSIONS);
        Files.createDirectories(sessions);

        // Set up some permissions:
        Path permissions = zebedee.resolve(Zebedee.PERMISSIONS);
        Files.createDirectories(permissions);

        AccessMapping accessMapping = new AccessMapping();

        accessMapping.owners = new HashSet<>();
        accessMapping.owners.add(jukesie.email);

        accessMapping.digitalPublishingTeam = new HashSet<>();
        accessMapping.digitalPublishingTeam.add(patricia.email);

        accessMapping.paths = new HashMap<>();
        Set contentOwners = new HashSet<>();
        contentOwners.add(ronny.email);
        accessMapping.paths.put("/economy", contentOwners);

        Path path = permissions.resolve("accessMapping.json");
        try (OutputStream output = Files.newOutputStream(path)) {
            Serialiser.serialise(output, accessMapping);
        }
    }

    public void delete() throws IOException {
        System.out.println("Deleting zebedee at "+parent);
        FileUtils.deleteDirectory(parent.toFile());
    }

    /**
     * Creates a published file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    public Path createPublishedFile(String uri) throws IOException {

        Path published = zebedee.resolve(Zebedee.PUBLISHED);
        Path content = published.resolve(uri.substring(1));
        Files.createDirectories(content.getParent());
        Files.createFile(content);
        return content;
    }

    /**
     * Creates an approved file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    public Path isApproved(String uri) throws IOException {

        Path approved = collections.get(1).resolve(com.github.onsdigital.zebedee.model.Collection.APPROVED);
        Path content = approved.resolve(uri.substring(1));
        Files.createDirectories(content.getParent());
        Files.createFile(content);
        return content;
    }

    /**
     * Creates an approved file in a different {@link com.github.onsdigital.zebedee.model.Collection}.
     *
     * @param uri        The URI to be created.
     * @param collection The {@link com.github.onsdigital.zebedee.model.Collection} in which to create the content.
     * @throws IOException If a filesystem error occurs.
     */
    public void isBeingEditedElsewhere(String uri, int collection) throws IOException {

        Path approved = collections.get(collection)
                .resolve(com.github.onsdigital.zebedee.model.Collection.APPROVED);
        Path content = approved.resolve(uri.substring(1));
        Files.createDirectories(content.getParent());
        Files.createFile(content);
    }

    /**
     * Creates an in-progress file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    public Path isInProgress(String uri) throws IOException {

        Path inProgress = collections.get(1).resolve(com.github.onsdigital.zebedee.model.Collection.IN_PROGRESS);
        Path content = inProgress.resolve(uri.substring(1));
        Files.createDirectories(content.getParent());
        Files.createFile(content);
        return content;
    }

    public Session createSession(String email) throws IOException {

        // Build the session object
        Session session = new Session();
        session.id = com.github.davidcarboni.cryptolite.Random.id();
        session.email = email;

        // Determine the path in which to create the session Json
        Path sessionPath = null;
        String sessionFileName = PathUtils.toFilename(session.id);
        sessionFileName += ".json";
        sessionPath = zebedee.resolve(Zebedee.SESSIONS).resolve(sessionFileName);

        // Serialise
        try (OutputStream output = Files.newOutputStream(sessionPath)) {
            Serialiser.serialise(output, session);
        }

        return session;
    }

    /**
     * This method creates the expected set of folders for a Zebedee structure.
     * This code is intentionaly copied from {@link Zebedee#create(Path)}. This
     * ensures there's a fixed expectation, rather than relying on a method that
     * will be tested as part of the test suite.
     *
     * @param parent The parent folder, in which the {@link Zebedee} structure will
     *               be built.
     * @return The root {@link Zebedee} path.
     * @throws IOException If a filesystem error occurs.
     */
    private Path createZebedee(Path parent) throws IOException {
        Path path = Files.createDirectory(parent.resolve(Zebedee.ZEBEDEE));
        Files.createDirectory(path.resolve(Zebedee.PUBLISHED));
        Files.createDirectory(path.resolve(Zebedee.COLLECTIONS));
        Files.createDirectory(path.resolve(Zebedee.SESSIONS));
        Files.createDirectory(path.resolve(Zebedee.PERMISSIONS));
        System.out.println("Created zebedee at "+path);
        return path;
    }

    /**
     * This method creates the expected set of folders for a Zebedee structure.
     * This code is intentionally copied from
     * {@link com.github.onsdigital.zebedee.model.Collection#create(com.github.onsdigital.zebedee.json.CollectionDescription, Zebedee)}. This ensures there's a fixed
     * expectation, rather than relying on a method that will be tested as part
     * of the test suite.
     *
     * @param root The root of the {@link Zebedee} structure
     * @param name The name of the {@link com.github.onsdigital.zebedee.model.Collection}.
     * @return The root {@link com.github.onsdigital.zebedee.model.Collection} path.
     * @throws IOException If a filesystem error occurs.
     */
    private Path createCollection(String name, Path root) throws IOException {

        String filename = PathUtils.toFilename(name);
        Path collections = root.resolve(Zebedee.COLLECTIONS);

        // Create the folders:
        Path collection = collections.resolve(filename);
        Files.createDirectory(collection);
        Files.createDirectory(collection.resolve(com.github.onsdigital.zebedee.model.Collection.APPROVED));
        Files.createDirectory(collection.resolve(com.github.onsdigital.zebedee.model.Collection.IN_PROGRESS));

        // Create the description:
        Path collectionDescription = collections.resolve(filename + ".json");
        CollectionDescription description = new CollectionDescription();
        description.name = name;
        try (OutputStream output = Files.newOutputStream(collectionDescription)) {
            Serialiser.serialise(output, description);
        }

        return collection;
    }

}
