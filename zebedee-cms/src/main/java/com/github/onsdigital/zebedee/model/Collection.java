package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Keys;
import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.KeyManangerUtil;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.page.release.Release;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.DeleteContentRequestDeniedException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.Events;
import com.github.onsdigital.zebedee.model.approval.tasks.ReleasePopulator;
import com.github.onsdigital.zebedee.model.content.item.ContentItemVersion;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.model.publishing.scheduled.Scheduler;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_CONTENT_REVIEWED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_CREATED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_NAME_CHANGED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_PUBLISH_RESCHEDULED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_TYPE_CHANGED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.DATA_VISUALISATION_COLLECTION_CONTENT_DELETED;
import static com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory.getCollectionHistoryDao;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.collectionCreated;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.contentReviewed;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.renamed;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.reschedule;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.typeChanged;

public class Collection {

    public static final String REVIEWED = "reviewed";
    public static final String COMPLETE = "complete";
    public static final String IN_PROGRESS = "inprogress";

    private static ConcurrentMap<Path, ReadWriteLock> collectionLocks = new ConcurrentHashMap<>();
    private static KeyManangerUtil keyManagerUtil = new KeyManangerUtil();

    public final CollectionDescription description;
    public final Path path;
    public final Content reviewed;
    public final Content complete;
    public final Content inProgress;

    public final Zebedee zebedee;

    private final Path collectionJsonPath;

    private static ServiceSupplier<CollectionHistoryDao> collectionHistoryDaoServiceSupplier = () -> getCollectionHistoryDao();

    @VisibleForTesting
    public static void setKeyManagerUtil(KeyManangerUtil manager) {
        keyManagerUtil = manager;
    }

    @VisibleForTesting
    public static void setCollectionHistoryDaoServiceSupplier(ServiceSupplier<CollectionHistoryDao> supplier) {
        collectionHistoryDaoServiceSupplier = supplier;
    }

    /**
     * Instantiates an existing {@link Collection}. This validates that the
     * directory contains folders named {@value #REVIEWED},
     * {@value #IN_PROGRESS}, and {@value #COMPLETE}
     * and throws an exception if not.
     *
     * @param path    The {@link Path} of the {@link Collection}.
     * @param zebedee The containing {@link Zebedee}.
     * @throws IOException
     */
    public Collection(Path path, Zebedee zebedee) throws IOException, CollectionNotFoundException {

        this.zebedee = zebedee;

        // Validate the directory:
        this.path = path;
        Path reviewed = path.resolve(REVIEWED);
        Path complete = path.resolve(COMPLETE);
        Path inProgress = path.resolve(IN_PROGRESS);

        this.collectionJsonPath = path.getParent().resolve(
                path.getFileName() + ".json");
        if (!Files.exists(reviewed) || !Files.exists(inProgress) || !Files.exists(complete)
                || !Files.exists(this.collectionJsonPath)) {
            throw new CollectionNotFoundException(
                    "This doesn't look like a collection folder: "
                            + path.toAbsolutePath());
        }

        // Deserialise the description:
        collectionLocks.putIfAbsent(this.path, new ReentrantReadWriteLock());
        collectionLocks.get(this.path).readLock().lock();
        try (InputStream input = Files.newInputStream(this.collectionJsonPath)) {
            this.description = Serialiser.deserialise(input,
                    CollectionDescription.class);
        } finally {
            collectionLocks.get(this.path).readLock().unlock();
        }

        // Set fields:
        this.reviewed = new Content(reviewed);
        this.complete = new Content(complete);
        this.inProgress = new Content(inProgress);
    }

