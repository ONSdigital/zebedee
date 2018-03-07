package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.ResourceUtils;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.ZebedeeConfiguration;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.json.serialiser.IsoDateSerializer;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.KeyManager;
import com.github.onsdigital.zebedee.model.csdb.CsdbImporter;
import com.github.onsdigital.zebedee.model.publishing.scheduled.PublishScheduler;
import com.github.onsdigital.zebedee.model.publishing.scheduled.Scheduler;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.util.SlackNotification;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

public class Root {

    private static final String DEFAULT_SYS_USER_EMAIL = "florence@magicroundabout.ons.gov.uk";
    private static final String DEFAULT_SYS_USER_NAME = "Florence";
    private static final String DEFAULT_SYS_USER_PASSWORD = "Doug4l";

    static final String ZEBEDEE_ROOT = "zebedee_root";
    // Environment variables are stored as a static variable so if necessary we can hijack them for testing
    public static Map<String, String> env = System.getenv();
    public static Zebedee zebedee;
    static Path root;
    private static Scheduler scheduler = new PublishScheduler();

    /**
     * Recursively lists all files within this {@link Content}.
     *
     * @param path  The path to start from. This method calls itself recursively.
     * @param files The list to which results will be added.
     * @throws IOException If a filesystem error occurs.
     */
    private static void listFiles(Path base, Path path, List<Path> files)
            throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    listFiles(base, entry, files);
                } else {
                    files.add(base.relativize(entry));
                }
            }
        }
    }

    public static void init() {

        logDebug("zebedee init").log();

        // Set ISO date formatting in Gson to match Javascript Date.toISODate()
        Serialiser.getBuilder().registerTypeAdapter(Date.class, new IsoDateSerializer());

        // Set the class that will be used to determine a ClassLoader when loading resources:
        ResourceUtils.classLoaderClass = Root.class;

        // If we have an environment variable and it is
        String rootDir = env.get(ZEBEDEE_ROOT);
        boolean zebedeeCreated = false;

        if (StringUtils.isNotEmpty(rootDir) && Files.exists(Paths.get(rootDir))) {
            root = Paths.get(rootDir);
            try {
                zebedee = initialiseZebedee(root);
                zebedeeCreated = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!zebedeeCreated) {
            try {
                // Create a Zebedee folder:
                root = Files.createTempDirectory("generated");
                zebedee = initialiseZebedee(root);
                logDebug("zebedee root created").addParameter("uri", root.toString()).log();
                ReaderConfiguration.init(root.toString());

                // Initialise content folders from bundle
                Path taxonomy = Paths.get(".").resolve(Configuration.getContentDirectory());
                List<Path> content = listContent(taxonomy);
                copyContent(content, taxonomy);
            } catch (IOException | UnauthorizedException | BadRequestException | NotFoundException e) {
                throw new RuntimeException("Error initialising Zebedee ", e);
            }
        }

        //Setting zebedee root as system property for zebedee reader module, since zebedee root is not set as environment variable on develop environment
        System.setProperty(ZEBEDEE_ROOT, root.toString());

        SlackNotification.alarm("Zebedee has just started. Ensure an administrator has logged in.");

        loadExistingCollectionsIntoScheduler();
        initialiseCsdbImportKeys();
        indexPublishedCollections();
        cleanupStaleCollectionKeys();
    }

    /**
     * If we have not previously generated a key for CSDB import, generate one and distribute it.
     */
    public static void initialiseCsdbImportKeys() {
        // if there is no key previously stored for CSDB import, generate a new one.
        if (!zebedee.getApplicationKeys().containsKey(CsdbImporter.APPLICATION_KEY_ID)) {
            // create new key pair
            logDebug("No key pair found for CSDB import. Generating and saving a new key.").log();
            try {
                SecretKey secretKey = zebedee.getApplicationKeys().generateNewKey(CsdbImporter.APPLICATION_KEY_ID);

                // distribute private key to all users.
                KeyManager.distributeApplicationKey(zebedee, CsdbImporter.APPLICATION_KEY_ID, secretKey);
            } catch (IOException e) {
                logError(e, "Failed to generate and save new application key for CSDB import.").log();
            }
        }
    }

    private static void cleanupStaleCollectionKeys() {
        try {
            for (User user : zebedee.getUsersService().list()) {
                zebedee.getUsersService().removeStaleCollectionKeys(user.getEmail());
            }
        } catch (IOException | NotFoundException | BadRequestException e) {
            logError(e).log();
        }
    }

    private static void indexPublishedCollections() {
//        try {
//            zebedee.publishedCollections.init(ElasticSearchClient.getClient());
//        } catch (IOException e) {
//            Log.print(e, "Exception indexing published collections: %s", e.getMessage());
//        }
    }

    private static void loadExistingCollectionsIntoScheduler() {
        if (Configuration.isSchedulingEnabled()) {

            logInfo("Adding existing collections to the scheduler.").log();

            Collections.CollectionList collections;
            try {
                collections = zebedee.getCollections().list();
            } catch (IOException e) {
                logError(e, "Failed to load collections list to schedule publishes").log();
                return;
            }

            for (Collection collection : collections) {
                schedulePublish(collection);
            }
        } else {
            logInfo("Scheduled publishing is disabled - not reading collections").log();
        }
    }


    public static void cancelPublish(Collection collection) {
        try {
            logInfo("Attempting to cancel collection publish.")
                    .collectionName(collection)
                    .collectionId(collection)
                    .addParameter("type", collection.getDescription().getType())
                    .log();
            scheduler.cancel(collection);
        } catch (Exception e) {
            logError(e, "Exception caught trying to cancel scheduled publish of collection").log();
        }
    }

    private static List<Path> listContent(Path taxonomy) throws IOException {
        List<Path> content = new ArrayList<>();

        // List the taxonomy files:
        listFiles(taxonomy, taxonomy, content);

        return content;
    }

    private static void copyContent(List<Path> content, Path taxonomy)
            throws IOException {

        // Extract the content:
        // Copy to the master and launchpad content directories
        for (Path item : content) {
            Path source = taxonomy.resolve(item);
            Path masterDestination = zebedee.getPublished().path.resolve(item);
            Files.createDirectories(masterDestination.getParent());
            try (InputStream input = Files.newInputStream(source);
                 OutputStream output = Files.newOutputStream(masterDestination)) {
                IOUtils.copy(input, output);
            }
        }
        logDebug("Zebedee root").addParameter("uri", root.toAbsolutePath()).log();
    }

    public static void schedulePublish(Collection collection) {
        scheduler.schedulePublish(collection, zebedee);
    }

    public static Scheduler getScheduler() {
        return scheduler;
    }

    private static Zebedee initialiseZebedee(Path root) throws IOException, NotFoundException, BadRequestException,
            UnauthorizedException {
        zebedee = new Zebedee(new ZebedeeConfiguration(root, true));
        createSystemUser();
        return zebedee;
    }

    private static void createSystemUser() throws NotFoundException, BadRequestException, UnauthorizedException, IOException {
        User user = new User();
        user.setEmail(DEFAULT_SYS_USER_EMAIL);
        user.setName(DEFAULT_SYS_USER_NAME);
        zebedee.getUsersService().createSystemUser(user, DEFAULT_SYS_USER_PASSWORD);
    }

    /**
     * Cleans up
     */
    @Override
    protected void finalize() throws Throwable {

    }
}
