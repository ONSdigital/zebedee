package com.github.onsdigital.zebedee.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.github.onsdigital.zebedee.model.Content;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TaxonomyTest {

	Path path;
	Content taxonomy;

	@Before
	public void setUp() throws Exception {
		path = Files.createTempDirectory(this.getClass().getSimpleName());
		taxonomy = new Content(path);
	}

	@After
	public void tearDown() throws Exception {
		FileUtils.deleteDirectory(path.toFile());
	}

	@Test
	public void shouldExist() throws IOException {

		// Given
		String folder = "economy";
		String name = "data.json";
		URI uri = URI.create("/" + folder + "/" + name);
		Files.createDirectory(path.resolve(folder));
		Files.createFile(path.resolve(folder).resolve(name));

		// When
		boolean exists = taxonomy.exists(uri);

		// Then
		assertTrue(exists);
	}

	@Test
	public void shouldGet() throws IOException {

		// Given
		String folder = "economy";
		String name = "data.json";
		URI uri = URI.create("/" + folder + "/" + name);
		Files.createDirectory(path.resolve(folder));
		Files.createFile(path.resolve(folder).resolve(name));

		// When
		Path path = taxonomy.get(uri);

		// Then
		assertNotNull(path);
	}

	@Test
	public void shouldNotExist() throws IOException {

		// Given
		String folder = "economy";
		String name = "nothere.json";
		URI uri = URI.create("/" + folder + "/" + name);

		// When
		boolean exists = taxonomy.exists(uri);

		// Then
		assertFalse(exists);
	}

	@Test
	public void shouldNotGet() throws IOException {

		// Given
		String folder = "economy";
		String name = "nothere.json";
		URI uri = URI.create("/" + folder + "/" + name);

		// When
		Path path = taxonomy.get(uri);

		// Then
		assertNull(path);
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfFolderDoesntExist() throws IOException {

		// Given
		FileUtils.deleteDirectory(path.toFile());

		// When
		taxonomy = new Content(path);

		// Then
		// We should get an exception because the directory is invalid.
	}

	@Test
	public void shouldListUris() throws IOException {

		// Given
		String uri1 = "/some/content.html";
		String uri2 = "/some/other/content.html";
		Path path1 = taxonomy.toPath(uri1);
		Path path2 = taxonomy.toPath(uri2);
		Files.createDirectories(path1.getParent());
		Files.createDirectories(path2.getParent());
		Files.createFile(path1);
		Files.createFile(path2);

		// When
		List<String> uris = taxonomy.uris();

		// Then
		assertEquals(2, uris.size());
		assertTrue(uris.contains(uri1));
		assertTrue(uris.contains(uri2));
	}

}
