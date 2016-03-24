package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.ResourceUtils;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.json.UserList;
import com.github.onsdigital.zebedee.json.serialiser.IsoDateSerializer;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.KeyManager;
import com.github.onsdigital.zebedee.model.csdb.CsdbImporter;
import com.github.onsdigital.zebedee.model.publishing.scheduled.PublishScheduler;
import com.github.onsdigital.zebedee.model.publishing.scheduled.Scheduler;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.util.Log;
import com.github.onsdigital.zebedee.util.SlackNotification;
import org.apache.commons.io.IOUtils;

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

public class Root {
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

        System.out.println("zebedee init: ");

        // Set ISO date formatting in Gson to match Javascript Date.toISODate()
        Serialiser.getBuilder().registerTypeAdapter(Date.class, new IsoDateSerializer());

        // Set the class that will be used to determine a ClassLoader when loading resources:
        ResourceUtils.classLoaderClass = Root.class;

        // If we have an environment variable and it is
        String rootDir = env.get(ZEBEDEE_ROOT);
        boolean zebedeeCreated = false;
        if (rootDir != null && rootDir != "" && Files.exists(Paths.get(rootDir))) {
            root = Paths.get(rootDir);
            try {
                zebedee = Zebedee.create(root);
                zebedeeCreated = true;
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        if (!zebedeeCreated) {
            try {
                // Create a Zebedee folder:
                root = Files.createTempDirectory("zebedee");
                zebedee = Zebedee.create(root);
                System.out.println("zebedee root: " + root.toString());
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
        System.setProperty("zebedee_root", root.toString());

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
        if (!zebedee.applicationKeys.containsKey(CsdbImporter.APPLICATION_KEY_ID)) {
            // create new key pair
            Log.print("No key pair found for CSDB import. Generating and saving a new key.");
            try {
                SecretKey secretKey = zebedee.applicationKeys.generateNewKey(CsdbImporter.APPLICATION_KEY_ID);

                // distribute private key to all users.
                KeyManager.disributeApplicationKey(zebedee, CsdbImporter.APPLICATION_KEY_ID, secretKey);
            } catch (IOException e) {
                Log.print(e, "Failed to generate and save new application key for CSDB import.");
            }
        }
    }

    private static void cleanupStaleCollectionKeys() {
        try {
            UserList users = zebedee.users.list();

            for (User user : users) {
                com.github.onsdigital.zebedee.model.Users.cleanupCollectionKeys(zebedee, user);
            }

        } catch (IOException e) {
            Log.print(e, "");
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

            System.out.println("Adding existing collections to the scheduler.");

            Collections.CollectionList collections;
            try {
                collections = zebedee.collections.list();
            } catch (IOException e) {
                System.out.println("*** Failed to load collections list to schedule publishes ***");
                return;
            }

            for (Collection collection : collections) {
                schedulePublish(collection);
            }
        } else {
            Log.print("Scheduled publishing is disabled - not reading collections");
        }
    }


    public static void cancelPublish(Collection collection) {
        try {
            System.out.println("Attempting to cancel publish for collection " + collection.description.name + " type=" + collection.description.type);
            scheduler.cancel(collection);
        } catch (Exception e) {
            System.out.println("Exception caught trying to cancel scheduled publish of collection: " + e.getMessage());
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
            Path masterDestination = zebedee.published.path.resolve(item);
            Files.createDirectories(masterDestination.getParent());
            try (InputStream input = Files.newInputStream(source);
                 OutputStream output = Files.newOutputStream(masterDestination)) {
                IOUtils.copy(input, output);
            }
        }

        System.out.println("Zebedee root is at: " + root.toAbsolutePath());
    }

    public static void schedulePublish(Collection collection) {
        scheduler.schedulePublish(collection, zebedee);
    }

    public static Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * Cleans up
     */
    @Override
    protected void finalize() throws Throwable {

    }
}
