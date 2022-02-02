package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.ResourceUtils;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.ZebedeeConfiguration;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.serialiser.IsoDateSerializer;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.publishing.scheduled.PublishScheduler;
import com.github.onsdigital.zebedee.model.publishing.scheduled.Scheduler;
import com.github.onsdigital.zebedee.notification.NotificationException;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.util.slack.AttachmentField;
import com.github.onsdigital.zebedee.util.slack.Notifier;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

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

import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.warn;
import static java.text.MessageFormat.format;

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
     * Initalise the CMS.
     */
    public static void init() {
        info().log("initalizing zebedee-cms");

        // Set ISO date formatting in Gson to match Javascript Date.toISODate()
        Serialiser.getBuilder().registerTypeAdapter(Date.class, new IsoDateSerializer());

        // Set the class that will be used to determine a ClassLoader when loading resources:
        ResourceUtils.classLoaderClass = Root.class;

        // If we have an environment variable and it is
        String rootDir = env.get(ZEBEDEE_ROOT);

        root = validateZebedeeRootConfig(rootDir);
        try {
            zebedee = initialiseZebedee(root);
            info().data(ZEBEDEE_ROOT, rootDir)
                    .log("successfully initalized zebedeed cms using the specified environment config");
        } catch (Exception ex) {
            throw new RuntimeException("error attempting to initalize zebedee cms", ex);
        }

        // Setting zebedee root as system property for zebedee reader module, since zebedee root is not set as
        // environment variable on develop environment
        System.setProperty(ZEBEDEE_ROOT, root.toString());

        try {
            Collections.CollectionList collections = zebedee.getCollections().list();
            loadExistingCollectionsIntoScheduler(collections);
        } catch (IOException ex) {
            throw new RuntimeException("failed to load collections list on startup", ex);
        }

        boolean notificationSuccessful = false;
        try {
            notificationSuccessful = zebedee.getStartUpNotifier().notifyStartUpComplete();
        } catch (NotificationException ex) {
            error().exception(ex).log("error sending CMS start up notifications");
        }

        if (!notificationSuccessful) {
            warn().log("sending CMS start up notifications unsuccessful, it is strongly suggested that you " +
                    "investigate this issue.");
        }

        info().data(ZEBEDEE_ROOT, rootDir).log("zebedee cmd initialization completed successfully");
    }

    /**
     * Validate the zebedee root path meets the requirements - not null/empty, the file exists and is a directory.
     * Throws {@link RuntimeException} if any of the checks fail.
     *
     * @param rootDir the path to validate.
     * @return the root dir value as a {@link Path} object.
     */
    private static Path validateZebedeeRootConfig(String rootDir) {
        if (StringUtils.isEmpty(rootDir)) {
            throw new RuntimeException("zebedee_root env var required but none provided");
        }

        Path p = Paths.get(rootDir);
        if (Files.notExists(p)) {
            throw new RuntimeException(
                    format("invalid zebedee_root config: {0} directory does not exist", rootDir));
        }

        if (!Files.isDirectory(p)) {
            throw new RuntimeException(
                    format("invalid zebedee_root config:{0} the specified path is not a directory", rootDir));
        }

        info().data(ZEBEDEE_ROOT, rootDir)
                .log("successfully validated zebedee_root config proceeding beginning CMS initialization");
        return p;
    }

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

    private static void loadExistingCollectionsIntoScheduler(Collections.CollectionList collections) {
        if (Configuration.isSchedulingEnabled()) {

            info().log("zebedee root: adding existing collections to the scheduler.");
            for (Collection collection : collections) {
                schedulePublish(collection);
            }
        } else {
            info().log("zebedee root: scheduled publishing is disabled - not reading collections");
        }
    }

    static void alertOnInProgressCollections(Collections.CollectionList collections, Notifier notifier) {
        info().log("zebedee root: checking existing collections for in progress approvals");

        String channel = Configuration.getDefaultSlackAlarmChannel();

        collections.withApprovalInProgressOrError().forEach(c -> {
            info().data("collectionId", c.getDescription().getId())
                    .data("type", c.getDescription().getType().name())
                    .log("zebedee root: collection approval is in error or in progress state on zebedee startup");
            AttachmentField status = new AttachmentField("Approval Status", c.getDescription().getApprovalStatus().name(), true);
            notifier.sendCollectionAlarm(c, channel, "Collection approval is in IN_PROGRESS or ERROR state on zebedee startup. It may need to be re-approved manually.", status);
        });
    }

    public static void cancelPublish(Collection collection) {
        try {
            info().data("collection_id", collection.description.getId())
                    .data("type", collection.getDescription().getType().name())
                    .log("zebedee root: cancelling scheduled collection publish");
            scheduler.cancel(collection);
        } catch (Exception e) {
            error().data("collection_id", collection.getId()).logException(e, "zebedee root: error cancelling scheduled publish of collection");
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
        info().data("uri", root.toAbsolutePath()).log("Zebedee root: absolute path");
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

        // TODO: Remove this logic after migration to using the dp-identity-api
        if (!cmsFeatureFlags().isJwtSessionsEnabled()) {
            createSystemUser();
        }

        return zebedee;
    }

    private static void createSystemUser() throws NotFoundException, BadRequestException, UnauthorizedException, IOException {
        User user = new User();
        user.setEmail(DEFAULT_SYS_USER_EMAIL);
        user.setName(DEFAULT_SYS_USER_NAME);
        zebedee.getUsersService().createSystemUser(user, DEFAULT_SYS_USER_PASSWORD);
    }
}
