package com.github.onsdigital.zebedee;

import com.github.onsdigital.JWTVerifier;
import com.github.onsdigital.JWTVerifierImpl;
import com.github.onsdigital.dp.image.api.client.ImageAPIClient;
import com.github.onsdigital.dp.image.api.client.ImageClient;
import com.github.onsdigital.slack.Profile;
import com.github.onsdigital.slack.client.SlackClient;
import com.github.onsdigital.slack.client.SlackClientImpl;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.kafka.KafkaClient;
import com.github.onsdigital.zebedee.kafka.KafkaClientImpl;
import com.github.onsdigital.zebedee.keyring.CollectionKeyCache;
import com.github.onsdigital.zebedee.keyring.CollectionKeyStore;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.keyring.central.CollectionKeyCacheImpl;
import com.github.onsdigital.zebedee.keyring.central.CollectionKeyStoreImpl;
import com.github.onsdigital.zebedee.keyring.central.CollectionKeyringImpl;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.PublishedContent;
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
import com.github.onsdigital.zebedee.service.KafkaService;
import com.github.onsdigital.zebedee.service.KafkaServiceImpl;
import com.github.onsdigital.zebedee.service.NoOpKafkaService;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.service.ZebedeeDatasetService;
import com.github.onsdigital.zebedee.servicetokens.store.ServiceStore;
import com.github.onsdigital.zebedee.servicetokens.store.ServiceStoreImpl;
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
import com.github.onsdigital.zebedee.user.store.UserStore;
import com.github.onsdigital.zebedee.user.store.UserStoreFileSystemImpl;
import com.github.onsdigital.zebedee.util.slack.NopNotifierImpl;
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
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;
import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static com.github.onsdigital.zebedee.configuration.Configuration.getDatasetAPIAuthToken;
import static com.github.onsdigital.zebedee.configuration.Configuration.getDatasetAPIURL;
import static com.github.onsdigital.zebedee.configuration.Configuration.getDefaultVerificationUrl;
import static com.github.onsdigital.zebedee.configuration.Configuration.getIdentityAPIURL;
import static com.github.onsdigital.zebedee.configuration.Configuration.getImageAPIURL;
import static com.github.onsdigital.zebedee.configuration.Configuration.getInitialRetryInterval;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaContentPublishedTopic;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaURL;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKeyringInitVector;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKeyringSecretKey;
import static com.github.onsdigital.zebedee.configuration.Configuration.getMaxRetryInterval;
import static com.github.onsdigital.zebedee.configuration.Configuration.getMaxRetryTimeout;
import static com.github.onsdigital.zebedee.configuration.Configuration.getServiceAuthToken;
import static com.github.onsdigital.zebedee.configuration.Configuration.getSlackSupportChannelID;
import static com.github.onsdigital.zebedee.configuration.Configuration.getSlackToken;
import static com.github.onsdigital.zebedee.configuration.Configuration.isVerificationEnabled;
import static com.github.onsdigital.zebedee.configuration.Configuration.slackChannelsToNotfiyOnStartUp;
import static com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl.initialiseAccessMapping;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Object encapsulating the set up configuration required by {@link Zebedee}. Set paths to & create relevant
 * directories required by zebedee, create the service instances it requires etc.
 */
public class ZebedeeConfiguration {
    public static final String PUBLISHED = "master";
    public static final String COLLECTIONS = "collections";
    public static final String PUBLISHED_COLLECTIONS = "publish-log";
    public static final String ZEBEDEE = "zebedee";
    public static final String USERS = "users";
    public static final String SESSIONS = "sessions";
    public static final String PERMISSIONS = "permissions";
    public static final String TEAMS = "teams";
    public static final String LAUNCHPAD = "launchpad";
    public static final String APPLICATION_KEYS = "application-keys";
    public static final String SERVICES = "services";
    public static final String KEYRING = "keyring";

