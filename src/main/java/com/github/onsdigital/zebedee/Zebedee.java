package com.github.onsdigital.zebedee;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Zebedee {
	static final String ZEBEDEE = "zebedee";
	static final String PUBLISHED = "published";
	static final String COLLECTIONS = "collections";

	public final Path path;
	public final Content published;
	public final Path collections;

    Zebedee(Path path) {

        // Validate the directory:
        this.path = path;
        Path published = path.resolve(PUBLISHED);
        Path collections = path.resolve(COLLECTIONS);
        if (!Files.exists(published) || !Files.exists(collections)) {
            throw new IllegalArgumentException(
                    "This folder doesn't look like a collection folder: "
                            + path.toAbsolutePath());
        }
        this.published = new Content(published);
        this.collections = collections;
    }

	/**
	 * Creates a new Zebedee folder in the specified parent Path.
     *
     * @param parent
	 *            The directory in which the folder will be created.
	 * @return A {@link Zebedee} instance representing the newly created folder.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	public static Zebedee create(Path parent) throws IOException {
		Path path = Files.createDirectory(parent.resolve(ZEBEDEE));
		Files.createDirectory(path.resolve(PUBLISHED));
		Files.createDirectory(path.resolve(COLLECTIONS));
		return new Zebedee(path);
	}

	/**
	 * This method works out how many {@link Collection}s contain the given URI.
	 * The intention is to allow double-checking in case of concurrent editing.
	 * This should be 0 in order for someone to be allowed to edit a URI and
	 * should be 1 after editing is initiated. If this returns more than 1 after
	 * initiating editing then the current attempt to edit should be reverted -
	 * presumably a race condition.
	 * 
	 * @param uri
	 *            The URI to check.
	 * @return The number of {@link Collection}s containing the given URI.
	 * @throws IOException
	 */
	public int isBeingEdited(String uri) throws IOException {
		int result = 0;

		// Is this URI present anywhere else?
		for (Collection collection : getCollections()) {
			if (collection.isInCollection(uri)) {
				result++;
			}
		}

		return result;
	}

	/**
	 * 
	 * @return A list of all {@link Collection}s.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
    public Collections getCollections() throws IOException {
        Collections result = new Collections();
        try (DirectoryStream<Path> stream = Files
				.newDirectoryStream(collections)) {
			for (Path path : stream) {
				if (Files.isDirectory(path)) {
					result.add(new Collection(path, this));
				}
			}
		}
		return result;
	}

	public Path find(String uri) throws IOException {

		// There's currently only one place to look for content.
		// We may add one or more staging layers later.
		return published.get(uri);
	}

	public boolean publish(Collection collection) throws IOException {

		// Check everything has been approved:
		if (collection.inProgress.uris().size() > 0) {
			return false;
		}

		// Move each item of content:
		for (String uri : collection.approved.uris()) {
			Path source = collection.approved.get(uri);
			Path destination = published.toPath(uri);
			PathUtils.move(source, destination);
		}

		// Delete the folders:
		delete(collection.path);

		return true;
	}

	/**
	 * Deletes a collection folder structure. This method only deletes folders
	 * and will throw an exception if any of the folders aren't empty. This
	 * ensures that only a release that has been published can be deleted.
	 * 
	 * @param path
	 *            The {@link Path} to the collection folder.
	 * @throws IOException
	 *             If any of the subfolders is not empty or if a filesystem
	 *             error occurs.
	 */
	private void delete(Path path) throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path directory : stream) {
				// Recursively delete directories only:
				if (Files.isDirectory(directory)) {
					delete(directory);
				}
			}
		}
		Files.delete(path);
	}

}
