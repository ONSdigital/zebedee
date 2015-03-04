package com.github.onsdigital.zebedee;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.github.davidcarboni.restolino.json.Serialiser;

/**
 * This is a utility class to build a known {@link Zebedee} structure for
 * testing.
 * 
 * @author david
 *
 */
public class Builder {

	String[] changeSetNames = { "Inflation Q2 2015", "Labour Market Q2 2015" };
	Path parent;
	Path zebedee;
	List<Path> changeSets;
	List<String> contentUris;

	Builder(Class<?> name) throws IOException {

		// Create the structure:
		parent = Files.createTempDirectory(name.getSimpleName());
		zebedee = createZebedee(parent);

		// Create the change sets:
		changeSets = new ArrayList<>();
		for (String changeSetName : changeSetNames) {
			Path changeSet = createChangeSet(changeSetName, zebedee);
			changeSets.add(changeSet);
		}

		// Create some published content:
		Path folder = zebedee.resolve(Zebedee.PUBLISHED);
		contentUris = new ArrayList<>();
		String contentUri;
		Path contentPath;

		// Something for Economy:
		contentUri = "/economy/inflationandpriceindices/bulletins/consumerpriceinflationjune2014.html";
		contentPath = folder.resolve(contentUri.substring(1));
		Files.createDirectories(contentPath.getParent());
		Files.createFile(contentPath);
		contentUris.add(contentUri);

		// Something for Labour market:
		contentUri = "/employmentandlabourmarket/peopleinwork/earningsandworkinghours/bulletins/uklabourmarketjuly2014.html";
		contentPath = folder.resolve(contentUri.substring(1));
		Files.createDirectories(contentPath.getParent());
		Files.createFile(contentPath);
		contentUris.add(contentUri);
	}

	void delete() throws IOException {
		FileUtils.deleteDirectory(parent.toFile());
	}

	/**
	 * Creates a published file.
	 * 
	 * @param uri
	 *            The URI to be created.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	void isPublished(String uri) throws IOException {

		Path published = zebedee.resolve(Zebedee.PUBLISHED);
		Path content = published.resolve(uri.substring(1));
		Files.createDirectories(content.getParent());
		Files.createFile(content);
	}

	/**
	 * Creates an approved file.
	 * 
	 * @param uri
	 *            The URI to be created.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	void isApproved(String uri) throws IOException {

		Path approved = changeSets.get(1).resolve(ChangeSet.APPROVED);
		Path content = approved.resolve(uri.substring(1));
		Files.createDirectories(content.getParent());
		Files.createFile(content);
	}

	/**
	 * Creates an approved file in a different {@link ChangeSet}.
	 * 
	 * @param uri
	 *            The URI to be created.
	 * @param changeSet
	 *            The {@link ChangeSet} in which to create the content.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	void isBeingEditedElsewhere(String uri, int changeSet) throws IOException {

		Path approved = changeSets.get(changeSet).resolve(ChangeSet.APPROVED);
		Path content = approved.resolve(uri.substring(1));
		Files.createDirectories(content.getParent());
		Files.createFile(content);
	}

	/**
	 * Creates an in-progress file.
	 * 
	 * @param uri
	 *            The URI to be created.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	void isInProgress(String uri) throws IOException {

		Path inProgress = changeSets.get(1).resolve(ChangeSet.IN_PROGRESS);
		Path content = inProgress.resolve(uri.substring(1));
		Files.createDirectories(content.getParent());
		Files.createFile(content);
	}

	/**
	 * This method creates the expected set of folders for a Zebedee structure.
	 * This code is intentionaly copied from {@link Zebedee#create(Path)}. This
	 * ensures there's a fixed expectation, rather than relying on a method that
	 * will be tested as part of the test suite.
	 * 
	 * @param parent
	 *            The parent folder, in which the {@link Zebedee} structure will
	 *            be built.
	 * @return The root {@link Zebedee} path.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	private Path createZebedee(Path parent) throws IOException {
		Path path = Files.createDirectory(parent.resolve(Zebedee.ZEBEDEE));
		Files.createDirectory(path.resolve(Zebedee.PUBLISHED));
		Files.createDirectory(path.resolve(Zebedee.CHANGE_SETS));
		return path;
	}

	/**
	 * This method creates the expected set of folders for a Zebedee structure.
	 * This code is intentionaly copied from
	 * {@link ChangeSet#create(String, Zebedee)}. This ensures there's a fixed
	 * expectation, rather than relying on a method that will be tested as part
	 * of the test suite.
	 * 
	 * @param root
	 *            The root of the {@link Zebedee} structure
	 * @param name
	 *            The name of the {@link ChangeSet}.
	 * @return The root {@link ChangeSet} path.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	private Path createChangeSet(String name, Path root) throws IOException {

		String filename = PathUtils.toFilename(name);
		Path changeSets = root.resolve(Zebedee.CHANGE_SETS);

		// Create the folders:
		Path changeSet = changeSets.resolve(filename);
		Files.createDirectory(changeSet);
		Files.createDirectory(changeSet.resolve(ChangeSet.APPROVED));
		Files.createDirectory(changeSet.resolve(ChangeSet.IN_PROGRESS));

		// Create the description:
		Path changeSetDescription = changeSets.resolve(filename + ".json");
		ChangeSetDescription description = new ChangeSetDescription();
		description.name = name;
		try (OutputStream output = Files.newOutputStream(changeSetDescription)) {
			Serialiser.serialise(output, description);
		}

		return changeSet;
	}

}