    private boolean useVerificationAgent;
    private Path zebedeePath;
    private PublishedCollections publishedCollections;
    private Collections collections;
    private PublishedContent published;
    private PermissionsService permissionsService;
    private UsersService usersService;
    private TeamsService teamsService;
    private Sessions sessions;
    private DataIndex dataIndex;
    private DatasetService datasetService;
    private ImageService imageService;
    private KafkaService kafkaService;
    private CollectionKeyring collectionKeyring;
    private CollectionKeyCache schedulerKeyCache;
    private EncryptionKeyFactory encryptionKeyFactory;
    private StartUpNotifier startUpNotifier;
    private Notifier slackNotifier;
    private ServiceStore serviceStore;
    private VerificationAgent verificationAgent;

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

        this.useVerificationAgent = enableVerificationAgent;

        // Create the zebedee file system structure
        zebedeePath = createDir(rootPath, ZEBEDEE);

        // Create the services and objects...
        // Initialise the teams service (uses a service supplier to break circular dependency with permissions service)
        initTeamsService(zebedeePath, this::getPermissionsService);

        // Initialise the permissions service
        initPermissionsService(zebedeePath, teamsService);

        // Initialise the users service
        initUsersService(zebedeePath, permissionsService);

        // Initialise the sessions service
        initSessionsService(zebedeePath, getIdentityAPIURL(), getInitialRetryInterval(), getMaxRetryTimeout(), getMaxRetryInterval());

        // Initialise published content services (data index and published content reader)
        initPublishedContentServices(zebedeePath);

        // Initialise collection content services
        initCollectionServices(zebedeePath, permissionsService);

        // Initialise the collection keyring and scheduler cache.
        initCollectionKeyring(zebedeePath, permissionsService, collections, getKeyringSecretKey(), getKeyringInitVector());

        // Initialise the post publish verification service
        initVerificationAgent(publishedCollections, getDefaultVerificationUrl());

        // Initialise Dataset API integration
        initDatasetService(getDatasetAPIURL(), getDatasetAPIAuthToken(), getServiceAuthToken());

        // Initialise Image service
        initImageService(getImageAPIURL(), getServiceAuthToken());

        // Initialise the kafka producer for producing content published events for use by services such as search indexing
        initKafkaService(getKafkaURL(), getKafkaContentPublishedTopic());

        // Initialise service token store
        initServiceTokenStore(zebedeePath);

        // Initialise slack integration
        initSlackIntegration(getSlackToken(), slackChannelsToNotfiyOnStartUp(), getSlackSupportChannelID());

