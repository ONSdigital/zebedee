package com.github.onsdigital.zebedee;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.json.*;
import com.github.onsdigital.zebedee.json.serialiser.IsoDateSerializer;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.permissions.model.AccessMapping;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.user.model.User;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 * {@link Deprecated} Please do not use this any more.
 */
@Deprecated
public class Builder {

    public static final String COLLECTION_ONE_NAME = "inflationq22015";
    public static final String COLLECTION_TWO_NAME = "labourmarketq22015";

    private static int teamId;
    private static User administratorTemplate;
    private static User publisher1Template;
    private static User publisher2Template;
    private static User reviewer1Template;
    private static User reviewer2Template;
    private static User dataVisTemplate;
    private static boolean usersInitialised = false;

    public String[] collectionNames = {"Inflation Q2 2015", "Labour Market Q2 2015"};
    public String[] teamNames = {"Economy Team", "Labour Market Team"};
    public Path parent;
    public Path zebedeeRootPath;
    public List<Path> collections;
    public List<String> teams;
    public List<String> contentUris;
    public User administrator;
    public User publisher1;
    public User publisher2;
    public User dataVis;
    public User reviewer1;
    public User reviewer2;
    public Credentials administratorCredentials;
    public Credentials publisher1Credentials;
    public Credentials publisher2Credentials;
    public Credentials dataVisCredentials;
    public Credentials reviewer1Credentials;
    public Credentials reviewer2Credentials;
    public Team labourMarketTeam;
    public Team inflationTeam;

    private Zebedee zebedee;

