package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.ContentEvent;
import com.github.onsdigital.zebedee.json.ContentEventType;
import com.github.onsdigital.zebedee.json.ContentEvents;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Collection {
    public static final String REVIEWED = "reviewed";
    public static final String COMPLETE = "complete";
    public static final String IN_PROGRESS = "inprogress";

    public final CollectionDescription description;
    public Path path;
    public Content reviewed;
    public Content complete;
    public Content inProgress;
    Zebedee zebedee;

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
    public Collection(Path path, Zebedee zebedee) throws IOException {

        // Validate the directory:
        this.path = path;
        Path reviewed = path.resolve(REVIEWED);
        Path complete = path.resolve(COMPLETE);
        Path inProgress = path.resolve(IN_PROGRESS);

        Path description = path.getParent().resolve(
                path.getFileName() + ".json");
        if (!Files.exists(reviewed) || !Files.exists(inProgress) || !Files.exists(complete)
                || !Files.exists(description)) {
            throw new IllegalArgumentException(
                    "This doesn't look like a collection folder: "
                            + path.toAbsolutePath());
        }

        // Deserialise the description:
        try (InputStream input = Files.newInputStream(description)) {
            this.description = Serialiser.deserialise(input,
                    CollectionDescription.class);
        }

        // Set fields:
        this.zebedee = zebedee;
        this.reviewed = new Content(reviewed);
        this.complete = new Content(complete);
        this.inProgress = new Content(inProgress);
    }

    Collection(CollectionDescription collectionDescription, Zebedee zebedee) throws IOException {
        this(zebedee.collections.resolve(PathUtils.toFilename(collectionDescription.name)), zebedee);
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
    public static Collection create(CollectionDescription collectionDescription, Zebedee zebedee)
            throws IOException {

        String filename = PathUtils.toFilename(collectionDescription.name);
        collectionDescription.id = filename;

        // Create the folders:
        Path collection = zebedee.collections.resolve(filename);
        Files.createDirectory(collection);
        Files.createDirectory(collection.resolve(REVIEWED));
        Files.createDirectory(collection.resolve(COMPLETE));
        Files.createDirectory(collection.resolve(IN_PROGRESS));

        // Create the description:
        Path collectionDescriptionPath = zebedee.collections.resolve(filename
                + ".json");

        try (OutputStream output = Files.newOutputStream(collectionDescriptionPath)) {
            Serialiser.serialise(output, collectionDescription);
        }

        return new Collection(collectionDescription, zebedee);
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
            throws IOException {

        String filename = PathUtils.toFilename(collectionDescription.name);
        String newFilename = PathUtils.toFilename(newName);

        Path collection = zebedee.collections.resolve(filename);
        Path newCollection = zebedee.collections.resolve(newFilename);

        new File(collection.toUri()).renameTo(new File(newCollection.toUri()));

        // Create the description:
        Path newPath = zebedee.collections.resolve(newFilename
                + ".json");

        CollectionDescription renamedCollectionDescription = new CollectionDescription(
                newName,
                collectionDescription.publishDate);
        renamedCollectionDescription.id = newFilename;

        try (OutputStream output = Files.newOutputStream(newPath)) {
            Serialiser.serialise(output, renamedCollectionDescription);
        }

        Files.delete(zebedee.collections.resolve(filename + ".json"));

        return new Collection(renamedCollectionDescription, zebedee);
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
        this.zebedee.delete(path);

        // Delete the description file
        String filename = PathUtils.toFilename(this.description.name);
        Path collectionDescriptionPath = zebedee.collections.resolve(filename + ".json");
        Files.delete(collectionDescriptionPath);
    }

    public boolean save() throws IOException {
        try (OutputStream output = Files.newOutputStream(this.descriptionPath())) {
            Serialiser.serialise(output, this.description);
            return true;
        }
    }

    private Path descriptionPath() {
        return zebedee.collections.resolve(this.description.id + ".json");
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
        boolean permission = zebedee.permissions.canView(email, uri);

        // Only show edited material if the user has permission:
        if (permission) {
            result = inProgress.get(uri);
            if (result == null) {
                result = complete.get(uri);
            }
            if (result == null) {
                result = reviewed.get(uri);
            }
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

            AddEvent(uri, new ContentEvent(new Date(), ContentEventType.CREATED, email));

            result = true;
        }

        return result;
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
                PathUtils.move(source, destination);
            else {
                PathUtils.copy(source, destination);
            }

            AddEvent(uri, new ContentEvent(new Date(), ContentEventType.EDITED, email));
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
            PathUtils.move(source, destination);

            AddEvent(uri, new ContentEvent(new Date(), ContentEventType.COMPLETED, email));
            result = true;
        }

        return result;
    }

    /**
     * Set the given uri to reviewed in this collection.
     *
     * @param email The reviewing user's email.
     * @param uri   The path you would like to review.
     * @return True if the path is found in {@link #inProgress} and was copied
     * to {@link #reviewed}.
     * @throws IOException If a filesystem error occurs.
     */
    public boolean review(String email, String uri) throws IOException {
        boolean result = false;
        boolean permission = zebedee.permissions.canEdit(email);
        boolean userCompletedContent = didUserCompleteContent(email, uri);

        if (isComplete(uri) && permission && !userCompletedContent) {
            // Move the complete copy to reviewed:
            Path source = complete.get(uri);
            Path destination = reviewed.toPath(uri);
            PathUtils.move(source, destination);

            AddEvent(uri, new ContentEvent(new Date(), ContentEventType.REVIEWED, email));
            result = true;
        }

        return result;
    }

    private boolean didUserCompleteContent(String email, String uri) {

        if (this.description.eventsByUri == null)
            throw new IllegalStateException("This content has not been completed. No events found.");
        ContentEvents contentEvents = this.description.eventsByUri.get(uri);
        if (contentEvents == null)
            throw new IllegalStateException("This content has not been completed. No events found.");

        boolean userCompletedContent = true;
        ContentEvent mostRecentCompletedEvent = this.description.eventsByUri.get(uri).mostRecentEventForType(ContentEventType.COMPLETED);
        if (mostRecentCompletedEvent != null) userCompletedContent = mostRecentCompletedEvent.email == email;
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
     *
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
     * Add a {@link ContentEvent} for the given uri.
     *
     * @param uri   The uri the event belongs to.
     * @param event The event to add.
     */
    void AddEvent(String uri, ContentEvent event) {

        if (this.description.eventsByUri == null)
            this.description.eventsByUri = new HashMap<>();

        if (!this.description.eventsByUri.containsKey(uri))
            this.description.eventsByUri.put(uri, new ContentEvents());

        this.description.eventsByUri.get(uri).add(event);
    }

    /**
     * Deletes this collection from the file system
     *
     * @return True if the file system has been amended
     */
    public boolean deleteContent(String uri) throws IOException {
        // Find the relevant collection for the uri and delete
        if(isInProgress(uri)) {
            return inProgress.delete(uri);
        } else if (isComplete(uri)) {
            return complete.delete(uri);
        } else if (isReviewed(uri)) {
            return inProgress.delete(uri);
        }
        return false;
    }
}

