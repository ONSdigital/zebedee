package com.github.onsdigital.zebedee;

import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.permissions.service.PermissionsServiceImpl;
import com.github.onsdigital.zebedee.model.RedirectTablePartialMatch;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStoreFileSystemImpl;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import com.github.onsdigital.zebedee.model.Teams;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
import com.github.onsdigital.zebedee.model.publishing.PublishedCollections;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.service.UsersService;
import com.github.onsdigital.zebedee.service.UsersServiceImpl;
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
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Created by dave on 19/05/2017.
 */
public class ZebedeeConfiguration {

    private Path zebedeeRootPath;
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
    private Path path;
    private PermissionsServiceImpl permissionsServiceImpl;

    private UsersService usersService;
    private Teams teams;
    private SessionsService sessionsService;
    private DataIndex dataIndex;
    private PermissionsStore permissionsStore;

    private static Path verifyDir(Path root, String dirName) {
        Path dir = root.resolve(dirName);
        if (!Files.exists(dir)) {
            throw new IllegalArgumentException("This folder doesn't look like a zebedee folder: " + dir.toAbsolutePath());
        }
        return dir;
    }

    public ZebedeeConfiguration(Path zebedeeRootPath, boolean enableVerificationAgent) {
        this.zebedeeRootPath = zebedeeRootPath;
        this.publishedContentPath = verifyDir(zebedeeRootPath, PUBLISHED);
        this.collectionsPath = verifyDir(zebedeeRootPath, COLLECTIONS);
        this.publishedCollectionsPath = verifyDir(zebedeeRootPath, PUBLISHED_COLLECTIONS);
        this.usersPath = verifyDir(zebedeeRootPath, USERS);
        this.sessionsPath = verifyDir(zebedeeRootPath, SESSIONS);
        this.permissionsPath = verifyDir(zebedeeRootPath, PERMISSIONS);
        this.teamsPath = verifyDir(zebedeeRootPath, TEAMS);
        this.applicationKeysPath = verifyDir(zebedeeRootPath, APPLICATION_KEYS);
        this.redirectPath = this.publishedContentPath.resolve(Content.REDIRECT);
        this.useVerificationAgent = enableVerificationAgent;

        this.dataIndex = new DataIndex(new FileSystemContentReader(publishedContentPath));
        this.publishedCollections = new PublishedCollections(publishedCollectionsPath);
        this.keyringCache = new KeyringCache(sessionsService);
        this.applicationKeys = new ApplicationKeys(applicationKeysPath);
        this.sessionsService = new SessionsService(sessionsPath);
        this.teams = new Teams(teamsPath, () -> getPermissionsServiceImpl());

        this.permissionsStore = new PermissionsStoreFileSystemImpl(permissionsPath);
        this.permissionsServiceImpl = new PermissionsServiceImpl(permissionsStore, () -> this.getUsersService(),
                () -> this.getTeams(), keyringCache);

        this.collections = new Collections(collectionsPath, permissionsServiceImpl, published);
        this.usersService = UsersServiceImpl.getInstance(usersPath, collections, permissionsServiceImpl,
                applicationKeys, keyringCache);

    }


    public void enableVerificationAgent(boolean enabled) {
        this.useVerificationAgent = enabled;
    }

    public boolean isUseVerificationAgent() {
        return useVerificationAgent;
    }

    public Path getZebedeeRootPath() {
        return zebedeeRootPath;
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
        Content content = new Content(publishedContentPath);
        Path redirectPath = publishedContentPath.resolve(Content.REDIRECT);
        if (!Files.exists(redirectPath)) {
            content.redirect = new RedirectTablePartialMatch(content);
            try {
                Files.createFile(redirectPath);
            } catch (IOException e) {
                logError(e, "Could not save redirect to requested path")
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

    public PermissionsServiceImpl getPermissionsServiceImpl() {
        return this.permissionsServiceImpl;
    }

    public Teams getTeams() {
        return this.teams;
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
}
