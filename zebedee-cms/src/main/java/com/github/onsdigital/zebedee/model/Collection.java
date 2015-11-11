package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.page.release.Release;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.*;
import com.github.onsdigital.zebedee.json.*;
import com.github.onsdigital.zebedee.model.content.item.ContentItemVersion;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.util.Log;
import com.github.onsdigital.zebedee.util.ReleasePopulator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Collection {
    public static final String REVIEWED = "reviewed";
    public static final String COMPLETE = "complete";
    public static final String IN_PROGRESS = "inprogress";

    private static ConcurrentMap<Path, ReadWriteLock> collectionLocks = new ConcurrentHashMap<>();
    public final CollectionDescription description;
    public final Path path;
    public final Content reviewed;
    public final Content complete;
    public final Content inProgress;
    final Zebedee zebedee;
    final Collections collections;

    //public RedirectTableChained redirect = null;
    //private RedirectTableChained collectionRedirect = null;

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

        // Validate the directory:
        this.path = path;
        Path reviewed = path.resolve(REVIEWED);
        Path complete = path.resolve(COMPLETE);
        Path inProgress = path.resolve(IN_PROGRESS);

        Path description = path.getParent().resolve(
                path.getFileName() + ".json");
        if (!Files.exists(reviewed) || !Files.exists(inProgress) || !Files.exists(complete)
                || !Files.exists(description)) {
            throw new CollectionNotFoundException(
                    "This doesn't look like a collection folder: "
                            + path.toAbsolutePath());
        }

        // Deserialise the description:
        collectionLocks.putIfAbsent(this.path, new ReentrantReadWriteLock());
        collectionLocks.get(this.path).readLock().lock();
        try (InputStream input = Files.newInputStream(description)) {
            this.description = Serialiser.deserialise(input,
                    CollectionDescription.class);
        } finally {
            collectionLocks.get(this.path).readLock().unlock();
        }

        // Set fields:
        this.zebedee = zebedee;
        this.collections = zebedee.collections;
        this.reviewed = new Content(reviewed);
        this.complete = new Content(complete);
        this.inProgress = new Content(inProgress);

//        this.inProgress.redirect.setChild(this.complete.redirect);
//        this.complete.redirect.setChild(this.reviewed.redirect);
//        this.reviewed.redirect.setChild(this.zebedee.published.redirect);

        // Set up redirect
        // this compound redirect will retrieve
//        redirect = this.inProgress.redirect;
    }

    Collection(CollectionDescription collectionDescription, Zebedee zebedee) throws IOException, CollectionNotFoundException {
        this(zebedee.collections.path.resolve(PathUtils.toFilename(collectionDescription.name)), zebedee);
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
    public static Collection create(CollectionDescription collectionDescription, Zebedee zebedee, String email)
            throws IOException, ZebedeeException {

        Release release = checkForRelease(collectionDescription, zebedee);

        String filename = PathUtils.toFilename(collectionDescription.name);
        collectionDescription.id = filename + "-" + Random.id();

        // Create the folders:
        Path collectionPath = zebedee.collections.path.resolve(filename);
        Files.createDirectory(collectionPath);
        Files.createDirectory(collectionPath.resolve(REVIEWED));
        Files.createDirectory(collectionPath.resolve(COMPLETE));
        Files.createDirectory(collectionPath.resolve(IN_PROGRESS));

        collectionDescription.AddEvent(new Event(new Date(), EventType.CREATED, email));

        // Create the description:
        Path collectionDescriptionPath = zebedee.collections.path.resolve(filename
                + ".json");
        try (OutputStream output = Files.newOutputStream(collectionDescriptionPath)) {
            Serialiser.serialise(output, collectionDescription);
        }

        Collection collection = new Collection(collectionDescription, zebedee);

        if (release != null) {
            collection.associateWithRelease(email, release);
            collection.save();
        }

        return collection;
    }

    private static Release checkForRelease(CollectionDescription collectionDescription, Zebedee zebedee) throws IOException, ZebedeeException {
        Release release = null;
        if (StringUtils.isNotEmpty(collectionDescription.releaseUri)) {
            release = getPublishedRelease(collectionDescription.releaseUri, zebedee);

            if (zebedee.isBeingEdited(release.getUri().toString() + "/data.json") > 0) {
                throw new ConflictException(
                        "Cannot use this release. It is being edited as part of another collection.");
            }

            if (release.getDescription().getReleaseDate() == null) {
                throw new BadRequestException("Could not use this release, the release has no release date.");
            }

            collectionDescription.publishDate = release.getDescription().getReleaseDate();
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

        String filename = PathUtils.toFilename(collectionDescription.name);
        String newFilename = PathUtils.toFilename(newName);

        Path collection = zebedee.collections.path.resolve(filename);
        Path newCollection = zebedee.collections.path.resolve(newFilename);

        new File(collection.toUri()).renameTo(new File(newCollection.toUri()));

        // Create the description:
        Path newPath = zebedee.collections.path.resolve(newFilename
                + ".json");

        CollectionDescription renamedCollectionDescription = new CollectionDescription(
                newName,
                collectionDescription.publishDate);
        renamedCollectionDescription.id = collectionDescription.id;
        renamedCollectionDescription.type = collectionDescription.type;

        try (OutputStream output = Files.newOutputStream(newPath)) {
            Serialiser.serialise(output, renamedCollectionDescription);
        }

        Files.delete(zebedee.collections.path.resolve(filename + ".json"));

        return new Collection(renamedCollectionDescription, zebedee);
    }

    private static Release getPublishedRelease(String uri, Zebedee zebedee) throws IOException, ZebedeeException {
        Release release = (Release) new ZebedeeReader(zebedee.published.path.toString(), null).getPublishedContent(uri);
        return release;
    }

    /**
     * Helper method to update the given collection with only the specified properties in the given CollectionDescription.
     *
     * @param collection
     * @param collectionDescription
     * @param zebedee
     * @return
     */
    public static Collection update(Collection collection, CollectionDescription collectionDescription, Zebedee zebedee) throws IOException, CollectionNotFoundException {

        Collection updatedCollection = collection;

        // only update the collection name if its given and its changed.
        if (collectionDescription.name != null && !collectionDescription.name.equals(collection.description.name)) {
            updatedCollection = collection.rename(collection.description, collectionDescription.name, zebedee);
        }

        if (collectionDescription.type != null && updatedCollection.description.type != collectionDescription.type) {
            updatedCollection.description.type = collectionDescription.type;
            if (collectionDescription.type == CollectionType.scheduled) {
                if (collectionDescription.publishDate != null) {
                    updatedCollection.description.publishDate = collectionDescription.publishDate;
                }
            }

            updatedCollection.save();
        }

        return updatedCollection;
    }

    private Release getReleaseFromCollection(String email, String uri) throws IOException, ZebedeeException {
        Path collectionReleasePath = this.find(email, uri);
        Release release = (Release) ContentUtil.deserialiseContent(FileUtils.openInputStream(collectionReleasePath.toFile()));
        return release;
    }

    public Release populateRelease(String email) throws IOException, ZebedeeException {

        if (StringUtils.isEmpty(this.description.releaseUri)) {
            throw new BadRequestException("This collection is not associated with a release.");
        }

        String uri = this.description.releaseUri + "/data.json";
        Release release = getReleaseFromCollection(email, uri);
        Log.print("Release identified for collection %s: %s", this.description.name, release.getDescription().getTitle());

        if (release == null) {
            throw new BadRequestException("This collection is not associated with a release.");
        }

        release = ReleasePopulator.populate(release, this);

        Path releasePath = reviewed.get(uri);
        FileUtils.write(releasePath.toFile(), ContentUtil.serialise(release));

        return release;
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
        String filename = PathUtils.toFilename(this.description.name);
        Path collectionDescriptionPath = collections.path.resolve(filename + ".json");

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
        String filename = PathUtils.toFilename(this.description.name);
        return collections.path.resolve(filename + ".json");
    }

    /**
     * Finds the given URI in the resolved overlay.
     *
     * @param uri The URI to find.
     * @return The {@link #inProgress} path, otherwise the {@link #reviewed}
     * path, otherwise the existing published path, otherwise null.
     */
    public Path find(String email, String uri) throws IOException {
        Path result = null;

        // Does the user have permission tno see this content?
        boolean permission = zebedee.permissions.canView(email, description);

        // Only show edited material if the user has permission:
        if (permission) {
            //String redirected = redirect.get(uri);
            //if (redirected == null) { redirected = uri; }

            result = inProgress.get(uri);

            if (result == null) {
                result = complete.get(uri);
            }

            if (result == null) {
                result = reviewed.get(uri);
            }

            if (result == null) {
                result = zebedee.published.get(uri);
            }

            return result;
        }

        // Default is the published version:
        if (result == null) {
            result = zebedee.published.get(uri);
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
        boolean exists = find(email, uri) != null;

        // Is someone creating the same file in another collection?
        boolean isBeingEdited = zebedee.isBeingEdited(uri) > 0;

        // Does the current user have permission to edit?
        boolean permission = zebedee.permissions.canEdit(email);

        if (!isBeingEdited && !exists && permission) {
            // Copy from Published to in progress:
            Path path = inProgress.toPath(uri);
            PathUtils.create(path);

            addEvent(uri, new Event(new Date(), EventType.CREATED, email));

            result = true;
        }

        return result;
    }

    public Path autocreateReviewedPath(String uri) throws IOException {
        // Does this path already exist in the published area?
        Path path = reviewed.get(uri);
        if (path == null) {
            path = reviewed.toPath(uri);
            PathUtils.create(path);
        }
        return path;
    }

    /**
     * @param uri The path you would like to edit.
     * @return True if the path was added to {@link #inProgress}. If the path is
     * already present in the {@link Collection}, another
     * {@link Collection} is editing this path or this path already
     * exists in the published content, false.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean edit(String email, String uri) throws IOException {
        boolean result = false;

        if (isInProgress(uri))
            return true;

        Path source = find(email, uri);

        // Is the path being edited anywhere but here?
        boolean isBeingEditedElsewhere = !isInCollection(uri)
                && zebedee.isBeingEdited(uri) > 0;

        // Does the user have permission to edit?
        boolean permission = zebedee.permissions.canEdit(email);

        if (source != null && !isBeingEditedElsewhere && permission) {
            // Copy to in progress:
            Path destination = inProgress.toPath(uri);

            if (this.isInCollection(uri))
                PathUtils.moveFilesInDirectory(source, destination);
            else {
                // Optimise zebedee to only upload new files to a collection
                PathUtils.copy(source, destination);
            }

            addEvent(uri, new Event(new Date(), EventType.EDITED, email));
            result = true;
        }

        return result;
    }

    /**
     * @param email The reviewing user's email.
     * @param uri   The path you would like to review.
     * @return True if the path is found in {@link #inProgress} and was copied
     * to {@link #reviewed}.
     * @throws IOException If a filesystem error occurs.
     */

    public boolean complete(String email, String uri) throws IOException {
        boolean result = false;
        boolean permission = zebedee.permissions.canEdit(email);

        if (isInProgress(uri) && permission) {
            // Move the in-progress copy to completed:
            Path source = inProgress.get(uri);
            Path destination = complete.toPath(uri);
            PathUtils.moveFilesInDirectory(source, destination);

            addEvent(uri, new Event(new Date(), EventType.COMPLETED, email));
            result = true;
        }

        return result;
    }

    /**
     * Set the given uri to reviewed in this collection.
     *
     * @param session The user session attempting to review
     * @param uri     The path you would like to review.
     * @return True if the path is found in {@link #inProgress} and was copied
     * to {@link #reviewed}.
     * @throws UnauthorizedException if user
     * @throws IOException           If a filesystem error occurs.
     */
    public boolean review(Session session, String uri) throws IOException, BadRequestException, UnauthorizedException, NotFoundException {
        if (session == null) {
            throw new UnauthorizedException("Insufficient permissions");
        }


        boolean result = false;

        if (!this.isInCollection(uri)) {
            throw new NotFoundException("File not found");
        }


        boolean permission = zebedee.permissions.canEdit(session.email);
        if (!permission) {
            throw new UnauthorizedException("Insufficient permissions");
        }

        if (Files.isDirectory(this.find(session.email, uri))) {
            throw new BadRequestException("Cannot complete a directory");
        }

        boolean contentWasCompleted = contentWasCompleted(uri);
        if (contentWasCompleted == false) {
            throw new BadRequestException("Item has not been marked completed");
        }

        boolean userCompletedContent = didUserCompleteContent(session.email, uri);
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

            PathUtils.moveFilesInDirectory(source, destination);

            addEvent(uri, new Event(new Date(), EventType.REVIEWED, session.email));
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
    public boolean deleteContent(String email, String uri) throws IOException {

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
        try {
            new VersionedContentItem(URI.create(uri), path).deleteVersionDirectory();
        } catch (NotFoundException e) {
            // do nothing, there is nothing to delete.
        }
    }

    /**
     * Associate this collection with the given release
     * @param email
     * @param release
     * @return
     * @throws NotFoundException
     * @throws IOException
     */
    public Release associateWithRelease(String email, Release release) throws IOException {

        String uri = release.getUri().toString() + "/data.json";

        // add the release page to the collection in progress
        if (!isInCollection(uri)) {
            this.edit(email, uri);
        }

        Path releasePath = find(email, uri);
        release.getDescription().setPublished(true);

        // write file
        FileUtils.write(releasePath.toFile(), ContentUtil.serialise(release));

        return release;
    }

    /**
     * Return trie if this collection is associated with a release.
     *
     * @return
     */
    public boolean isRelease() {
        return StringUtils.isNotEmpty(this.description.releaseUri);
    }

    /**
     * Create a new version for the given URI.
     *
     * @param email - The email of the user requesting the version.
     * @param uri   - The URI of the file to version
     * @return
     */
    public ContentItemVersion version(String email, String uri) throws NotFoundException, IOException, ConflictException {

        // first ensure the content exists in published area so we can create a version from it.
        Path versionSource = zebedee.published.get(uri);
        if (versionSource == null) {
            throw new NotFoundException(String.format("The given URI %s was not found - it has not been published.", uri));
        }

        // determine path in reviewed section for the version to sit
        Path reviewedPath = this.reviewed.toPath(uri);
        if (!this.reviewed.exists(reviewedPath.toUri())) {
            Files.createDirectories(reviewedPath);
        }

        VersionedContentItem versionedContentItem = new VersionedContentItem(URI.create(uri), reviewedPath);

        if (versionedContentItem.versionExists()) {
            throw new ConflictException("A previous version of this file already exists");
        }

        ContentItemVersion version = versionedContentItem.createVersion(versionSource);
        addEvent(uri, new Event(new Date(), EventType.VERSIONED, email, version.getIdentifier()));
        return version;
    }

    /**
     * Create a new version for the given timeseries URI.
     *
     * The same as the regular timeseries function but does not record update in the event log
     *
     * @param uri   - The URI of the file to version
     * @return
     */
    public ContentItemVersion versionTimeSeries(String uri) throws NotFoundException, IOException, ConflictException {

        // first ensure the content exists in published area so we can create a version from it.
        Path versionSource = zebedee.published.get(uri);
        if (versionSource == null) {
            throw new NotFoundException(String.format("The given URI %s was not found - it has not been published.", uri));
        }

        // determine path in reviewed section for the version to sit
        Path reviewedPath = this.reviewed.toPath(uri);
        if (!this.reviewed.exists(reviewedPath.toUri())) {
            Files.createDirectories(reviewedPath);
        }

        VersionedContentItem versionedContentItem = new VersionedContentItem(URI.create(uri), reviewedPath);

        if (versionedContentItem.versionExists()) {
            throw new ConflictException("A previous version of this file already exists");
        }

        ContentItemVersion version = versionedContentItem.createVersion(versionSource);
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
     * @param email   - The email of the user moving the content.
     * @param fromUri - The current URI of the content to be moved.
     * @param toUri   - The URI to move the content to.
     */
    public boolean moveContent(String email, String fromUri, String toUri) throws IOException {

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

        if (hasMoved) addEvent(fromUri, new Event(new Date(), EventType.MOVED, email));
        save();

        return hasMoved;
    }

    private void moveContent(Content content, String uri, String newUri) throws IOException {
        File fromFile = content.toPath(uri).toFile();
        File toFile = content.toPath(newUri).toFile();
        toFile.delete(); // delete an existing file if it is to be overwritten.
        FileUtils.moveFile(fromFile, toFile);
    }
}