        info().data("root_path", rootPath.toString())
                .data("zebedee_path", zebedeePath.toString())
                .data("enable_verification_agent", useVerificationAgent)
                .log("zebedee configuration creation complete");
    }


    public Path getZebedeePath() {
        return zebedeePath;
    }

    public PublishedContent getPublished() {
        return this.published;
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

    public VerificationAgent getVerificationAgent() {
        return verificationAgent;
    }

    public DatasetService getDatasetService() {
        return datasetService;
    }

    public ImageService getImageService() {
        return imageService;
    }

    public KafkaService getKafkaService() {
        return kafkaService;
    }

    public ServiceStore getServiceStore() {
        return serviceStore;
    }

    public CollectionKeyring getCollectionKeyring() {
        return collectionKeyring;
    }

    public EncryptionKeyFactory getEncryptionKeyFactory() {
        return encryptionKeyFactory;
    }

    public CollectionKeyCache getSchedulerKeyringCache() {
        return schedulerKeyCache;
    }

    public StartUpNotifier getStartUpNotifier() {
        return startUpNotifier;
    }

    public Notifier getSlackNotifier() {
        return slackNotifier;
    }

    /**
     * Initialise the teams service.
     *
     * @param zebedeePath                       the zebedee base directory (i.e. `$zebedee_root/zebedee`)
     * @param permissionsServiceServiceSupplier a {@link ServiceSupplier} to supply the {@link PermissionsService} without
     *                                          creating a circular dependency
     * @throws IOException If a filesystem error occurs.
     *
     * TODO: Remove after migration to JWT sessions is complete
     */
    private void initTeamsService(Path zebedeePath,
                                  ServiceSupplier<PermissionsService> permissionsServiceServiceSupplier)
            throws IOException {

        Path teamsPath = createDir(zebedeePath, TEAMS);
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            this.teamsService = new StubbedTeamsServiceImpl();
            info().log("JWT sessions enabled: stubbed teams service initialised");
        } else {
            this.teamsService = new TeamsServiceImpl(
                    new TeamsStoreFileSystemImpl(teamsPath), permissionsServiceServiceSupplier);

            info().data("teams_path", teamsPath.toString())
                    .log("legacy teams service initialised");
        }
    }

    /**
     * Initialise the permissions service.
     *
     * @param zebedeePath  the zebedee base directory (i.e. `$zebedee_root/zebedee`)
     * @param teamsService the {@link TeamsService}
     * @throws IOException If a filesystem error occurs.
     */
    private void initPermissionsService(Path zebedeePath, TeamsService teamsService) throws IOException {
        Path permissionsPath = createDir(zebedeePath, PERMISSIONS);
        initialiseAccessMapping(permissionsPath);

        PermissionsStore permissionsStore = new PermissionsStoreFileSystemImpl(permissionsPath);
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            this.permissionsService = new JWTPermissionsServiceImpl(permissionsStore);
        } else {
            this.permissionsService = new PermissionsServiceImpl(permissionsStore);
        }

        info().data("permissions_path", permissionsPath.toString())
                .data("jwt_sessions_enabled", cmsFeatureFlags().isJwtSessionsEnabled())
                .log("permissions service initialised");
    }

    /**
     * Initialise the users service.
     *
     * @param zebedeePath        the zebedee base directory (i.e. `$zebedee_root/zebedee`)
     * @param permissionsService the {@link PermissionsService}
     * @throws IOException If a filesystem error occurs.
     *
     * TODO: Remove after migration to JWT sessions is complete
     */
    private void initUsersService(Path zebedeePath, PermissionsService permissionsService) throws IOException {
        Path usersPath = createDir(zebedeePath, USERS);
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            this.usersService = StubbedUsersServiceImpl.getInstance();
            info().log("JWT sessions enabled: stubbed users service initialised");
        } else {
            UserStore usersStore = new UserStoreFileSystemImpl(usersPath);
            this.usersService = UsersServiceImpl.getInstance(usersStore, permissionsService);

            info().data("users_path", usersPath.toString())
                    .log("legacy users service initialised");
        }
    }

    /**
     * Initialise the sessions service.
     *
     * @param zebedeePath       the zebedee base directory (i.e. `$zebedee_root/zebedee`)
     * @param cognitoKeyIdPairs the cognito public signing keys
     * @throws IOException If a filesystem error occurs.
     */
    private void initSessionsService(Path zebedeePath, String identityAPIURL, int initialRetryInterval, int maxRetryTimeout, int maxRetryInterval) throws IOException {
        Path sessionsPath = createDir(zebedeePath, SESSIONS);
        if (cmsFeatureFlags().isJwtSessionsEnabled()) {
            JWTVerifier jwtVerifier = null;
            try {
                jwtVerifier = new JWTVerifierImpl(identityAPIURL, initialRetryInterval, maxRetryTimeout, maxRetryInterval);
            } catch (Exception e) {
                error().logException(e, "failed to initialise JWT validator");
                throw new RuntimeException(e);
            }
            this.sessions = new JWTSessionsServiceImpl(jwtVerifier);
            info().log("JWT sessions service initialised");
        } else {
            LegacySessionsStore legacySessionsStore = new LegacySessionsStoreImpl(sessionsPath);
            this.sessions = new ThreadLocalSessionsServiceImpl(legacySessionsStore, permissionsService, teamsService);
            info().data("sessions_path", sessionsPath.toString())
                    .log("legacy sessions service initialised");
        }
    }

    /**
     * Initialise the central keyring used to store the encryption keys for the collection content.
     *
     * @param zebedeePath        the zebedee base directory (i.e. `$zebedee_root/zebedee`)
     * @param permissionsService the {@link PermissionsService}
     * @param collections        the {@link Collections} service
     * @throws IOException If a filesystem error occurs.
     */
    private void initCollectionKeyring(Path zebedeePath, PermissionsService permissionsService, Collections collections, SecretKey keyringSecretKey, IvParameterSpec keyringInitVector)
            throws IOException {

        Path keyringPath = createDir(zebedeePath, KEYRING);

        CollectionKeyStore keyStore = new CollectionKeyStoreImpl(keyringPath, keyringSecretKey, keyringInitVector);

        CollectionKeyCacheImpl.init(keyStore);
        CollectionKeyCache collectionKeyCache = CollectionKeyCacheImpl.getInstance();

        this.schedulerKeyCache = collectionKeyCache;

        CollectionKeyringImpl.init(collectionKeyCache, permissionsService, collections);
        this.collectionKeyring = CollectionKeyringImpl.getInstance();

        info().data("keyring_path", keyringPath.toString())
                .log("collection keyring initialised");
    }

    /**
     * Initialise content services (i.e. data index and published content reader).
     *
     * @param zebedeePath the zebedee base directory (i.e. `$zebedee_root/zebedee`)
     * @throws IOException If a filesystem error occurs.
     */
    private void initPublishedContentServices(Path zebedeePath) throws IOException {
        Path publishedContentPath = createDir(zebedeePath, PUBLISHED);

        Path redirectPath = publishedContentPath.resolve(Content.REDIRECT);
        if (!Files.exists(redirectPath)) {
            Files.createFile(redirectPath);
        }

        published = new PublishedContent(publishedContentPath);
        published.setRedirects(new RedirectTablePartialMatch(published));

        this.dataIndex = new DataIndex(new FileSystemContentReader(publishedContentPath));

        info().data("published_content_path", publishedContentPath.toString())
                .log("published content services initialised");
    }

    /**
     * Initialise the collection content services.
     *
     * @param zebedeePath        the zebedee base directory (i.e. `$zebedee_root/zebedee`)
     * @param permissionsService the {@link PermissionsService}
     * @throws IOException If a filesystem error occurs.
     */
    private void initCollectionServices(Path zebedeePath, PermissionsService permissionsService)
            throws IOException {

        Path collectionsPath = createDir(zebedeePath, COLLECTIONS);
        Path publishedCollectionsPath = createDir(zebedeePath, PUBLISHED_COLLECTIONS);

        this.publishedCollections = new PublishedCollections(publishedCollectionsPath);
        this.encryptionKeyFactory = new EncryptionKeyFactoryImpl();

        VersionsService versionsService = new VersionsServiceImpl();
        this.collections = new Collections(collectionsPath, permissionsService, versionsService, published);

        info().data("collections_path", collectionsPath.toString())
                .data("published_collections_path", publishedCollectionsPath.toString())
                .log("collection services initialised");
    }

    /**
     * Initialise the dataset service responsible for integration with the dataset API for publishing datasets.
     *
     * @param datasetAPIURL       the base dataset API url
     * @param datasetAPIAuthToken the deprecated service to service shared secret token
     * @param serviceAuthToken    zebedee's service auth token for use in authenticating requests to the dataset API
     */
    private void initDatasetService(String datasetAPIURL, String datasetAPIAuthToken, String serviceAuthToken) {
        DatasetClient datasetClient;
        try {
            datasetClient = new DatasetAPIClient(datasetAPIURL, datasetAPIAuthToken, serviceAuthToken);
        } catch (URISyntaxException e) {
            error().logException(e, "failed to initialise dataset api client - invalid URI");
            throw new IllegalArgumentException(e);
        }

        datasetService = new ZebedeeDatasetService(datasetClient);

        info().data("dataset_api_url", datasetAPIURL)
                .log("dataset api client service initialised");
    }

    /**
     * Initialise the image service which provides integration with the Image API for publishing images.
     *
     * @param imageAPIURL      the image API base url
     * @param serviceAuthToken zebedee's service auth token for use in authenticating requests to the image API
     */
    private void initImageService(String imageAPIURL, String serviceAuthToken) {
        ImageClient imageClient;
        try {
            imageClient = new ImageAPIClient(imageAPIURL, serviceAuthToken);
        } catch (URISyntaxException e) {
            error().logException(e, "failed to initialise image api client - invalid URI");
            throw new IllegalArgumentException(e);
        }

        imageService = new ImageServiceImpl(imageClient);

        info().data("image_api_url", imageAPIURL)
                .log("image api client service initialised");
    }

    /**
     * Initialise the kafka producer service. This service produces content published events for consumption by services
     * such as search indexing
     *
     * @param kafkaURL                   the kafka URL
     * @param kafkaContentPublishedTopic the topic name on which to produce content published events
     */
    private void initKafkaService(String kafkaURL, String kafkaContentPublishedTopic) {
        if (cmsFeatureFlags().isKafkaEnabled()) {
            KafkaClient kafkaClient = new KafkaClientImpl(kafkaURL, kafkaContentPublishedTopic);
            kafkaService = new KafkaServiceImpl(kafkaClient);
        } else {
            kafkaService = new NoOpKafkaService();
        }

        info().data("kafka_url", kafkaURL)
                .data("kafka_content_published_topic", kafkaContentPublishedTopic)
                .log("kafka producer service initialised");
    }

    /**
     * Initialise the service token store.
     *
     * @param zebedeePath the zebedee base directory (i.e. `$zebedee_root/zebedee`)
     * @throws IOException If a filesystem error occurs.
     */
    private void initServiceTokenStore(Path zebedeePath) throws IOException {
        Path servicePath = createDir(zebedeePath, SERVICES);

        this.serviceStore = new ServiceStoreImpl(servicePath);

        info().data("services_path", servicePath.toString())
                .log("service token store initialised");
    }

    /**
     * Initalise the CMS's Slack integration. If the required configuration values are not available the CMS will
     * default to No Op implementation - slack messages will be logged but sent to the Slack API.
     *
     * @param slackToken                     the slack token to use to authenticate requests to slack
     * @param slackChannelsToNotfiyOnStartUp the list of slack channels in which to post startup notifications
     * @param slackSupportChannelID          the support slack channel id to include in messages so that user's know
     *                                       which channel to raise support requests
     */
    private void initSlackIntegration(String slackToken, List<String> slackChannelsToNotfiyOnStartUp, String slackSupportChannelID) {

        boolean validConfig = true;
        if (isEmpty(slackToken)) {
            warn().log("env var slack_api_token is null or empty");
            validConfig = false;
        }

        if (slackChannelsToNotfiyOnStartUp == null || slackChannelsToNotfiyOnStartUp.isEmpty()) {
            warn().log("env var START_UP_NOTIFY_LIST is null or empty");
            validConfig = false;
        }

        if (validConfig) {
            info().log("valid Slack configuation found for CMS/Slack integration");

            SlackClient slackClient = new SlackClientImpl(new Profile.Builder()
                    .emoji(":flo:")
                    .username("Florence")
                    .authToken(slackToken)
                    .create());

            this.slackNotifier = new SlackNotifier(slackClient);

            this.startUpNotifier = new SlackStartUpNotifier(
                    slackClient,
                    slackChannelsToNotfiyOnStartUp,
                    slackSupportChannelID
            );

            info().data("slack_channels_to_notify_on_startup", slackChannelsToNotfiyOnStartUp.toString())
                    .log("slack integration initialised");

        } else {
            warn().log("slack configuration missing/empty defaulting to No op implementation");

            this.slackNotifier = new NopNotifierImpl();
            this.startUpNotifier = new NopStartUpNotifier();
        }
    }

    private void initVerificationAgent(PublishedCollections publishedCollections, String verificationUrl) {
        if (isVerificationEnabled()) {
            this.verificationAgent = new VerificationAgent(publishedCollections, verificationUrl);
        }
    }

    private Path createDir(Path root, String dirName) throws IOException {
        Path dir = root.resolve(dirName);
        if (!Files.exists(dir)) {
            info().data("path", dirName).log("creating required Zebedee directory as it does not exist.");
            Files.createDirectory(dir);
        }
        return dir;
    }
}
