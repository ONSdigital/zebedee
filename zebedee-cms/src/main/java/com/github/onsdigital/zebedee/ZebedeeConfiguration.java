package com.github.onsdigital.zebedee;

import com.github.onsdigital.session.service.client.Http;
import com.github.onsdigital.session.service.client.SessionClient;
import com.github.onsdigital.session.service.client.SessionClientImpl;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.keyring.Keyring;
import com.github.onsdigital.zebedee.keyring.KeyringImpl;
import com.github.onsdigital.zebedee.keyring.KeyringMigratorImpl;
import com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl;
import com.github.onsdigital.zebedee.keyring.cache.KeyringCache;
import com.github.onsdigital.zebedee.keyring.cache.KeyringCacheImpl;
import com.github.onsdigital.zebedee.keyring.store.KeyringStore;
import com.github.onsdigital.zebedee.keyring.store.KeyringStoreImpl;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.RedirectTablePartialMatch;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
import com.github.onsdigital.zebedee.model.publishing.PublishedCollections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.permissions.service.PermissionsServiceImpl;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.service.*;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.session.service.SessionsAPIServiceImpl;
import com.github.onsdigital.zebedee.session.service.SessionsServiceImpl;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import com.github.onsdigital.zebedee.teams.service.TeamsServiceImpl;
import com.github.onsdigital.zebedee.teams.store.TeamsStoreFileSystemImpl;
import com.github.onsdigital.zebedee.user.service.UsersService;
import com.github.onsdigital.zebedee.user.service.UsersServiceImpl;
import com.github.onsdigital.zebedee.user.store.UserStoreFileSystemImpl;
import com.github.onsdigital.zebedee.util.versioning.VersionsService;
import com.github.onsdigital.zebedee.util.versioning.VersionsServiceImpl;
import com.github.onsdigital.zebedee.verification.VerificationAgent;
import dp.api.dataset.DatasetAPIClient;
import dp.api.dataset.DatasetClient;
import com.github.onsdigital.dp.image.api.client.ImageAPIClient;
import com.github.onsdigital.dp.image.api.client.ImageClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.zebedee.Zebedee.APPLICATION_KEYS;
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
import static com.github.onsdigital.zebedee.configuration.Configuration.getImageAPIURL;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKeyringInitVector;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKeyringSecretKey;
import static com.github.onsdigital.zebedee.configuration.Configuration.getServiceAuthToken;
import static com.github.onsdigital.zebedee.configuration.Configuration.getSessionsApiUrl;
import static com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl.initialisePermissions;

/**
 * Object encapsulating the set up configuration required by {@link Zebedee}. Set paths to & create relevant
 * directories required by zebedee, create the service instances it requires etc.
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
    private Path applicationKeysPath;
    private Path redirectPath;
    private Path servicePath;
    private Path keyRingPath;
    private boolean useVerificationAgent;
    private ApplicationKeys applicationKeys;
    private PublishedCollections publishedCollections;
    private Collections collections;
    private Content published;
    private com.github.onsdigital.zebedee.model.KeyringCache legacyKeyringCache;
    private PermissionsService permissionsService;
    private UsersService usersService;
    private TeamsService teamsService;
    private Sessions sessions;
    private DataIndex dataIndex;
    private PermissionsStore permissionsStore;
    private DatasetService datasetService;
    private ImageService imageService;
    private SessionClient sessionClient;
    private KeyringCache keyringCache;
    private Keyring keyring;


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
        this.applicationKeysPath = createDir(zebedeePath, APPLICATION_KEYS);
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
        this.applicationKeys = new ApplicationKeys(applicationKeysPath);

        // Configure the sessions
        if (cmsFeatureFlags().isSessionAPIEnabled()) {
            sessionClient = new SessionClientImpl(getSessionsApiUrl(), getServiceAuthToken(), new Http());
            this.sessions = new SessionsAPIServiceImpl(sessionClient);
        } else {
            this.sessions = new SessionsServiceImpl(sessionsPath);
        }

        // Initialise legacy keyring regardless - they will dual run until we cut over to new impl.
        this.legacyKeyringCache = new com.github.onsdigital.zebedee.model.KeyringCache(sessions);

        this.teamsService = new TeamsServiceImpl(
                new TeamsStoreFileSystemImpl(teamsPath), this::getPermissionsService);

        this.published = createPublished();

        initialisePermissions(permissionsPath);
        this.permissionsStore = new PermissionsStoreFileSystemImpl(permissionsPath);

        this.permissionsService = new PermissionsServiceImpl(permissionsStore,
                this::getUsersService, this::getTeamsService, legacyKeyringCache);

        // Configure the new KeyringCache if enabled
        if (cmsFeatureFlags().isCentralisedKeyringEnabled()) {
            KeyringStore keyStore = new KeyringStoreImpl(getKeyRingPath(), getKeyringSecretKey(), getKeyringInitVector());

            KeyringCacheImpl.init(keyStore);
            KeyringCache keyringCache = KeyringCacheImpl.getInstance();

            KeyringImpl.init(keyringCache, permissionsService);
            Keyring centralKeyring = KeyringImpl.getInstance();

            Keyring legacyKeyring = new LegacyKeyringImpl(sessions, usersService, legacyKeyringCache, applicationKeys);

            this.keyring = new KeyringMigratorImpl(false, legacyKeyring, centralKeyring);
        }

        VersionsService versionsService = new VersionsServiceImpl();
        this.collections = new Collections(collectionsPath, permissionsService, versionsService, published);

        this.usersService = UsersServiceImpl.getInstance(
                new UserStoreFileSystemImpl(this.usersPath),
                collections,
                permissionsService,
                applicationKeys,
                legacyKeyringCache);

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
            imageClient = new ImageAPIClient(getImageAPIURL(),getServiceAuthToken());
        } catch (URISyntaxException e) {
            error().logException(e, "failed to initialise image api client - invalid URI");
            throw new RuntimeException(e);
        }

        imageService = new ImageServiceImpl(imageClient);

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
                .data("application_keys_path", applicationKeysPath.toString())
                .data("redirect_path", applicationKeysPath.toString())
                .data("services_path", servicePath.toString())
                .data("enable_verification_agent", useVerificationAgent)
                .data("sessions_api_enabled", cmsFeatureFlags().isSessionAPIEnabled())
                .log("zebedee configuration creation complete");

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

    public Path getApplicationKeysPath() {
        return applicationKeysPath;
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
            content.redirect = new RedirectTablePartialMatch(content);
            try {
                Files.createFile(redirectPath);
            } catch (IOException e) {
                error().data("requestedPath", redirectPath.toString())
                        .logException(e, "could not save redirect to requested path");
            }
        } else {
            content.redirect = new RedirectTablePartialMatch(content, redirectPath);
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

    public com.github.onsdigital.zebedee.model.KeyringCache getKeyringCache() {
        return this.legacyKeyringCache;
    }

    public ApplicationKeys getApplicationKeys() {
        return this.applicationKeys;
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

    public ImageService getImageService() {
        return imageService;
    }

    public ServiceStoreImpl getServiceStore() {
        return new ServiceStoreImpl(servicePath);
    }

    public Keyring getCollectionKeyring() {
        return this.keyring;
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