    /**
     * Deconstructs a {@link Collection} in the given {@link Zebedee},
     * creating the necessary folders {@value #REVIEWED} and
     * {@value #IN_PROGRESS}.
     *
     * @param collectionDescription The {@link CollectionDescription} for the {@link Collection}.
     * @param zebedee
     * @return
     * @throws IOException
     */
    public static Collection create(CollectionDescription collectionDescription, Zebedee zebedee, Session session)
            throws IOException, ZebedeeException {

        collectionDescription.isEncrypted = true; // force encryption on new collections.
        collectionDescription.approvalStatus = ApprovalStatus.NOT_STARTED;

        Release release = checkForRelease(collectionDescription, zebedee);

        String filename = PathUtils.toFilename(collectionDescription.getName());
        collectionDescription.setId(filename + "-" + Random.id());

        // Create the folders:
        Path rootCollectionsPath = zebedee.getCollections().path;

        CreateCollectionFolders(filename, rootCollectionsPath);

        collectionDescription.addEvent(new Event(new Date(), EventType.CREATED, session.getEmail()));
        // Create the description:
        Path collectionDescriptionPath = rootCollectionsPath.resolve(filename
                + ".json");
        try (OutputStream output = Files.newOutputStream(collectionDescriptionPath)) {
            Serialiser.serialise(output, collectionDescription);
        }

        Collection collection = new Collection(rootCollectionsPath.resolve(filename), zebedee);
        collectionHistoryDaoServiceSupplier.getService().saveCollectionHistoryEvent(collection, session, COLLECTION_CREATED,
                collectionCreated(collectionDescription));

        if (collectionDescription.getTeams() != null) {
            for (String teamName : collectionDescription.getTeams()) {
                Team team = zebedee.getTeamsService().findTeam(teamName);
                zebedee.getPermissionsService().addViewerTeam(collectionDescription, team, session);
            }
        }

        // Encryption
        // assign a key for the collection to the session user
        keyManagerUtil.assignKeyToUser(zebedee, zebedee.getUsersService().getUserByEmail(session.getEmail()),
                collection.getDescription().getId(), Keys.newSecretKey());

        // get the session user to distribute the key to all
        keyManagerUtil.distributeCollectionKey(zebedee, session, collection, true);

        if (release != null) {
            collection.associateWithRelease(session.getEmail(), release, new ZebedeeCollectionWriter(zebedee,
                    collection, session));
            collection.save();
        }

        return collection;
    }

    public static void CreateCollectionFolders(String filename, Path rootCollectionsPath) throws IOException {
        Path collectionPath = rootCollectionsPath.resolve(filename);
        Files.createDirectory(collectionPath);
        Files.createDirectory(collectionPath.resolve(REVIEWED));
        Files.createDirectory(collectionPath.resolve(COMPLETE));
        Files.createDirectory(collectionPath.resolve(IN_PROGRESS));
    }

    private static Release checkForRelease(CollectionDescription collectionDescription, Zebedee zebedee) throws IOException, ZebedeeException {
        Release release = null;
        if (StringUtils.isNotEmpty(collectionDescription.getReleaseUri())) {
            release = getPublishedRelease(collectionDescription.getReleaseUri(), zebedee);

            if (zebedee.isBeingEdited(release.getUri().toString() + "/data.json") > 0) {
                Optional<Collection> otherCollection = zebedee.checkForCollectionBlockingChange(release.getUri().toString() + "/data.json");
                if(otherCollection.isPresent()) {
                    throw new ConflictException(
                            "Cannot use this release. It is being edited as part of another collection.", otherCollection.get().getDescription().getName());
                }
                throw new ConflictException(
                        "Cannot use this release. It is being edited as part of another collection.");
            }

            // TODO does this check need to be here.
            try {
                zebedee.checkAllCollectionsForDeleteMarker(release.getUri().toString());
            } catch (DeleteContentRequestDeniedException ex) {
                Optional<Collection> otherCollection = zebedee.checkForCollectionBlockingChange(release.getUri().toString() + "/data.json");
                if(otherCollection.isPresent()) {
                    throw new ConflictException(
                            "Cannot use this release. It is being deleted as part of another collection.", otherCollection.get().getDescription().getName());
                }
                throw new ConflictException(
                        "Cannot use this release. It is being deleted as part of another collection.");
            }

            if (release.getDescription().getReleaseDate() == null) {
                throw new BadRequestException("Could not use this release, the release has no release date.");
            }

            collectionDescription.setPublishDate(release.getDescription().getReleaseDate());
        }
        return release;
    }

    /**
     * Renames an existing {@link Collection} in the given {@link Zebedee}.
     *
     * @param collectionDescription The {@link CollectionDescription} for the {@link Collection} to rename.
     * @param newName               The new name to apply to the {@link Collection}.
     * @param zebedee
     * @return
     * @throws IOException
     */
    public static Collection rename(CollectionDescription collectionDescription, String newName, Zebedee zebedee)
            throws IOException, CollectionNotFoundException {

        String filename = PathUtils.toFilename(collectionDescription.getName());
        String newFilename = PathUtils.toFilename(newName);

        Path collection = zebedee.getCollections().path.resolve(filename);
        Path newCollection = zebedee.getCollections().path.resolve(newFilename);

        new File(collection.toUri()).renameTo(new File(newCollection.toUri()));

        // Create the description:
        Path newPath = zebedee.getCollections().path.resolve(newFilename
                + ".json");

        collectionDescription.setName(newName);

        try (OutputStream output = Files.newOutputStream(newPath)) {
            Serialiser.serialise(output, collectionDescription);
        }

        Files.delete(zebedee.getCollections().path.resolve(filename + ".json"));

        return new Collection(zebedee.getCollections().path.resolve(newFilename), zebedee);
    }

