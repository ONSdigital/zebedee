package com.github.onsdigital.zebedee;

import com.github.davidcarboni.cryptolite.Password;
import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.*;
import com.github.onsdigital.zebedee.model.Collection;
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
    public String[] teamNames = {"Economy Team", "Labour Market Team"};
    public Path parent;
    public Path zebedee;
    public List<Path> collections;
    public List<String> teams;
    public List<String> contentUris;

    public User administrator;
    public User publisher;
    public User reviewer;

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

        // Create the teams
        teams = new ArrayList<>();

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
        administrator = jukesie;

        User patricia = new User();
        patricia.name = "Patricia Pumpkin";
        patricia.email = "patricia@example.com";
        patricia.passwordHash = Password.hash("password");
        patricia.inactive = false;
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(patricia.email) + ".json"))) {
            Serialiser.serialise(outputStream, patricia);
        }
        publisher = patricia;

        User freddy = new User();
        freddy.name = "freddy Pumpkin";
        freddy.email = "freddy@example.com";
        freddy.passwordHash = Password.hash("password");
        freddy.inactive = false;
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(freddy.email) + ".json"))) {
            Serialiser.serialise(outputStream, freddy);
        }
        reviewer = freddy;

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
        Path teams = zebedee.resolve(Zebedee.TEAMS);
        Files.createDirectories(teams);

        AccessMapping accessMapping = new AccessMapping();

        accessMapping.administrators = new HashSet<>();
        accessMapping.administrators.add(jukesie.email);

        accessMapping.digitalPublishingTeam = new HashSet<>();
        accessMapping.digitalPublishingTeam.add(patricia.email);
        accessMapping.digitalPublishingTeam.add(freddy.email);

        CollectionDescription collectionDescription = new CollectionDescription();
        collectionDescription.id = Random.id();
        accessMapping.collections = new HashMap<>();

        Zebedee z = new Zebedee(zebedee);
        Team labourMarketTeam = createTeam(freddy, "Labour Market Team", teams);
        Team inflationTeam = createTeam(ronny, "Inflation and Price Indices Team", teams);
        accessMapping.collections.put(new Collection(collections.get(0), z).description.id, set(labourMarketTeam));
        accessMapping.collections.put(new Collection(collections.get(1), z).description.id, set(inflationTeam));

        Path path = permissions.resolve("accessMapping.json");
        try (OutputStream output = Files.newOutputStream(path)) {
            Serialiser.serialise(output, accessMapping);
        }
    }

    private Set<Integer> set(Team team) {
        Set<Integer> ids = new HashSet<>();
        ids.add(team.id);
        return ids;
    }

    static int teamId;

    private Team createTeam(User user, String name, Path teams) throws IOException {
        Team team = new Team();

        team.id = ++teamId;
        team.name = name;
        team.members = new HashSet<>();
        team.members.add(user.email);
        Path labourMarketTeamPath = teams.resolve(PathUtils.toFilename(team.name));
        try (OutputStream output = Files.newOutputStream(labourMarketTeamPath)) {
            Serialiser.serialise(output, team);
        }

        return team;
    }

    public void delete() throws IOException {
        System.out.println("Deleting zebedee at " + parent);
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
     * Creates an reviewed file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    public Path createReviewedFile(String uri) throws IOException {

        return createFile(Collection.REVIEWED, uri);
    }

    /**
     * Creates a complete file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    public Path createCompleteFile(String uri) throws IOException {

        return createFile(Collection.COMPLETE, uri);
    }

    /**
     * Creates a complete file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    public Path createInProgressFile(String uri) throws IOException {

        return createFile(Collection.IN_PROGRESS, uri);
    }

    /**
     * Creates a file in the given directory.
     *
     * @param directory The directory to be created.
     * @param uri       The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    private Path createFile(String directory, String uri) throws IOException {

        Path inProgress = collections.get(1).resolve(directory);
        Path content = inProgress.resolve(uri.substring(1));
        Files.createDirectories(content.getParent());
        Files.createFile(content);
        return content;
    }

    /**
     * Creates an reviewed file in a different {@link com.github.onsdigital.zebedee.model.Collection}.
     *
     * @param uri        The URI to be created.
     * @param collection The {@link com.github.onsdigital.zebedee.model.Collection} in which to create the content.
     * @throws IOException If a filesystem error occurs.
     */
    public void isBeingEditedElsewhere(String uri, int collection) throws IOException {

        Path reviewed = collections.get(collection)
                .resolve(com.github.onsdigital.zebedee.model.Collection.REVIEWED);
        Path content = reviewed.resolve(uri.substring(1));
        Files.createDirectories(content.getParent());
        Files.createFile(content);
    }

    public Session createSession(String email) throws IOException {

        // Build the session object
        Session session = new Session();
        session.id = Random.id();
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
        Files.createDirectory(path.resolve(Zebedee.TEAMS));
        System.out.println("Created zebedee at " + path);
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
        Files.createDirectory(collection.resolve(com.github.onsdigital.zebedee.model.Collection.REVIEWED));
        Files.createDirectory(collection.resolve(com.github.onsdigital.zebedee.model.Collection.COMPLETE));
        Files.createDirectory(collection.resolve(com.github.onsdigital.zebedee.model.Collection.IN_PROGRESS));

        // Create the description:
        Path collectionDescription = collections.resolve(filename + ".json");
        CollectionDescription description = new CollectionDescription();
        description.id = Random.id();
        description.name = name;
        try (OutputStream output = Files.newOutputStream(collectionDescription)) {
            Serialiser.serialise(output, description);
        }

        return collection;
    }

}
