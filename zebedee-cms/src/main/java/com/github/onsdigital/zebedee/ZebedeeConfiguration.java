package com.github.onsdigital.zebedee;

import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.model.Permissions;
import com.github.onsdigital.zebedee.model.RedirectTablePartialMatch;
import com.github.onsdigital.zebedee.model.Sessions;
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

    private static Path verifyDir(Path root, String dirName) {
        Path dir = root.resolve(dirName);
        if (!Files.exists(dir)) {
            throw new IllegalArgumentException("This folder doesn't look like a zebedee folder: " + dir.toAbsolutePath());
        }
        return dir;
    }

    public void setZebedeeRootPath(Path zebedeeRootPath) {
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
        return new DataIndex(new FileSystemContentReader(publishedContentPath));
    }

    public Collections getCollections(Permissions permissions, Content published) {
        return new Collections(collectionsPath, permissions, published);
    }

    public PublishedCollections getPublishCollections() {
        return new PublishedCollections(publishedCollectionsPath);
    }

    public KeyringCache getKeyringCache(Zebedee z) {
        return new KeyringCache(z);
    }

    public ApplicationKeys getApplicationKeys() {
        return new ApplicationKeys(applicationKeysPath);
    }

    public Sessions getSessions() {
        return new Sessions(sessionsPath);
    }

    public Permissions getPermissions(Zebedee z) {
        return new Permissions(permissionsPath, z);
    }

    public Teams getTeams(Permissions permissions) {
        return new Teams(teamsPath, permissions);
    }

    public UsersService getUsersService(Collections collections, Permissions permissions, ApplicationKeys
            applicationKeys, KeyringCache keyringCache) {
        return UsersServiceImpl.getInstance(usersPath, collections, permissions, applicationKeys, keyringCache);
    }

    public VerificationAgent getVerificationAgent(boolean verificationIsEnabled, Zebedee z) {
        return isUseVerificationAgent() && verificationIsEnabled ? new VerificationAgent(z) : null;
    }
}
