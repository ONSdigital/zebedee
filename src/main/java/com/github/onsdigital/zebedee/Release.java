package com.github.onsdigital.zebedee;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Release {
	static final String APPROVED = "approved";
	static final String IN_PROGRESS = "inprogress";

	ReleaseDescription description;
	Path path;
	Taxonomy approved;
	Taxonomy inProgress;
	Zebedee zebedee;

	/**
	 * Constructs a new release in the given {@link Zebedee}, creating the
	 * necessary folders {@value #APPROVED} and {@value #IN_PROGRESS}.
	 * 
	 * @param name
	 *            The readable name of the release.
	 * @param zebedee
	 * @return
	 * @throws IOException
	 */
	public static Release create(String name, Zebedee zebedee)
			throws IOException {

		String filename = PathUtils.toFilename(name);

		// Create the folders:
		Path release = zebedee.releases.resolve(filename);
		Files.createDirectory(release);
		Files.createDirectory(release.resolve(APPROVED));
		Files.createDirectory(release.resolve(IN_PROGRESS));

		// Create the release description:
		Path releaseDescription = zebedee.releases.resolve(filename + ".json");
		ReleaseDescription description = new ReleaseDescription();
		description.name = name;
		try (OutputStream output = Files.newOutputStream(releaseDescription)) {
			Serialiser.serialise(output, description);
		}

		return new Release(name, zebedee);
	}

	/**
	 * Instantiates an existing release. This validates that the directory
	 * contains folders named {@value #APPROVED} and {@value #IN_PROGRESS} and
	 * throws an exception if not.
	 * 
	 * @param path
	 *            The {@link Path} of the release.
	 * @param zebedee
	 *            The containing {@link Zebedee}.
	 * @throws IOException
	 */
	Release(Path path, Zebedee zebedee) throws IOException {

		// Validate the directory:
		this.path = path;
		Path approved = path.resolve(APPROVED);
		Path inProgress = path.resolve(IN_PROGRESS);
		Path description = path.getParent().resolve(
				path.getFileName() + ".json");
		if (!Files.exists(approved) || !Files.exists(inProgress)
				|| !Files.exists(description)) {
			throw new IllegalArgumentException(
					"This doesn't look like a release folder: "
							+ path.toAbsolutePath());
		}

		// Deserialise the release description:
		try (InputStream input = Files.newInputStream(description)) {
			this.description = Serialiser.deserialise(input,
					ReleaseDescription.class);
		}

		// Set fields:
		this.zebedee = zebedee;
		this.approved = new Taxonomy(approved);
		this.inProgress = new Taxonomy(inProgress);
	}

	Release(String name, Zebedee zebedee) throws IOException {
		this(zebedee.releases.resolve(PathUtils.toFilename(name)), zebedee);
	}

	/**
	 * Finds the given URI in the resolved overlay.
	 * 
	 * @param uri
	 *            The URI to find.
	 * @return The {@link #inProgress} path, otherwise the {@link #approved}
	 *         path, otherwise the existing published path, otherwise null.
	 */
	Path find(String uri) {
		Path result = inProgress.get(uri);
		if (result == null) {
			result = approved.get(uri);
		}
		if (result == null) {
			result = zebedee.published.get(uri);
		}
		return result;
	}

	/**
	 * @param uri
	 *            The URI to check.
	 * @return If the given URI is being edited as part of this release, true.
	 */
	boolean isInRelease(String uri) {
		return isInProgress(uri) || isApproved(uri);
	}

	/**
	 * @param uri
	 *            uri The URI to check.
	 * @return If the given URI is being edited as part of this release and has
	 *         not yet been approved, true.
	 */
	boolean isInProgress(String uri) {
		return inProgress.exists(uri);
	}

	/**
	 * @param uri
	 *            uri The URI to check.
	 * @return If the given URI is being edited as part of this release and has
	 *         been approved, true.
	 */
	boolean isApproved(String uri) {
		return !isInProgress(uri) && approved.exists(uri);
	}

	/**
	 * 
	 * @param uri
	 *            The new path you would like to create.
	 * @return True if the new path was created. If the path is already present
	 *         in the release, another release is editing this path or this path
	 *         already exists in the published content, false.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	boolean create(String uri) throws IOException {
		boolean result = false;

		boolean exists = find(uri) != null;
		boolean isBeingEdited = zebedee.isBeingEdited(uri) > 0;
		if (!isBeingEdited && !exists) {
			// Copy from Published to in progress:
			Path destination = inProgress.toPath(uri);
			Files.createDirectories(destination.getParent());
			Files.createFile(destination);
			result = true;
		}

		return result;
	}

	/**
	 * 
	 * @param sourceUri
	 *            The path you would like to make a copy of.
	 * @param uri
	 *            The new path you would like to create.
	 * @return True if the new path was created. If the path is already present
	 *         in the release, another release is editing this path or this path
	 *         already exists in the published content, false.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	boolean copy(String sourceUri, String targetUri) throws IOException {
		boolean result = false;

		Path source = find(sourceUri);
		boolean isBeingEdited = zebedee.isBeingEdited(targetUri) > 0;
		if (source != null && !isBeingEdited) {
			// Copy from source to in progress:
			Path destination = inProgress.toPath(targetUri);
			PathUtils.copy(source, destination);
			result = true;
		}

		return result;
	}

	/**
	 * 
	 * @param uri
	 *            The path you would like to edit.
	 * @return True if the path was added to {@link #inProgress}. If the path is
	 *         already present in the release, another release is editing this
	 *         path or this path already exists in the published content, false.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	boolean edit(String uri) throws IOException {
		boolean result = false;

		Path source = find(uri);
		boolean isBeingEditedElsewhere = !isInRelease(uri)
				&& zebedee.isBeingEdited(uri) > 0;
		if (source != null && !isInProgress(uri) && !isBeingEditedElsewhere) {
			// Copy to in progress:
			Path destination = inProgress.toPath(uri);
			PathUtils.copy(source, destination);
			result = true;
		}

		return result;
	}

	/**
	 * 
	 * @param uri
	 *            The path you would like to approve.
	 * @return True if the path is found in {@link #inProgress} and was copied
	 *         to {@link #approved}.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	boolean approve(String uri) throws IOException {
		boolean result = false;

		if (isInProgress(uri)) {
			// Copy to in approved and then delete the in-progress copy:
			Path source = inProgress.get(uri);
			Path destination = approved.toPath(uri);
			PathUtils.move(source, destination);
			result = true;
		}

		return result;
	}

	/**
	 * This only enables you to access the content of items that are currently
	 * in progress.
	 * <p>
	 * To open an item for editing, use {@link #create(String)},
	 * {@link #edit(String)} or {@link #copy(String, String)}
	 * 
	 * @param uri
	 *            The URI of the item.
	 * @return The {@link Path} to the item, so that you can call e.g.
	 *         {@link Files#newInputStream(Path, java.nio.file.OpenOption...)}
	 *         or
	 *         {@link Files#newOutputStream(Path, java.nio.file.OpenOption...)}.
	 *         NB if this item is not currently in progress, null will be
	 *         returned.
	 */
	Path getPath(String uri) {
		return inProgress.get(uri);
	}

}
