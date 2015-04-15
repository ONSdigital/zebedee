package com.github.onsdigital.zebedee.model;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Content {

	public final Path path;

	public Content(Path path) {
		this.path = path;
		if (!Files.exists(path)) {
			throw new IllegalArgumentException("Path does not exist: "
					+ path.toAbsolutePath());
		}
	}

	boolean exists(URI uri) {
		return exists(uri.getPath());
	}

	boolean exists(String uri) {
		Path path = toPath(uri);
		return Files.exists(path);
	}

	Path get(URI uri) {
		return get(uri.getPath());
	}

    public Path get(String uri) {
		Path path = toPath(uri);
		Path result = null;
		if (Files.exists(path)) {
			result = path;
		}
		return result;
	}

	/**
	 * Generates a {@link Path} that represents the given URI within this
	 * {@link Content}. The {@link Path} is generated whether or not a file
	 * actually exists, so this method is suitable for use when creating new
	 * content.
	 * 
	 * @param uri
	 *            The URI of the item.
	 * @return A {@link Path} to the [potential] location of the specified item.
	 */
    public Path toPath(String uri) {
		String relative = uri;
		if (StringUtils.startsWith(uri, "/")) {
			relative = StringUtils.substring(uri, 1);
		}
		return path.resolve(relative);
	}

	public List<String> uris() throws IOException {

		// Get a list of files:
		List<Path> files = new ArrayList<>();
		listFiles(path, files);

		// Convert to URIs:
		List<String> uris = new ArrayList<>();
		String uri;
		for (Path path : files) {
			uri = path.toString();
			if (!uri.startsWith("/")) {
				uri = "/" + uri;
			}
			uris.add(uri);
		}

		return uris;
	}

	/**
	 * Recursively lists all files within this {@link Content}.
	 * 
	 * @param path
	 *            The path to start from. This method calls itself recursively.
	 * @param files
	 *            The list to which results will be added.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	private void listFiles(Path path, List<Path> files) throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path entry : stream) {
				if (Files.isDirectory(entry)) {
					listFiles(entry, files);
				} else {
					Path relative = this.path.relativize(entry);
                    if (!relative.endsWith(".DS_Store"))
                        files.add(relative);
                }
			}
		}
	}

	public boolean delete(String uri) throws IOException {
		Path path = toPath(uri);

		if(Files.exists(path)) { // If there is a file to be deleted

			Files.delete(path); // Delete it

			// Delete the folder tree by walking up the folder structure
			Path delPath = path.getParent();
			while (path.equals(delPath) == false) { // Go no further than the Content root
				if (isDirEmpty(delPath)) { // If the folder is empty
					Files.delete(delPath); // delete
					delPath = delPath.getParent();
				} else {
					break;
				}
			}
			return true;
		}
		return false;
	}

	private static boolean isDirEmpty(final Path directory) throws IOException {
		try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
			return !dirStream.iterator().hasNext();
		}
	}
}
