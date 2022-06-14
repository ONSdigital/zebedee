package com.github.onsdigital.zebedee;

import com.github.onsdigital.JWTVerifier;
import com.github.onsdigital.JWTVerifierImpl;
import com.github.onsdigital.dp.files.api.APIClient;
import com.github.onsdigital.dp.image.api.client.ImageAPIClient;
import com.github.onsdigital.dp.image.api.client.ImageClient;
import com.github.onsdigital.dp.interactives.api.InteractivesAPIClient;
import com.github.onsdigital.slack.Profile;
import com.github.onsdigital.slack.client.SlackClient;
import com.github.onsdigital.slack.client.SlackClientImpl;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.kafka.KafkaClient;
import com.github.onsdigital.zebedee.kafka.KafkaClientImpl;
import com.github.onsdigital.zebedee.keyring.CollectionKeyCache;
import com.github.onsdigital.zebedee.keyring.CollectionKeyStore;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.keyring.central.CollectionKeyCacheImpl;
import com.github.onsdigital.zebedee.keyring.central.CollectionKeyStoreImpl;
import com.github.onsdigital.zebedee.keyring.central.CollectionKeyringImpl;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.RedirectTablePartialMatch;
import com.github.onsdigital.zebedee.model.encryption.EncryptionKeyFactory;
import com.github.onsdigital.zebedee.model.encryption.EncryptionKeyFactoryImpl;
import com.github.onsdigital.zebedee.model.publishing.PublishedCollections;
import com.github.onsdigital.zebedee.notification.StartUpNotifier;
import com.github.onsdigital.zebedee.permissions.service.JWTPermissionsServiceImpl;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.permissions.service.PermissionsServiceImpl;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.service.DatasetService;
import com.github.onsdigital.zebedee.service.ImageService;
import com.github.onsdigital.zebedee.service.ImageServiceImpl;
import com.github.onsdigital.zebedee.service.InteractivesService;
import com.github.onsdigital.zebedee.service.InteractivesServiceImpl;
import com.github.onsdigital.zebedee.service.KafkaService;
import com.github.onsdigital.zebedee.service.KafkaServiceImpl;
import com.github.onsdigital.zebedee.service.NoOpKafkaService;
import com.github.onsdigital.zebedee.service.ServiceStoreImpl;
import com.github.onsdigital.zebedee.service.StaticFilesService;
import com.github.onsdigital.zebedee.service.StaticFilesServiceImpl;
import com.github.onsdigital.zebedee.service.ZebedeeDatasetService;
import com.github.onsdigital.zebedee.session.service.JWTSessionsServiceImpl;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.session.service.ThreadLocalSessionsServiceImpl;
import com.github.onsdigital.zebedee.session.store.LegacySessionsStore;
import com.github.onsdigital.zebedee.session.store.LegacySessionsStoreImpl;
import com.github.onsdigital.zebedee.teams.service.StubbedTeamsServiceImpl;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import com.github.onsdigital.zebedee.teams.service.TeamsServiceImpl;
import com.github.onsdigital.zebedee.teams.store.TeamsStoreFileSystemImpl;
import com.github.onsdigital.zebedee.user.service.StubbedUsersServiceImpl;
import com.github.onsdigital.zebedee.user.service.UsersService;
import com.github.onsdigital.zebedee.user.service.UsersServiceImpl;
import com.github.onsdigital.zebedee.user.store.UserStoreFileSystemImpl;
import com.github.onsdigital.zebedee.util.slack.NopNotifierImpl;
import com.github.onsdigital.zebedee.util.slack.NopSlackClientImpl;
import com.github.onsdigital.zebedee.util.slack.NopStartUpNotifier;
import com.github.onsdigital.zebedee.util.slack.Notifier;
import com.github.onsdigital.zebedee.util.slack.SlackNotifier;
import com.github.onsdigital.zebedee.util.slack.SlackStartUpNotifier;
import com.github.onsdigital.zebedee.util.versioning.VersionsService;
import com.github.onsdigital.zebedee.util.versioning.VersionsServiceImpl;
import com.github.onsdigital.zebedee.verification.VerificationAgent;
import dp.api.dataset.DatasetAPIClient;
import dp.api.dataset.DatasetClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;
import static com.github.onsdigital.zebedee.Zebedee.COLLECTIONS;
import static com.github.onsdigital.zebedee.Zebedee.KEYRING;
import static com.github.onsdigital.zebedee.Zebedee.PERMISSIONS;
import static com.github.onsdigital.zebedee.Zebedee.PUBLISHED;
import static com.github.onsdigital.zebedee.Zebedee.PUBLISHED_COLLECTIONS;
import static com.github.onsdigital.zebedee.Zebedee.SERVICES;
import static com.github.onsdigital.zebedee.Zebedee.SESSIONS;
import static com.github.onsdigital.zebedee.Zebedee.TEAMS;
import static com.github.onsdigital.zebedee.Zebedee.USERS;
import static com.github.onsdigital.zebedee.Zebedee.ZEBEDEE;
import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static com.github.onsdigital.zebedee.configuration.Configuration.getDatasetAPIAuthToken;
import static com.github.onsdigital.zebedee.configuration.Configuration.getDatasetAPIURL;
import static com.github.onsdigital.zebedee.configuration.Configuration.getIdentityAPIURL;
import static com.github.onsdigital.zebedee.configuration.Configuration.getImageAPIURL;
import static com.github.onsdigital.zebedee.configuration.Configuration.getInitialRetryInterval;
import static com.github.onsdigital.zebedee.configuration.Configuration.getInteractivesAPIURL;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaContentUpdatedTopic;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaURL;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKeyringInitVector;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKeyringSecretKey;
import static com.github.onsdigital.zebedee.configuration.Configuration.getMaxRetryInterval;
import static com.github.onsdigital.zebedee.configuration.Configuration.getMaxRetryTimeout;
import static com.github.onsdigital.zebedee.configuration.Configuration.getServiceAuthToken;
import static com.github.onsdigital.zebedee.configuration.Configuration.getSlackSupportChannelID;
import static com.github.onsdigital.zebedee.configuration.Configuration.getStaticFilesAPIURL;
import static com.github.onsdigital.zebedee.configuration.Configuration.slackChannelsToNotfiyOnStartUp;
import static com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl.initialisePermissions;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Object encapsulating the set up configuration required by {@link Zebedee}.
 * Set paths to & create relevant
 * directories required by zebedee, create the service instances it requires
 * etc.
 */