    private static Release getPublishedRelease(String uri, Zebedee zebedee) throws IOException, ZebedeeException {
        Release release = (Release) new ZebedeeReader(zebedee.getPublished().path.toString(), null).getPublishedContent(uri);
        return release;
    }

    /**
     * Helper method to update the given collection with only the specified properties in the given CollectionDescription.
     *
     * @param collection
     * @param collectionDescription
     * @param zebedee
     * @param scheduler
     * @return
     */
    public static Collection update(
            Collection collection,
            CollectionDescription collectionDescription,
            Zebedee zebedee,
            Scheduler scheduler,
            Session session
    ) throws IOException, ZebedeeException {

        if (collection == null) {
            throw new BadRequestException("Please specify a collection");
        }
        Collection updatedCollection = collection;

        // only update the collection name if its given and its changed.
        if (collectionDescription.getName() != null
                && !collectionDescription.getName().equals(collection.getDescription().getName())) {
            String nameBeforeUpdate = collection.getDescription().getName();

            // check if only the casing of the name has changed. If so only the json is updated, not the filename.
            if (!collectionDescription.getName().equalsIgnoreCase(collection.getDescription().getName())) {
                updatedCollection = collection.rename(collection.getDescription(), collectionDescription.getName(), zebedee);
            } else {
                updatedCollection.getDescription().setName(collectionDescription.getName());
            }

            collectionHistoryDaoServiceSupplier.getService().saveCollectionHistoryEvent(collection, session, COLLECTION_NAME_CHANGED, renamed
                    (nameBeforeUpdate));
        }

        // if the type has changed
        if (collectionDescription.getType() != null
                && updatedCollection.getDescription().getType() != collectionDescription.getType()) {
            updatedCollection.getDescription().setType(collectionDescription.getType());
            collectionHistoryDaoServiceSupplier.getService().saveCollectionHistoryEvent(collection, session, COLLECTION_TYPE_CHANGED, typeChanged
                    (updatedCollection.description));
        }

        if (updatedCollection.getDescription().getType() == CollectionType.scheduled) {
            if (collectionDescription.getPublishDate() != null) {
                if (!collectionDescription.getPublishDate().equals(collection.getDescription().getPublishDate())) {
                    collectionHistoryDaoServiceSupplier.getService().saveCollectionHistoryEvent(collection, session, COLLECTION_PUBLISH_RESCHEDULED,
                            reschedule(collection.getDescription().getPublishDate(), collectionDescription.getPublishDate()));
                }
                updatedCollection.getDescription().setPublishDate(collectionDescription.getPublishDate());
                scheduler.schedulePublish(updatedCollection, zebedee);
            }
        } else { // the type is now manual so cancel it
            updatedCollection.getDescription().setPublishDate(null);
            scheduler.cancel(collection);
        }

        Set<String> updatesTeams = updateViewerTeams(collectionDescription, zebedee, session);

        if (updatedCollection.getDescription().getTeams() != null) {
            updatedCollection.getDescription().getTeams().clear();
        } else {
            updatedCollection.getDescription().setTeams(new ArrayList<>());
        }
        updatedCollection.getDescription().getTeams().addAll(updatesTeams);

        updatedCollection.save();

        KeyManager.distributeCollectionKey(zebedee, session, collection, false);

        return updatedCollection;
    }

    private static Set<String> updateViewerTeams(CollectionDescription collectionDescription, Zebedee zebedee, Session session) throws IOException, ZebedeeException {

        Set<String> updatedTeams = new HashSet<>();

        if (collectionDescription.getTeams() != null) {
            // work out which teams need to be removed from the existing teams.
            Set<Integer> currentTeamIds = zebedee.getPermissionsService().listViewerTeams(collectionDescription, session);
            TeamsService teamsService = zebedee.getTeamsService();
            for (Integer currentTeamId : currentTeamIds) { // for each current team ID
                for (Team team : teamsService.listTeams()) { // iterate the teamsService list to find the team object
                    if (currentTeamId.equals(team.getId())) { // if the ID's match
                        if (!collectionDescription.getTeams().contains(team.getName())) { // if the team is not
                            // listed in the updated list
                            zebedee.getPermissionsService().removeViewerTeam(collectionDescription, team, session);
                        }
                    }
                }
            }

            // Add all the new teams. The add is idempotent so we don't need to check if it already exists.
            for (String teamName : collectionDescription.getTeams()) {
                // We have already deserialised the teams list to its more efficient to iterate it again rather than
                // deserialise by team name.
                for (Team team : teamsService.listTeams()) {
                    if (teamName.equals(team.getName())) {
                        zebedee.getPermissionsService().addViewerTeam(collectionDescription, team, session);
                        updatedTeams.add(teamName);
                    }
                }
            }
        }

        return updatedTeams;
    }

    public CollectionDescription getDescription() {
        return this.description;
    }

