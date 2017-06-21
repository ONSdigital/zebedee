package com.github.onsdigital.zebedee;

import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.model.RedirectTablePartialMatch;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import com.github.onsdigital.zebedee.teams.service.TeamsServiceImpl;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
import com.github.onsdigital.zebedee.model.publishing.PublishedCollections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.permissions.service.PermissionsServiceImpl;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import com.github.onsdigital.zebedee.teams.store.TeamsStoreFileSystemImpl;
import com.github.onsdigital.zebedee.user.service.UsersService;
import com.github.onsdigital.zebedee.user.service.UsersServiceImpl;
import com.github.onsdigital.zebedee.user.store.UserStoreFileSystemImpl;
import com.github.onsdigital.zebedee.verification.VerificationAgent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.Zebedee.APPLICATION_KEYS;
import static com.github.onsdigital.zebedee.Zebedee.COLLECTIONS;
import static com.github.onsdigital.zebedee.Zebedee.PERMISSIONS;
import static com.github.onsdigital.zebedee.Zebedee.PUBLISHED;
import static com.github.onsdigital.zebedee.Zebedee.PUBLISHED_COLLECTIONS;
import static com.github.onsdigital.zebedee.Zebedee.SESSIONS;
import static com.github.onsdigital.zebedee.Zebedee.TEAMS;
import static com.github.onsdigital.zebedee.Zebedee.USERS;
import static com.github.onsdigital.zebedee.Zebedee.ZEBEDEE;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl.initialisePermissions;

/**
 * Object encapsulating the set up configuration required by {@link Zebedee}. Set paths to & create relevant
 * directories required by zebedee, create the service instances it requires etc.
 */
public class ZebedeeConfiguration {

    private static final String LOG_PREFIX = "[ZebedeeConfiguration]: ";

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
    private boolean useVerificationAgent;
    private VerificationAgent verificationAgent;
    private ApplicationKeys applicationKeys;
    private PublishedCollections publishedCollections;
    private Collections collections;
    private Content published;
    private KeyringCache keyringCache;
    private PermissionsService permissionsService;
    private UsersService usersService;
    private TeamsService teamsService;
    private SessionsService sessionsService;
    private DataIndex dataIndex;
    private PermissionsStore permissionsStore;

    private static Path createDir(Path root, String dirName) throws IOException {
        Path dir = root.resolve(dirName);
        if (!Files.exists(dir)) {
            logDebug(LOG_PREFIX + "Creating required Zebedee directory as it does not exist.")
                    .path(dirName).log();
            Files.createDirectory(dir);
        } else {
            logDebug(LOG_PREFIX + "Zebedee directory already exists no action required.")
                    .path(dir.toString()).log();
        }
        return dir;
    }

    /**
     * Create a new configuration object.
     *
     * @param rootPath
     * @param enableVerificationAgent
     * @throws IOException
     */
    public ZebedeeConfiguration(Path rootPath, boolean enableVerificationAgent) throws IOException {
        logDebug(LOG_PREFIX + "Creating ZebedeeConfiguration").log();

        if (Files.exists(rootPath)) {
            logDebug(LOG_PREFIX + "Setting Zebedee root directory").path(rootPath.toString()).log();
        } else {
            throw new IllegalArgumentException(LOG_PREFIX + "Zebedee root directory doesn't not exist." + rootPath.toAbsolutePath());
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

        if (!Files.exists(redirectPath)) {
            Files.createFile(redirectPath);
        }

        this.useVerificationAgent = enableVerificationAgent;

        // Create the services and objects...
        this.dataIndex = new DataIndex(new FileSystemContentReader(publishedContentPath));
        this.publishedCollections = new PublishedCollections(publishedCollectionsPath);
        this.applicationKeys = new ApplicationKeys(applicationKeysPath);
        this.sessionsService = new SessionsService(sessionsPath);
        this.keyringCache = new KeyringCache(sessionsService);

        this.teamsService = new TeamsServiceImpl(
                new TeamsStoreFileSystemImpl(teamsPath), this::getPermissionsService);

        this.published = createPublished();

        initialisePermissions(permissionsPath);
        this.permissionsStore = new PermissionsStoreFileSystemImpl(permissionsPath);

        this.permissionsService = new PermissionsServiceImpl(permissionsStore,
                this::getUsersService, this::getTeamsService, keyringCache);

        this.collections = new Collections(collectionsPath, permissionsService, published);

        this.usersService = UsersServiceImpl.getInstance(
                new UserStoreFileSystemImpl(this.usersPath),
                collections,
                permissionsService,
                applicationKeys,
                keyringCache)
        ;

        logDebug(LOG_PREFIX + "ZebedeeConfiguration creation complete.").log();
    }


    public ZebedeeConfiguration enableVerificationAgent(boolean enabled) {
        this.useVerificationAgent = enabled;
        return this;
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
                logError(e, LOG_PREFIX + "Could not save redirect to requested path")
                        .addParameter("requestedPath", redirectPath.toString())
                        .log();
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

    public KeyringCache getKeyringCache() {
        return this.keyringCache;
    }

    public ApplicationKeys getApplicationKeys() {
        return this.applicationKeys;
    }

    public SessionsService getSessionsService() {
        return this.sessionsService;
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

    public PermissionsStore getPermissionsStore(Path accessMappingPath) {
        return new PermissionsStoreFileSystemImpl(accessMappingPath);
    }

    public void setUsersService(UsersService usersService) {
        this.usersService = usersService;
    }
}