public class ZebedeeConfiguration {
    private Path rootPath;
    private Path zebedeePath;
    private Path publishedContentPath;
    private Path publishedCollectionsPath;
    private Path collectionsPath;
    private Path usersPath;
    private Path sessionsPath;
    private Path permissionsPath;
    private Path teamsPath;
    private Path redirectPath;
    private Path servicePath;
    private Path keyRingPath;
    private boolean useVerificationAgent;
    private PublishedCollections publishedCollections;
    private Collections collections;
    private Content published;
    private PermissionsService permissionsService;
    private UsersService usersService;
    private TeamsService teamsService;
    private Sessions sessions;
    private DataIndex dataIndex;
    private DatasetService datasetService;
    private InteractivesService interactivesService;
    private ImageService imageService;
    private KafkaService kafkaService;
    private StaticFilesService staticFilesService;
    private CollectionKeyring collectionKeyring;
    private CollectionKeyCache schedulerKeyCache;
    private EncryptionKeyFactory encryptionKeyFactory;
    private StartUpNotifier startUpNotifier;
    private SlackClient slackClient;
    private Notifier slackNotifier;

    /**
     * Create a new configuration object.
     *
     * @param rootPath
     * @param enableVerificationAgent
     * @throws IOException
     */
    public ZebedeeConfiguration(Path rootPath, boolean enableVerificationAgent) throws IOException {
        if (Files.exists(rootPath)) {
            info().data("path", rootPath.toString()).log("setting Zebedee root directory");
        } else {
            throw new IllegalArgumentException("zebedee root directory doesn't not exist." + rootPath.toAbsolutePath());
        }

        // Create the zebedee file system structure
        this.rootPath = rootPath;
        this.zebedeePath = createDir(rootPath, ZEBEDEE);
        this.publishedContentPath = createDir(zebedeePath, PUBLISHED);
        this.collectionsPath = createDir(zebedeePath, COLLECTIONS);
        this.publishedCollectionsPath = createDir(zebedeePath, PUBLISHED_COLLECTIONS);
        this.usersPath = createDir(zebedeePath, USERS);
        this.sessionsPath = createDir(zebedeePath, SESSIONS);
        this.permissionsPath = createDir(zebedeePath, PERMISSIONS);
        this.teamsPath = createDir(zebedeePath, TEAMS);
        this.redirectPath = this.publishedContentPath.resolve(Content.REDIRECT);
        this.servicePath = createDir(zebedeePath, SERVICES);
        this.keyRingPath = createDir(zebedeePath, KEYRING);
        if (!Files.exists(redirectPath)) {
            Files.createFile(redirectPath);
        }
        this.useVerificationAgent = enableVerificationAgent;

        // Create the services and objects...
        this.dataIndex = new DataIndex(new FileSystemContentReader(publishedContentPath));
        this.publishedCollections = new PublishedCollections(publishedCollectionsPath);
        this.encryptionKeyFactory = new EncryptionKeyFactoryImpl();

        // TODO: Remove after migration to JWT sessions is complete
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            this.teamsService = new StubbedTeamsServiceImpl();
        } else {
            this.teamsService = new TeamsServiceImpl(
                    new TeamsStoreFileSystemImpl(teamsPath), this::getPermissionsService);
        }

