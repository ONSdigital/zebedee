package com.github.onsdigital.zebedee.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.zebedee.Taxonomy;
import com.github.onsdigital.zebedee.Zebedee;

public class Root implements Startup {

	static Path root;
	static Zebedee zebedee;

	@Override
	public void init() {
		try {

			// Create a Zebedee folder:
			root = Files.createTempDirectory("zebedee");
			zebedee = Zebedee.create(root);

			// List the taxonomy files:
			// ResourceUtils.classLoaderClass = Root.class;
			// Path taxonomy = ResourceUtils.getPath("/taxonomy");
			Path taxonomy = Paths.get(".").resolve("target/taxonomy");
			List<Path> files = new ArrayList<>();
			listFiles(taxonomy.toAbsolutePath(), taxonomy, files);

			// Extract the content:
			for (Path source : files) {
				Path destination = zebedee.published.path.resolve(source
						.toString());
				Files.createDirectories(destination.getParent());
				try (InputStream input = Files.newInputStream(source);
						OutputStream output = Files
								.newOutputStream(destination)) {
					IOUtils.copy(input, output);
				}
			}

			System.out.println("Zebedee root is at: " + root.toAbsolutePath());

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Recursively lists all files within this {@link Taxonomy}.
	 * 
	 * @param path
	 *            The path to start from. This method calls itself recursively.
	 * @param files
	 *            The list to which results will be added.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	private static void listFiles(Path base, Path path, List<Path> files)
			throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path entry : stream) {
				if (Files.isDirectory(entry)) {
					listFiles(base, entry, files);
				} else {
					// Path relative = base.relativize(entry.toAbsolutePath());
					files.add(entry);
				}
			}
		}
	}

	/**
	 * Cleans up
	 */
	@Override
	protected void finalize() throws Throwable {
		System.out.println(" - Deleting Zebeddee at: " + root);
		try {
			FileUtils.deleteDirectory(root.toFile());
			System.out.println(" - Deleting Zebeddee complete: " + root);
		} catch (Throwable t) {
			System.out.println(" - Error deleting Zebedee: ");
			System.out.println(t.getStackTrace());
		}
	}

}
