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
		return StringUtils.abbreviateMiddle(filename.toString(), "_",
				MAX_LENGTH);
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
		if (!Files.exists(destination.getParent())) {
			Files.createDirectories(destination.getParent());
		}
		try (InputStream input = Files.newInputStream(source);
				OutputStream output = Files.newOutputStream(destination)) {
			IOUtils.copy(input, output);
		}
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

		// Create any necessary parent folders:
		if (!Files.exists(destination.getParent())) {
			Files.createDirectories(destination.getParent());
		}

		// Move if we can:
		if (!Files.exists(destination)) {
			Files.move(source, destination);
		} else {
			try (InputStream input = Files.newInputStream(source);
					OutputStream output = Files.newOutputStream(destination)) {
				IOUtils.copy(input, output);
			}
			Files.delete(source);
		}
	}
}