        this.published = createPublished();

        initialisePermissions(permissionsPath);
        PermissionsStore permissionsStore = new PermissionsStoreFileSystemImpl(permissionsPath);
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            this.permissionsService = new JWTPermissionsServiceImpl(permissionsStore);
        } else {
            this.permissionsService = new PermissionsServiceImpl(permissionsStore);
        }

        InteractivesAPIClient interactivesClient = new InteractivesAPIClient(getInteractivesAPIURL(),
                getServiceAuthToken());
        interactivesService = new InteractivesServiceImpl(interactivesClient);

        VersionsService versionsService = new VersionsServiceImpl();
        this.collections = new Collections(collectionsPath, permissionsService, versionsService, interactivesService,
                published);

        // TODO: Remove after migration to JWT sessions is complete
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            this.usersService = StubbedUsersServiceImpl.getInstance();
        } else {
            this.usersService = UsersServiceImpl.getInstance(
                    new UserStoreFileSystemImpl(this.usersPath), permissionsService);
        }

        // Configure the sessions
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            JWTVerifier jwtVerifier = null;
            try {
                jwtVerifier = new JWTVerifierImpl(getIdentityAPIURL(), getInitialRetryInterval(), getMaxRetryTimeout(),
                        getMaxRetryInterval());
            } catch (Exception e) {
                error().logException(e, "failed to initialise JWT validator");
                throw new RuntimeException(e);
            }
            this.sessions = new JWTSessionsServiceImpl(jwtVerifier);
        } else {
            LegacySessionsStore legacySessionsStore = new LegacySessionsStoreImpl(sessionsPath);
            this.sessions = new ThreadLocalSessionsServiceImpl(legacySessionsStore, permissionsService, teamsService);
        }

        // Init the collection keyring and scheduler cache.
        initCollectionKeyring();

        DatasetClient datasetClient;
        try {
            datasetClient = new DatasetAPIClient(getDatasetAPIURL(), getDatasetAPIAuthToken(), getServiceAuthToken());
        } catch (URISyntaxException e) {
            error().logException(e, "failed to initialise dataset api client - invalid URI");
            throw new RuntimeException(e);
        }

        datasetService = new ZebedeeDatasetService(datasetClient);

        ImageClient imageClient;
        try {
            imageClient = new ImageAPIClient(getImageAPIURL(), getServiceAuthToken());
        } catch (URISyntaxException e) {
            error().logException(e, "failed to initialise image api client - invalid URI");
            throw new RuntimeException(e);
        }

        imageService = new ImageServiceImpl(imageClient);

        staticFilesService = new StaticFilesServiceImpl(new APIClient(getStaticFilesAPIURL(), getServiceAuthToken()));

        if (cmsFeatureFlags().isKafkaEnabled()) {

            KafkaClient kafkaClient = new KafkaClientImpl(getKafkaURL(), getKafkaContentUpdatedTopic());
            kafkaService = new KafkaServiceImpl(kafkaClient);
        } else {
            kafkaService = new NoOpKafkaService();
        }

        initSlackIntegration();

        info().data("root_path", rootPath.toString())
                .data("zebedee_path", zebedeePath.toString())
                .data("keyring_path", keyRingPath.toString())
                .data("published_content_path", publishedContentPath.toString())
                .data("collections_path", collectionsPath.toString())
                .data("published_collections_path", publishedCollectionsPath.toString())
                .data("users_path", usersPath.toString())
                .data("sessions_path", sessionsPath.toString())
                .data("permissions_path", permissionsPath.toString())
                .data("teams_path", teamsPath.toString())
                .data("services_path", servicePath.toString())
                .data("enable_verification_agent", useVerificationAgent)
                .data("sessions_api_enabled", cmsFeatureFlags().isSessionAPIEnabled())
                .log("zebedee configuration creation complete");
    }

    private void initCollectionKeyring() throws KeyringException {
        CollectionKeyStore keyStore = new CollectionKeyStoreImpl(
                getKeyRingPath(), getKeyringSecretKey(), getKeyringInitVector());

        CollectionKeyCacheImpl.init(keyStore);
        CollectionKeyCache collectionKeyCache = CollectionKeyCacheImpl.getInstance();

        this.schedulerKeyCache = collectionKeyCache;

        CollectionKeyringImpl.init(collectionKeyCache, permissionsService, collections);
        this.collectionKeyring = CollectionKeyringImpl.getInstance();
    }

    /**
     * Initalise the CMS's Slack integration. If the required configuration values
     * are not available the CMS will
     * default to No Op implementation - slack messages will be logged but sent to
     * the Slack API.
     */
    private void initSlackIntegration() {
        String slackToken = System.getenv("slack_api_token");
        List<String> startUpNotificationRecipients = slackChannelsToNotfiyOnStartUp();

        boolean validConfig = true;
        if (isEmpty(slackToken)) {
            warn().log("env var slack_api_token is null or empty");
            validConfig = false;
        }

        if (startUpNotificationRecipients == null || startUpNotificationRecipients.isEmpty()) {
            warn().log("env var START_UP_NOTIFY_LIST is null or empty");
            validConfig = false;
        }

        if (validConfig) {
            info().log("valid Slack configuation found for CMS/Slack integration");

            this.slackClient = new SlackClientImpl(new Profile.Builder()
                    .emoji(":flo:")
                    .username("Florence")
                    .authToken(slackToken)
                    .create());

            this.slackNotifier = new SlackNotifier(this.slackClient);

            this.startUpNotifier = new SlackStartUpNotifier(
                    slackClient,
                    startUpNotificationRecipients,
                    getSlackSupportChannelID());

        } else {
            warn().log("slack configuration missing/empty defaulting to No op implementation");

            this.slackClient = new NopSlackClientImpl();
            this.slackNotifier = new NopNotifierImpl();
            this.startUpNotifier = new NopStartUpNotifier();
        }
    }

    public boolean isUseVerificationAgent() {
        return useVerificationAgent;
    }

    public Path getZebedeePath() {
        return zebedeePath;
    }

    public Path getPublishedContentPath() {
        return publishedContentPath;
    }

    public Path getPublishedCollectionsPath() {
        return publishedCollectionsPath;
    }

    public Path getCollectionsPath() {
        return collectionsPath;
    }

    public Path getUsersPath() {
        return usersPath;
    }

    public Path getSessionsPath() {
        return sessionsPath;
    }

    public Path getPermissionsPath() {
        return permissionsPath;
    }

    public Path getTeamsPath() {
        return teamsPath;
    }

    public Path getRedirectPath() {
        return redirectPath;
    }

    public Path getKeyRingPath() {
        return keyRingPath;
    }

    public Path getServicePath() {
        return servicePath;
    }

    public Content getPublished() {
        return this.published;
    }

    private Content createPublished() {
        Content content = new Content(publishedContentPath);
        Path redirectPath = publishedContentPath.resolve(Content.REDIRECT);
        if (!Files.exists(redirectPath)) {
            content.setRedirects(new RedirectTablePartialMatch(content));
            try {
                Files.createFile(redirectPath);
            } catch (IOException e) {
                error().data("requestedPath", redirectPath.toString())
                        .logException(e, "could not save redirect to requested path");
            }
        } else {
            content.setRedirects(new RedirectTablePartialMatch(content, redirectPath));
        }
        return content;
    }

    public DataIndex getDataIndex() {
        return this.dataIndex;
    }

    public Collections getCollections() {
        return this.collections;
    }

    public PublishedCollections getPublishCollections() {
        return this.publishedCollections;
    }

    public Sessions getSessions() {
        return this.sessions;
    }

    public PermissionsService getPermissionsService() {
        return this.permissionsService;
    }

    public TeamsService getTeamsService() {
        return this.teamsService;
    }

    public UsersService getUsersService() {
        return this.usersService;
    }

    public VerificationAgent getVerificationAgent(boolean verificationIsEnabled, Zebedee z) {
        return isUseVerificationAgent() && verificationIsEnabled ? new VerificationAgent(z) : null;
    }

    public DatasetService getDatasetService() {
        return datasetService;
    }

    public InteractivesService getInteractivesService() {
        return interactivesService;
    }

    public ImageService getImageService() {
        return imageService;
    }

    public StaticFilesService getStaticFilesService() {
        return staticFilesService;
    }

    public KafkaService getKafkaService() {
        return kafkaService;
    }

    public ServiceStoreImpl getServiceStore() {
        return new ServiceStoreImpl(servicePath);
    }

    public CollectionKeyring getCollectionKeyring() {
        return this.collectionKeyring;
    }

    public EncryptionKeyFactory getEncryptionKeyFactory() {
        return this.encryptionKeyFactory;
    }

    public CollectionKeyCache getSchedulerKeyringCache() {
        return this.schedulerKeyCache;
    }

    private Path createDir(Path root, String dirName) throws IOException {
        Path dir = root.resolve(dirName);
        if (!Files.exists(dir)) {
            info().data("path", dirName).log("creating required Zebedee directory as it does not exist.");
            Files.createDirectory(dir);
        }
        return dir;
    }

    public StartUpNotifier getStartUpNotifier() {
        return startUpNotifier;
    }

    public Notifier getSlackNotifier() {
        return slackNotifier;
    }
}
