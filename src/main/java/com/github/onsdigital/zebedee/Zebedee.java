package com.github.onsdigital.zebedee;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Zebedee {
	static final String ZEBEDEE = "zebedee";
	static final String PUBLISHED = "published";
	static final String CHANGE_SETS = "changeSets";

	public final Path path;
	public final Content published;
	public final Path changeSets;

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
		Files.createDirectory(path.resolve(CHANGE_SETS));
		return new Zebedee(path);
	}

	Zebedee(Path path) {

		// Validate the directory:
		this.path = path;
		Path published = path.resolve(PUBLISHED);
		Path changeSetst = path.resolve(CHANGE_SETS);
		if (!Files.exists(published) || !Files.exists(changeSetst)) {
			throw new IllegalArgumentException(
					"This folder doesn't look like a change set folder: "
							+ path.toAbsolutePath());
		}
		this.published = new Content(published);
		this.changeSets = changeSetst;
	}

	/**
	 * This method works out how many {@link ChangeSet}s contain the given URI.
	 * The intention is to allow double-checking in case of concurrent editing.
	 * This should be 0 in order for someone to be allowed to edit a URI and
	 * should be 1 after editing is initiated. If this returns more than 1 after
	 * initiating editing then the current attempt to edit should be reverted -
	 * presumably a race condition.
	 * 
	 * @param uri
	 *            The URI to check.
	 * @return The number of {@link ChangeSet}s containing the given URI.
	 * @throws IOException
	 */
	public int isBeingEdited(String uri) throws IOException {
		int result = 0;

		// Is this URI present anywhere else?
		for (ChangeSet changeSet : getChangeSets()) {
			if (changeSet.isInChangeSet(uri)) {
				result++;
			}
		}

		return result;
	}

	/**
	 * 
	 * @return A list of all {@link ChangeSet}s.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	public List<ChangeSet> getChangeSets() throws IOException {
		List<ChangeSet> result = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files
				.newDirectoryStream(changeSets)) {
			for (Path path : stream) {
				if (Files.isDirectory(path)) {
					result.add(new ChangeSet(path, this));
				}
			}
		}
		return result;
	}

	public boolean publish(ChangeSet changeSet) throws IOException {

		// Check everything has been approved:
		if (changeSet.inProgress.uris().size() > 0) {
			return false;
		}

		for (String uri : changeSet.approved.uris()) {
			Path source = changeSet.approved.get(uri);
			Path destination = published.toPath(uri);
			PathUtils.move(source, destination);
		}
		return true;
	}

}