    /**
     * Constructor to build a known {@link Zebedee} structure with minimal structure for testing.
     *
     * @throws IOException
     */
    public Builder() throws IOException, CollectionNotFoundException {

        setupUsers();

        Root.env = new HashMap<>();

        // Create the structure:
        parent = Files.createTempDirectory(Random.id());
        zebedeeRootPath = createZebedee(parent);

        // Create the collections:
        collections = new ArrayList<>();
        for (String collectionName : collectionNames) {
            Path collection = createCollection(collectionName, zebedeeRootPath);
            collections.add(collection);
        }

        // Create the teams
        teams = new ArrayList<>();

        // Create some published content:
        Path folder = zebedeeRootPath.resolve(Zebedee.PUBLISHED);
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
        Path users = zebedeeRootPath.resolve(Zebedee.USERS);
        Files.createDirectories(users);

        administrator = clone(administratorTemplate);
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(administrator.getEmail()) + ".json"))) {
            Serialiser.serialise(outputStream, administrator);
        }

        publisher1 = clone(publisher1Template);
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(publisher1.getEmail()) + ".json"))) {
            Serialiser.serialise(outputStream, publisher1);
        }

        publisher2 = clone(publisher2Template);
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(publisher2.getEmail()) + ".json"))) {
            Serialiser.serialise(outputStream, publisher2);
        }

        reviewer1 = clone(reviewer1Template);
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(reviewer1.getEmail()) + ".json"))) {
            Serialiser.serialise(outputStream, reviewer1);
        }

        reviewer2 = clone(reviewer2Template);
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(reviewer2.getEmail()) + ".json"))) {
            Serialiser.serialise(outputStream, reviewer2);
        }

        dataVis = clone(dataVisTemplate);
        try (OutputStream outputStream = Files.newOutputStream(users.resolve(PathUtils.toFilename(dataVis.getEmail()) + ".json"))) {
            Serialiser.serialise(outputStream, dataVis);
        }

        administratorCredentials = userCredentials(administrator);
        publisher1Credentials = userCredentials(publisher1);
        publisher2Credentials = userCredentials(publisher2);
        reviewer1Credentials = userCredentials(reviewer1);
        reviewer2Credentials = userCredentials(reviewer2);
        dataVisCredentials = userCredentials(dataVis);

        Path sessions = zebedeeRootPath.resolve(Zebedee.SESSIONS);
        Files.createDirectories(sessions);

        // Set up some permissions:
        Path permissions = zebedeeRootPath.resolve(Zebedee.PERMISSIONS);
        Files.createDirectories(permissions);
        Path teams = zebedeeRootPath.resolve(Zebedee.TEAMS);
        Files.createDirectories(teams);

        AccessMapping accessMapping = new AccessMapping();

        accessMapping.setAdministrators(new HashSet<>());
        accessMapping.setDigitalPublishingTeam(new HashSet<>());

        accessMapping.getAdministrators().add(administrator.getEmail());
        accessMapping.getDigitalPublishingTeam().add(publisher1.getEmail());
        accessMapping.getDigitalPublishingTeam().add(publisher2.getEmail());

        CollectionDescription collectionDescription = new CollectionDescription();
        collectionDescription.setId(Random.id());
        accessMapping.setCollections(new HashMap<>());


        ZebedeeConfiguration configuration = new ZebedeeConfiguration(parent, false);
        this.zebedee = new Zebedee(configuration);

        inflationTeam = createTeam(reviewer1, teamNames[0], teams);
        labourMarketTeam = createTeam(reviewer2, teamNames[1], teams);
        accessMapping.getCollections().put(new Collection(collections.get(0), zebedee).description.getId(), set(inflationTeam));
        accessMapping.getCollections().put(new Collection(collections.get(1), zebedee).description.getId(),
                set(labourMarketTeam));

        Path path = permissions.resolve("accessMapping.json");
        try (OutputStream output = Files.newOutputStream(path)) {
            Serialiser.serialise(output, accessMapping);
        }
    }

    /**
     * Constructor to build an instance of zebedee using a predefined set of content
     *
     * @param bootStrap
     * @throws IOException
     */
    public Builder(Path bootStrap) throws IOException, CollectionNotFoundException {
        this();

        FileUtils.deleteDirectory(this.zebedeeRootPath.resolve(Zebedee.PUBLISHED).toFile());
        FileUtils.deleteDirectory(this.zebedeeRootPath.resolve(Zebedee.LAUNCHPAD).toFile());
        FileUtils.deleteDirectory(this.zebedeeRootPath.resolve(Zebedee.COLLECTIONS).toFile());
        Files.createDirectory(this.zebedeeRootPath.resolve(Zebedee.PUBLISHED));
        Files.createDirectory(this.zebedeeRootPath.resolve(Zebedee.LAUNCHPAD));
        Files.createDirectory(this.zebedeeRootPath.resolve(Zebedee.COLLECTIONS));

        FileUtils.copyDirectory(bootStrap.resolve(Zebedee.PUBLISHED).toFile(), this.zebedeeRootPath.resolve(Zebedee.PUBLISHED).toFile());
        if (Files.exists(bootStrap.resolve(Zebedee.LAUNCHPAD))) {
            FileUtils.copyDirectory(bootStrap.resolve(Zebedee.LAUNCHPAD).toFile(), this.zebedeeRootPath.resolve(Zebedee.LAUNCHPAD).toFile());
        } else {
            FileUtils.copyDirectory(bootStrap.resolve(Zebedee.PUBLISHED).toFile(), this.zebedeeRootPath.resolve(Zebedee.LAUNCHPAD).toFile()); // Not bothering with distinct launchpad
        }

        if (Files.exists(bootStrap.resolve(Zebedee.COLLECTIONS))) {
            FileUtils.copyDirectory(bootStrap.resolve(Zebedee.COLLECTIONS).toFile(), this.zebedeeRootPath.resolve(Zebedee.COLLECTIONS).toFile());
        }
    }

    public static User clone(User user) {
        User clone = new User();

        clone.setName(user.getName());
        clone.setEmail(user.getEmail());
        clone.setInactive(user.getInactive());
        clone.setTemporaryPassword(user.getTemporaryPassword());
        clone.setLastAdmin(user.getLastAdmin());
        clone(clone, user, "passwordHash");
        clone(clone, user, "keyring");
        return clone;
    }

    static void clone(User clone, User user, String fieldName) {
        try {
            Field field = User.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(clone, field.get(user));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Error cloning user", e);
        }
    }

    private synchronized void setupUsers() {

        if (!usersInitialised) {
            usersInitialised = true;

            // Set ISO date formatting in Gson to match Javascript Date.toISODate()
            Serialiser.getBuilder().registerTypeAdapter(Date.class, new IsoDateSerializer());

            logDebug("Generating test users and keys...").log();

            User jukesie = new User();
            jukesie.setName("Matt Jukes");
            jukesie.setEmail("jukesie@example.com");
            jukesie.setInactive(false);
            administratorTemplate = jukesie;
            jukesie.resetPassword("password");

            User patricia = clone(jukesie);
            patricia.setName("Patricia Pumpkin");
            patricia.setEmail("patricia@example.com");
            patricia.setInactive(false);
            publisher1Template = patricia;

            User bernard = clone(jukesie);
            bernard.setName("Bernard Black");
            bernard.setEmail("bernard@example.com");
            bernard.setInactive(false);
            publisher2Template = bernard;

            User freddy = clone(jukesie);
            freddy.setName("freddy Pumpkin");
            freddy.setEmail("freddy@example.com");
            freddy.setInactive(false);
            reviewer1Template = freddy;

            User ronny = clone(jukesie);
            ronny.setName("Ronny Roller");
            ronny.setName("ronny@example.com");
            ronny.setInactive(false);
            reviewer2Template = ronny;

            User dataVis = clone(jukesie);
            dataVis.setName("dataVis");
            dataVis.setEmail("datavis@example.com");
            dataVis.setInactive(false);
            dataVisTemplate = dataVis;
        }
    }

    private Credentials userCredentials(User user) {
        Credentials credentials = new Credentials();
        credentials.setEmail(user.getEmail());
        credentials.setPassword("password");
        return credentials;
    }

    private Set<Integer> set(Team team) {
        Set<Integer> ids = new HashSet<>();
        ids.add(team.getId());
        return ids;
    }

    private Team createTeam(User user, String name, Path teams) throws IOException {
        Team team = new Team();

        team.setId(++teamId);
        team.setName(name);
        team.addMember(user.getEmail());
        Path labourMarketTeamPath = teams.resolve(PathUtils.toFilename(team.getName() + ".json"));
        try (OutputStream output = Files.newOutputStream(labourMarketTeamPath)) {
            Serialiser.serialise(output, team);
        }

        return team;
    }

    public void delete() throws IOException {
        FileUtils.deleteDirectory(parent.toFile());
    }

    /**
     * Creates a published file.
     *
     * @param uri The URI to be created.
     * @throws IOException If a filesystem error occurs.
     */
    public Path createPublishedFile(String uri) throws IOException {

        Path published = zebedeeRootPath.resolve(Zebedee.PUBLISHED);
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
        session.setId(Random.id());
        session.setEmail(email);

        // Determine the path in which to create the session Json
        Path sessionPath;
        String sessionFileName = PathUtils.toFilename(session.getId());
        sessionFileName += ".json";
        sessionPath = zebedeeRootPath.resolve(Zebedee.SESSIONS).resolve(sessionFileName);

        // Serialise
        try (OutputStream output = Files.newOutputStream(sessionPath)) {
            Serialiser.serialise(output, session);
        }

        return session;
    }

    public Session createSession(User user) throws IOException {

        // Build the session object
        Session session = new Session();
        session.setId(Random.id());
        session.setEmail(user.getEmail());

        // Determine the path in which to create the session Json
        Path sessionPath;
        String sessionFileName = PathUtils.toFilename(session.getId());
        sessionFileName += ".json";
        sessionPath = zebedeeRootPath.resolve(Zebedee.SESSIONS).resolve(sessionFileName);

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
        Files.createDirectory(path.resolve(Zebedee.LAUNCHPAD));
        Files.createDirectory(path.resolve(Zebedee.PUBLISHED_COLLECTIONS));
        Files.createDirectory(path.resolve(Zebedee.APPLICATION_KEYS));
        return path;
    }

    /**
     * This method creates the expected set of folders for a Zebedee structure.
     * This code is intentionally copied from
     * <p>
     * This ensures there's a fixed expectation, rather than relying on a method that will be tested as part
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
        description.setId(Random.id());
        description.setName(name);
        try (OutputStream output = Files.newOutputStream(collectionDescription)) {
            Serialiser.serialise(output, description);
        }

        return collection;
    }

    /**
     * Builds simple random walk timeseries to
     *
     * @param name
     * @return
     */
    public Path randomWalkTimeSeries(String name) {
        return randomWalkTimeSeries(name, true, true, true, 10, 2015);
    }

    public Path randomWalkTimeSeries(String name, boolean withYears, boolean withQuarters, boolean withMonths, int yearsToGenerate) {
        return randomWalkTimeSeries(name, withYears, withQuarters, withMonths, yearsToGenerate, 2015);
    }

    public Path randomWalkTimeSeries(String name, boolean withYears, boolean withQuarters, boolean withMonths, int yearsToGenerate, int finalYear) {
        TimeSeries timeSeries = new TimeSeries();
        timeSeries.setDescription(new PageDescription());

        timeSeries.getDescription().setCdid(name);
        timeSeries.getDescription().setTitle(name);

        String[] months = "JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC".split(",");
        String[] quarters = "Q1,Q2,Q3,Q4".split(",");

        double val = 100.0;
        NormalDistribution distribution = new NormalDistribution(0, 10);
        for (int y = finalYear - yearsToGenerate + 1; y <= finalYear; y++) {
            if (withYears) {
                TimeSeriesValue value = new TimeSeriesValue();

                value.date = y + "";
                value.year = y + "";
                value.value = String.format("%.1f", val);
                timeSeries.years.add(value);
            }
            for (int q = 0; q < 4; q++) {
                if (withQuarters) {
                    TimeSeriesValue value = new TimeSeriesValue();
                    value.year = y + "";
                    value.quarter = quarters[q];

                    value.date = y + " " + quarters[q];
                    value.value = String.format("%.1f", val);
                    timeSeries.quarters.add(value);
                }
                for (int mInQ = 0; mInQ < 3; mInQ++) {
                    if (withMonths) {
                        TimeSeriesValue value = new TimeSeriesValue();
                        value.month = months[3 * q + mInQ];
                        value.year = y + "";

                        value.date = y + " " + months[3 * q + mInQ];
                        value.value = String.format("%.1f", val);
                        timeSeries.months.add(value);
                    }
                    val = val + distribution.sample();
                }
            }
        }


        // Save the timeseries
        Path path = null;
        try {
            path = Files.createTempFile(name, ".json");
            try (OutputStream output = Files.newOutputStream(path)) {
                Serialiser.serialise(output, timeSeries);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    public Path randomWalkQuarters(String name, int startYear, int startQuarter, int datapoints) {
        TimeSeries timeSeries = new TimeSeries();
        timeSeries.setDescription(new PageDescription());

        timeSeries.getDescription().setCdid(name);
        timeSeries.getDescription().setTitle(name);

        String[] quarters = "Q1,Q2,Q3,Q4".split(",");

        double val = 100.0;
        NormalDistribution distribution = new NormalDistribution(0, 10);
        for (int pt = 0; pt < datapoints; pt++) {
            val = val + distribution.sample();

            TimeSeriesValue value = new TimeSeriesValue();
            int quarter = (startQuarter + pt) % 4;
            int year = startYear + ((pt + startQuarter) / 4);

            value.date = year + " " + quarters[quarter];
            value.value = String.format("%.1f", val);

            timeSeries.quarters.add(value);

        }

        // Save the timeseries
        Path path = null;
        try {
            path = Files.createTempFile(name, ".json");
            try (OutputStream output = Files.newOutputStream(path)) {
                Serialiser.serialise(output, timeSeries);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }


    public Path randomWalkMonths(String name, int startYear, int startMonth, int datapoints) {
        TimeSeries timeSeries = new TimeSeries();
        timeSeries.setDescription(new PageDescription());

        timeSeries.getDescription().setCdid(name);
        timeSeries.getDescription().setTitle(name);

        String[] months = "JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC".split(",");

        double val = 100.0;
        NormalDistribution distribution = new NormalDistribution(0, 10);
        for (int pt = 0; pt < datapoints; pt++) {
            val = val + distribution.sample();

            TimeSeriesValue value = new TimeSeriesValue();
            int month = (startMonth + pt) % 12;
            int year = startYear + ((pt + startMonth) / 12);

            value.date = year + " " + months[month];
            value.value = String.format("%.1f", val);

            timeSeries.months.add(value);

        }

        // Save the timeseries
        Path path = null;
        try {
            path = Files.createTempFile(name, ".json");
            try (OutputStream output = Files.newOutputStream(path)) {
                Serialiser.serialise(output, timeSeries);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return path;
    }

    public Zebedee getZebedee() {
        return zebedee;
    }
}