    private Release getReleaseFromCollection(String uri) throws IOException, ZebedeeException {
        Path collectionReleasePath = this.find(uri);
        Release release = (Release) ContentUtil.deserialiseContent(FileUtils.openInputStream(collectionReleasePath.toFile()));
        return release;
    }

    public Release populateRelease(CollectionReader reader, CollectionWriter collectionWriter, List<ContentDetail> collectionContent) throws IOException, ZebedeeException {

        if (StringUtils.isEmpty(this.getDescription().getReleaseUri())) {
            throw new BadRequestException("This collection is not associated with a release.");
        }

        String uri = this.getDescription().getReleaseUri() + "/data.json";
        try (
                Resource resource = reader.getResource(uri);
                InputStream dataStream = resource.getData()
        ) {
            Release release = (Release) ContentUtil.deserialiseContent(dataStream);
            logInfo("Release identified for collection")
                    .collectionName(this.getDescription().getName())
                    .addParameter("title", release.getDescription().getTitle())
                    .log();

            if (release == null) {
                throw new BadRequestException("This collection is not associated with a release.");
            }

            release = ReleasePopulator.populate(release, collectionContent);
            collectionWriter.getReviewed().writeObject(release, uri);

            return release;
        }
    }

    /**
     * Deconstructs a {@link Collection} in the given {@link Zebedee} and deletes it's description
     *
     * @return
     * @throws IOException
     */
    public void delete()
            throws IOException {

        // Delete folders:
        //this.zebedee.delete(path); // delete the directory only if its empty
        FileUtils.deleteDirectory(path.toFile()); // delete the directory including any files.

        // Delete the description file
        Path collectionDescriptionPath = this.collectionJsonPath;

        // delete
        if (Files.exists(collectionDescriptionPath)) {
            Files.delete(collectionDescriptionPath);
        }

        // remove the lock for the collection
        collectionLocks.remove(path);
    }

    /**
     * This methods is used by {@link com.github.onsdigital.zebedee.model.publishing.Publisher Publisher}
     * to acquire a write lock on a collection during publishing.
     *
     * @return The collection write lock.
     */
    public Lock getWriteLock() {
        return collectionLocks.get(this.path).writeLock();
    }

    public boolean save() throws IOException {
        collectionLocks.get(this.path).writeLock().lock();
        try (OutputStream output = Files.newOutputStream(this.descriptionPath())) {
            Serialiser.serialise(output, this.description);
            return true;
        } finally {
            collectionLocks.get(this.path).writeLock().unlock();
        }
    }

    private Path descriptionPath() {
        return this.collectionJsonPath;
    }

    /**
     * Finds the given URI in the resolved overlay.
     *
     * @param uri The URI to find.
     * @return The {@link #inProgress} path, otherwise the {@link #reviewed}
     * path, otherwise the existing published path, otherwise null.
     */
    public Path find(String uri) throws IOException {
        Path result = inProgress.get(uri);

        if (result == null) {
            result = complete.get(uri);
        }

        if (result == null) {
            result = reviewed.get(uri);
        }

        if (result == null) {
            result = zebedee.getPublished().get(uri);
        }

        return result;

    }

    /**
     * @param uri The URI to check.
     * @return If the given URI is being edited as part of this
     * {@link Collection}, true.
     */
    public boolean isInCollection(String uri) {
        return isInProgress(uri) || isComplete(uri) || isReviewed(uri);
    }

    /**
     * @param uri uri The URI to check.
     * @return If the given URI is being edited as part of this
     * {@link Collection} and has not yet been reviewed, true.
     */
    boolean isInProgress(String uri) {
        return inProgress.exists(uri);
    }

    /**
     * @param uri uri The URI to check.
     * @return If the given URI is being edited as part of this
     * {@link Collection} and has not yet been reviewed, true.
     */
    boolean isComplete(String uri) {
        return !isInProgress(uri) && complete.exists(uri);
    }

    /**
     * @param uri uri The URI to check.
     * @return If the given URI is being edited as part of this
     * {@link Collection} and has been reviewed, true.
     */
    boolean isReviewed(String uri) {
        return !isInProgress(uri) && !isComplete(uri) && reviewed.exists(uri);
    }

