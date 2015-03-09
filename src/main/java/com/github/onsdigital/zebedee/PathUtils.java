package com.github.onsdigital.zebedee;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class PathUtils {
	static final int MAX_LENGTH = 255;

	static String toFilename(String string) {
		StringBuilder filename = new StringBuilder();

		// Strip dodgy characters:
		for (char c : string.toCharArray()) {
			if (c == '.' || Character.isJavaIdentifierPart(c)) {
				filename.append(c);
			}
		}

		// Ensure the String is a sensible length:
		return StringUtils.lowerCase(StringUtils.abbreviateMiddle(filename.toString(), "_",
				MAX_LENGTH));
	}

	/**
	 * Convenience method for copying content between two paths.
	 * 
	 * @param source
	 *            The source {@link Path}.
	 * @param destination
	 *            The destination {@link Path}.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	static void copy(Path source, Path destination) throws IOException {

		createParentFolders(destination);
		doCopy(source, destination);
	}

	/**
	 * Convenience method for moving content from one {@link Path} to another,
	 * regardless of whether the destination {@link Path} exists. If the
	 * destination exists, this method performs a copy-
	 * 
	 * @param source
	 *            The source {@link Path}.
	 * @param destination
	 *            The destination {@link Path}.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	static void move(Path source, Path destination) throws IOException {

		createParentFolders(destination);
		if (!Files.exists(destination)) {

			// Move
			Files.move(source, destination);

		} else {

			// Copy-then-delete
			doCopy(source, destination);
			Files.delete(source);
		}
	}

	private static void doCopy(Path source, Path destination)
			throws IOException {
		try (InputStream input = Files.newInputStream(source);
				OutputStream output = Files.newOutputStream(destination)) {
			IOUtils.copy(input, output);
		}
	}

	private static void createParentFolders(Path path) throws IOException {

		// Create any necessary parent folders:
		if (!Files.exists(path.getParent())) {
			Files.createDirectories(path.getParent());
		}
	}
}
