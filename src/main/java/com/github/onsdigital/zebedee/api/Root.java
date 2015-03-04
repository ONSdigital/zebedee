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

import org.apache.commons.io.IOUtils;

import com.github.davidcarboni.restolino.framework.Startup;
import com.github.onsdigital.zebedee.Zebedee;

public class Root implements Startup {

	static Path root;
	static Zebedee zebedee;

	@Override
	public void init() {
		try {

			// Create a Zebedee folder:
			root = Files.createTempDirectory("zebedee");

			// List the taxonomy files:
			// ResourceUtils.classLoaderClass = Root.class;
			// Path taxonomy = ResourceUtils.getPath("/taxonomy");
			Path taxonomy = Paths.get(".").resolve("target/taxonomy");
			List<Path> files = new ArrayList<>();
			listFiles(taxonomy.toAbsolutePath(), taxonomy, files);

			// Extract the content:
			for (Path source : files) {
				Path destination = root.resolve(source.toString());
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

	// public static void main(String[] args) throws IOException,
	// URISyntaxException {
	//
	// CodeSource src = Root.class.getProtectionDomain().getCodeSource();
	// if (src != null) {
	// URL url = src.getLocation();
	// URI uri = url.toURI();
	// Path path = Paths.get(uri);
	// if (StringUtils.endsWithIgnoreCase(uri.getPath(), ".jar")) {
	// uri = URI.create("jar:file:" + path);
	// FileSystem fileSystem = FileSystems.newFileSystem(uri,
	// new HashMap<String, Object>());
	// path = fileSystem.getPath("/");
	// }
	// List<Path> files = new ArrayList<>();
	// listFiles(path, path, files);
	// for (Path file : files) {
	// System.out.println(file);
	// }
	//
	// path = ResourceUtils.getPath("/com");
	// files.clear();
	// listFiles(path, path, files);
	// for (Path file : files) {
	// System.out.println(file);
	// }
	//
	// // ZipInputStream zip = new ZipInputStream(jar.openStream());
	// // while (true) {
	// // ZipEntry e = zip.getNextEntry();
	// // if (e == null) {
	// // break;
	// // }
	// // String name = e.getName();
	// // System.out.println(name);
	// // if (name.startsWith("path/to/your/dir/")) {
	// // /* Do something with this entry. */
	// // // ...
	// // }
	// // }
	//
	// } else {
	// /* Fail... */
	// }
	// }

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
}