    /**
     * @param uri The new path you would like to create.
     * @return True if the new path was created. If the path is already present
     * in the {@link Collection}, another {@link Collection} is editing
     * this path or this path already exists in the published content,
     * false.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean create(String email, String uri) throws IOException {
        boolean result = false;

        // Does this path already exist in the published area?
        boolean exists = find(uri) != null;

        // Is someone creating the same file in another collection?
        boolean isBeingEdited = zebedee.isBeingEdited(uri) > 0;

        boolean hasDeleteMarker = false;
        try {
            zebedee.checkAllCollectionsForDeleteMarker(uri);
        } catch (DeleteContentRequestDeniedException ex) {
            hasDeleteMarker = true;
        }

        // Does the current user have permission to edit?
        boolean permission = zebedee.getPermissionsService().canEdit(email);

        if (!isBeingEdited && !hasDeleteMarker && !exists && permission) {
            // Copy from Published to in progress:
            Path path = inProgress.toPath(uri);
            PathUtils.create(path);

            addEvent(uri, new Event(new Date(), EventType.CREATED, email));

            result = true;
        }

        return result;
    }

    /**
     * @param uri       The path you would like to edit.
     * @param recursive
     * @return True if the path was added to {@link #inProgress}. If the path is
     * already present in the {@link Collection}, another
     * {@link Collection} is editing this path or this path already
     * exists in the published content, false.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean edit(String email, String uri, CollectionWriter collectionWriter, Boolean recursive) throws IOException, BadRequestException {
        boolean result = false;

        try {
            zebedee.checkAllCollectionsForDeleteMarker(uri);
        } catch (DeleteContentRequestDeniedException ex) {
            return false;
        }

        if (isInProgress(uri)) {
            return true;
        }

        Path source = find(uri);

        Optional<Collection> blockingCollection = zebedee.checkForCollectionBlockingChange(this, uri);
        if (blockingCollection.isPresent()) {
            Collection collection = blockingCollection.get();

            logInfo("Content was not saved as it currently in another collection.")
                    .saveOrEditConflict(this, collection, uri)
                    .user(email)
                    .log();

            // return false as the content is blocked by another collection.
            return result;
        }


        // Does the user have permission to edit?
        boolean permission = zebedee.getPermissionsService().canEdit(email, description);
        if (!permission) {
            logInfo("Content was not saved as user does not have EDIT permission")
                    .path(uri)
                    .collectionName(this)
                    .user(email)
                    .log();
        }

        if (source != null && permission) {
            // Copy to in progress:
            if (this.isInCollection(uri)) {
                Path destination = inProgress.toPath(uri);

                if (recursive) {
                    FileUtils.deleteDirectory(destination.getParent().toFile());
                    FileUtils.moveDirectory(source.getParent().toFile(), destination.getParent().toFile());
                } else {
                    PathUtils.moveFilesInDirectory(source, destination);
                    zebedee.getCollections().removeEmptyCollectionDirectories(source);
                }
            } else {
                try (InputStream inputStream = new FileInputStream(source.toFile())) {
                    collectionWriter.getInProgress().write(inputStream, uri);
                }
            }

            addEvent(uri, new Event(new Date(), EventType.EDITED, email));
            result = true;
        }

        return result;
    }

    public boolean edit(String email, String uri, CollectionWriter collectionWriter) throws IOException, BadRequestException {
        return edit(email, uri, collectionWriter, false);
    }

    /**
     * @param email     The reviewing user's email.
     * @param uri       The path you would like to review.
     * @param recursive
     * @return True if the path is found in {@link #inProgress} and was copied
     * to {@link #reviewed}.
     * @throws IOException If a filesystem error occurs.
     */

    public boolean complete(String email, String uri, boolean recursive) throws IOException {
        boolean result = false;
        boolean permission = zebedee.getPermissionsService().canEdit(email);

        if (isInProgress(uri) && permission) {
            // Move the in-progress copy to completed:
            Path source = inProgress.get(uri);
            Path destination = complete.toPath(uri);

            if (recursive) {
                FileUtils.deleteDirectory(destination.getParent().toFile());
                FileUtils.moveDirectory(source.getParent().toFile(), destination.getParent().toFile());
            } else {
                PathUtils.moveFilesInDirectory(source, destination);
            }

            addEvent(uri, new Event(new Date(), EventType.COMPLETED, email));
            result = true;
        }

        return result;
    }

    /**
     * Set the given uri to reviewed in this collection.
     *
     * @param session   The user session attempting to review
     * @param uri       The path you would like to review.
     * @param recursive
     * @return True if the path is found in {@link #inProgress} and was copied
     * to {@link #reviewed}.
     * @throws UnauthorizedException if user
     * @throws IOException           If a filesystem error occurs.
     */
    public boolean review(Session session, String uri, boolean recursive) throws IOException, ZebedeeException {
        if (session == null) {
            throw new UnauthorizedException("Insufficient permissions");
        }


        boolean result = false;

        if (!this.isInCollection(uri)) {
            throw new NotFoundException("File not found");
        }


        boolean permission = zebedee.getPermissionsService().canEdit(session.getEmail());
        if (!permission) {
            throw new UnauthorizedException("Insufficient permissions");
        }

        if (Files.isDirectory(this.find(uri))) {
            throw new BadRequestException("Cannot complete a directory");
        }

        boolean contentWasCompleted = contentWasCompleted(uri);
        if (contentWasCompleted == false) {
            throw new BadRequestException("Item has not been marked completed");
        }

        boolean userCompletedContent = didUserCompleteContent(session.getEmail(), uri);
        if (userCompletedContent) {
            throw new UnauthorizedException("Reviewer must be a second set of eyes");
        }

        if (reviewed.get(uri) != null) {
            throw new BadRequestException("Item has already been reviewed");
        }

        if (permission && !userCompletedContent) {

            // Move the complete copy to reviewed:
            Path source = complete.get(uri);

            if (source == null) {
                source = inProgress.get(uri);
            }

            Path destination = reviewed.toPath(uri);

            if (recursive) {
                FileUtils.deleteDirectory(destination.getParent().toFile());
                FileUtils.moveDirectory(source.getParent().toFile(), destination.getParent().toFile());
            } else {
                PathUtils.moveFilesInDirectory(source, destination);
                zebedee.getCollections().removeEmptyCollectionDirectories(source);
            }

            addEvent(uri, new Event(new Date(), EventType.REVIEWED, session.getEmail()));
            collectionHistoryDaoServiceSupplier.getService().saveCollectionHistoryEvent(
                    new CollectionHistoryEvent(this.description.getId(), this.description.getName(), session,
                            COLLECTION_CONTENT_REVIEWED, contentReviewed(source, destination)));
            result = true;
        }

        return result;
    }

    private boolean contentWasCompleted(String uri) {

        if (!StringUtils.startsWith(uri, "/")) {
            uri = "/" + uri;
        }
        if (this.description.eventsByUri == null) {
            return false;
        }

        Events events = this.description.eventsByUri.get(uri);
        if (events == null) {
            return false;
        }

        return events.hasEventForType(EventType.COMPLETED);
    }

    private boolean didUserCompleteContent(String email, String uri) throws BadRequestException {

        if (!StringUtils.startsWith(uri, "/")) {
            uri = "/" + uri;
        }
        if (this.description.eventsByUri == null) {
            return false;
        }

        Events events = this.description.eventsByUri.get(uri);
        if (events == null) {
            return false;
        }

        boolean userCompletedContent = false;
        Event mostRecentCompletedEvent = events.mostRecentEventForType(EventType.COMPLETED);
        if (mostRecentCompletedEvent != null) userCompletedContent = mostRecentCompletedEvent.email.equals(email);
        return userCompletedContent;
    }

    /**
     * This enables you to access the content of items that are currently
     * in progress. This is used to update content when editing.
     *
     * @param uri The URI of the item.
     * @return The {@link Path} to the item, so that you can call e.g.
     * {@link Files#newInputStream(Path, java.nio.file.OpenOption...)}
     * or
     * {@link Files#newOutputStream(Path, java.nio.file.OpenOption...)}.
     * NB if this item is not currently in progress, null will be
     * returned.
     */
    public Path getInProgressPath(String uri) {
        return inProgress.toPath(uri);
    }

    public List<String> inProgressUris() throws IOException {
        return inProgress.uris();
    }

    public List<String> completeUris() throws IOException {
        return complete.uris();
    }

    public List<String> reviewedUris() throws IOException {
        return reviewed.uris();
    }

    public boolean isEmpty() {
        return uriCount() == 0;
    }

    /**
     * @return the total uri's in all edit folders
     */
    public int uriCount() {
        try {
            return inProgress.uris().size() + completeUris().size() + reviewedUris().size();
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Add a {@link Event} for the given uri.
     *
     * @param uri   The uri the event belongs to.
     * @param event The event to add.
     */
    public void addEvent(String uri, Event event) {

        if (!StringUtils.startsWith(uri, "/")) {
            uri = "/" + uri;
        }

        if (this.description.eventsByUri == null)
            this.description.eventsByUri = new HashMap<>();

        if (!this.description.eventsByUri.containsKey(uri))
            this.description.eventsByUri.put(uri, new Events());

        this.description.eventsByUri.get(uri).add(event);
    }

    /**
     * Deletes a single file.
     *
     * @return True if the file system has been amended
     */
    public boolean deleteFile(String uri) throws IOException {
        if (isInProgress(uri)) {
            return inProgress.delete(uri);
        } else if (isComplete(uri)) {
            return complete.delete(uri);
        } else if (isReviewed(uri)) {
            return reviewed.delete(uri);
        }
        return false;
    }

    /**
     * Delete all the content files in the directory of the given file.
     *
     * @param uri
     * @return
     * @throws IOException
     */
    public boolean deleteContentDirectory(String email, String uri) throws IOException {

        boolean hasDeleted = false;

        if (inProgress.exists(uri)) {
            deleteContent(inProgress, uri);
            hasDeleted = true;
        }
        if (complete.exists(uri)) {
            deleteContent(complete, uri);
            hasDeleted = true;
        }
        if (reviewed.exists(uri)) {
            deleteContent(reviewed, uri);
            hasDeleted = true;
        }

        if (hasDeleted) addEvent(uri, new Event(new Date(), EventType.DELETED, email));
        save();

        return hasDeleted;
    }

    public boolean deleteDataVisContent(Session session, Path contentPath) throws IOException {
        if (contentPath == null || StringUtils.isEmpty(contentPath.toString())) {
            return false;
        }

        String contentUri = contentPath.toString();
        boolean hasDeleted = false;

        for (Content collectionDir : new Content[]{inProgress, complete, reviewed}) {
            if (collectionDir.exists(contentUri)) {
                FileUtils.deleteDirectory(Paths.get(collectionDir.getPath().toString() + contentUri).toFile());
                hasDeleted = true;
            }
        }

        if (hasDeleted) {
            addEvent(contentUri, new Event(new Date(), EventType.DELETED, session.getEmail()));
            collectionHistoryDaoServiceSupplier.getService().saveCollectionHistoryEvent(new CollectionHistoryEvent(this, session,
                    DATA_VISUALISATION_COLLECTION_CONTENT_DELETED, contentUri));
        }
        save();
        return hasDeleted;
    }

    /**
     * When we delete content, we don't want to just delete the whole directory it lives in as it may have nested content.
     * Instead only the files in the directory are deleted, as we know these are all associated with that content.
     * We also delete the versions directory as all the contents will also be associated with the content being deleted.
     *
     * @param content
     * @param uri
     * @throws IOException
     * @throws NotFoundException
     */
    private void deleteContent(Content content, String uri) throws IOException {
        Path path = content.toPath(uri);
        PathUtils.deleteFilesInDirectory(path);

        File versionsDirectory = path.resolve(VersionedContentItem.getVersionDirectoryName()).toFile();
        FileUtils.deleteDirectory(versionsDirectory);
    }

    /**
     * Associate this collection with the given release
     *
     * @param email
     * @param release
     * @return
     * @throws NotFoundException
     * @throws IOException
     */
    public Release associateWithRelease(String email, Release release, CollectionWriter collectionWriter) throws IOException, BadRequestException {

        String uri = release.getUri().toString() + "/data.json";

        // add the release page to the collection in progress
        if (!isInCollection(uri)) {
            this.edit(email, uri, collectionWriter, false);
        }

        release.getDescription().setPublished(true);
        collectionWriter.getInProgress().writeObject(release, uri);
        return release;
    }

    /**
     * Return trie if this collection is associated with a release.
     *
     * @return
     */
    public boolean isRelease() {
        return StringUtils.isNotEmpty(this.description.getReleaseUri());
    }

    /**
     * Create a new version for the given URI.
     *
     * @param email - The email of the user requesting the version.
     * @param uri   - The URI of the file to version
     * @return
     */
    public ContentItemVersion version(String email, String uri, CollectionWriter collectionWriter) throws ZebedeeException, IOException {

        // first ensure the content exists in published area so we can create a version from it.
        Path versionSource = zebedee.getPublished().get(uri);
        if (versionSource == null) {
            throw new NotFoundException(String.format("The given URI %s was not found - it has not been published.", uri));
        }

        ContentReader contentReader = new FileSystemContentReader(zebedee.getPublished().path);

        VersionedContentItem versionedContentItem = new VersionedContentItem(uri);

        if (versionedContentItem.versionExists(this.reviewed)) {
            throw new ConflictException("A previous version of this file already exists");
        }

        ContentItemVersion version = versionedContentItem.createVersion(zebedee.getPublished().path, contentReader, collectionWriter.getReviewed());
        addEvent(uri, new Event(new Date(), EventType.VERSIONED, email, version.getIdentifier()));
        return version;
    }

    /**
     * Delete the version at the given URI.
     *
     * @param uri - The URI of the version to delete.
     * @throws NotFoundException   - if the given URI was not found in the collection.
     * @throws BadRequestException - if the given URI is not a valid version URI.
     */
    public void deleteVersion(String uri) throws NotFoundException, BadRequestException, IOException {

        if (!VersionedContentItem.isVersionedUri(uri)) {
            throw new BadRequestException("The given URI is not recognised as a version");
        }

        Path reviewedPath = this.reviewed.toPath(uri);
        if (!Files.exists(reviewedPath)) {
            throw new NotFoundException("This version does not exist in this collection");
        }

        FileUtils.deleteDirectory(reviewedPath.toFile());
    }

    /**
     * Move the given Uri to the given newUri.
     *
     * @param session - The session of the user moving the content.
     * @param fromUri - The current URI of the content to be moved.
     * @param toUri   - The URI to move the content to.
     */
    public boolean moveContent(Session session, String fromUri, String toUri) throws IOException, ZebedeeException {

        boolean hasMoved = false;

        if (inProgress.exists(fromUri)) {
            moveContent(inProgress, fromUri, toUri);
            hasMoved = true;
        }
        if (complete.exists(fromUri)) {
            moveContent(complete, fromUri, toUri);
            hasMoved = true;
        }
        if (reviewed.exists(fromUri)) {
            moveContent(reviewed, fromUri, toUri);
            hasMoved = true;
        }

        // Fix up links within the content
        if (hasMoved) replaceLinksWithinCollection(session, fromUri, toUri);

        if (hasMoved) addEvent(fromUri, new Event(new Date(), EventType.MOVED, session.getEmail()));

        return hasMoved;
    }

    private void moveContent(Content content, String uri, String newUri) throws IOException {
        File fromFile = content.toPath(uri).toFile();
        File toFile = content.toPath(newUri).toFile();
        toFile.delete(); // delete an existing file if it is to be overwritten.
        if (fromFile.isFile()) {
            FileUtils.moveFile(fromFile, toFile);
        } else {
            FileUtils.moveDirectory(fromFile, toFile);
        }
    }

    /**
     * Replace all uri references within a collection.
     *
     * @param session
     * @param oldUri
     * @param newUri
     * @throws IOException
     * @throws NotFoundException
     * @throws BadRequestException
     * @throws UnauthorizedException
     */
    private void replaceLinksWithinCollection(Session session, String oldUri, String newUri) throws IOException, ZebedeeException {

        CollectionReader collectionReader = new ZebedeeCollectionReader(this.zebedee, this, session);
        CollectionWriter collectionWriter = new ZebedeeCollectionWriter(this.zebedee, this, session);

        for (String uri : inProgressUris()) {
            replaceLinksInFile(uri, oldUri, newUri, collectionReader.getInProgress(), collectionWriter.getInProgress());
        }
        for (String uri : completeUris()) {
            replaceLinksInFile(uri, oldUri, newUri, collectionReader.getComplete(), collectionWriter.getComplete());
        }
        for (String uri : reviewedUris()) {
            replaceLinksInFile(uri, oldUri, newUri, collectionReader.getReviewed(), collectionWriter.getReviewed());
        }
    }

    /**
     * Replace uri references within a file
     *
     * @param uri           the uri to update the links in
     * @param oldUri        the uri we are moving from
     * @param newUri        the uri we are moving to
     * @param contentReader
     * @throws IOException if we encounter file problems
     * @ param contentWriter
     */
    private void replaceLinksInFile(String uri, String oldUri, String newUri, ContentReader contentReader, ContentWriter contentWriter) throws IOException, ZebedeeException {
        try {
            if (!uri.toLowerCase().endsWith(".json")) {
                return;
            }

            String content;
            try (
                    Resource resource = contentReader.getResource(uri);
                    InputStream inputStream = resource.getData();
                    Scanner scanner = new Scanner(inputStream)
            ) {
                content = scanner.useDelimiter("//Z").next();
                content = content.replaceAll("\"" + oldUri + "\"", "\"" + newUri + "\"");
                content = content.replaceAll(oldUri + "/", newUri + "/");

                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content.getBytes())) {
                    contentWriter.write(byteArrayInputStream, uri);
                }
            }

        } catch (NoSuchElementException e) {
            // do nothing if its not found.
        }
    }

    public boolean renameContent(String email, String fromUri, String toUri) throws IOException {
        boolean hasRenamed = false;

        if (inProgress.exists(fromUri)) {
            hasRenamed = renameContent(inProgress, fromUri, toUri);
        }
        if (complete.exists(fromUri)) {
            hasRenamed = renameContent(complete, fromUri, toUri);
        }
        if (reviewed.exists(fromUri)) {
            hasRenamed = renameContent(reviewed, fromUri, toUri);
        }

        if (hasRenamed) addEvent(fromUri, new Event(new Date(), EventType.RENAMED, email));

        return hasRenamed;
    }

    private boolean renameContent(Content content, String fromUri, String toUri) throws IOException {
        File fromFile = content.toPath(fromUri).toFile();
        File toFile = content.toPath(toUri).toFile();

        if (!fromFile.getParent().equals(toFile.getParent())) {
            return false;
        }

        if (fromFile.isDirectory()) {
            return false;
        }

        toFile.delete(); // delete an existing file if it is to be overwritten.
        FileUtils.moveFile(fromFile, toFile);
        return true;
    }

    public Content getInProgress() {
        return inProgress;
    }
}

