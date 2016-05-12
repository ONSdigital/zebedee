package com.github.onsdigital.zebedee;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ZebedeeTest {

	Path expectedPath;
	Builder builder;
	Map<String, String> env;

	@Before
	public void setUp() throws Exception {
		env = Root.env;
		Root.env = new HashMap<>(); // Run tests with known environment variables

		builder = new Builder();
		expectedPath = builder.parent.resolve(Zebedee.ZEBEDEE);
	}

	@After
	public void tearDown() throws Exception {
		builder.delete();
		Root.env = env;
	}

	@Test
	public void shouldCreate() throws IOException, UnauthorizedException, NotFoundException, BadRequestException {

		// Given
		// No existing Zebedee structure
		FileUtils.deleteDirectory(expectedPath.toFile());

		// When
		Zebedee.create(builder.parent);

		// Then
		assertTrue(Files.exists(expectedPath));
		assertTrue(Files.exists(expectedPath.resolve(Zebedee.PUBLISHED)));
        assertTrue(Files.exists(expectedPath.resolve(Zebedee.COLLECTIONS)));
        assertTrue(Files.exists(expectedPath.resolve(Zebedee.USERS)));
	}

	@Test
	public void shouldInstantiate() throws IOException {

		// Given
		// An existing Zebedee structure

		// When
		new Zebedee(expectedPath);

		// Then
		// No error should occur.
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldNotInstantiate() throws IOException {

		// Given
		// No Zebedee structure
		FileUtils.deleteDirectory(expectedPath.toFile());

		// When
		new Zebedee(expectedPath);

		// Then
		// An exception should be thrown
	}

	@Test
	public void shouldListReleases() throws IOException {

		// Given
		Zebedee zebedee = new Zebedee(expectedPath);

		// When
		List<Collection> releases = zebedee.collections.list();

		// Then
		assertEquals(builder.collections.size(), releases.size());
	}

	@Test
	public void shouldNotBeBeingEdited() throws IOException {

		// Given
		Zebedee zebedee = new Zebedee(expectedPath);

		// When
		int actual = zebedee.isBeingEdited(builder.contentUris.get(0));

		// Then
		assertEquals(0, actual);
	}

	@Test
	public void shouldBeBeingEdited() throws IOException {

		// Given
		Zebedee zebedee = new Zebedee(expectedPath);
		String path = builder.contentUris.get(0).substring(1);
        Path reviewed = builder.collections.get(0).resolve(Collection.REVIEWED);
        Path beingEdited = reviewed.resolve(path);
        Files.createDirectories(beingEdited.getParent());
		Files.createFile(beingEdited);

		// When
		int actual = zebedee.isBeingEdited(builder.contentUris.get(0));

		// Then
		assertEquals(1, actual);
	}

	@Test
	public void toUri_givenCollectionFilePath_shouldReturnUri() {
		// Given
		// a zebedee implementation
		Zebedee zebedee = new Zebedee(expectedPath);
		String expectedURI = "/expected";
		String inprogress = "collections/mycollection/inprogress/expected/data.json";
		String complete = "collections/mycollection/complete/expected/data.json";
		String reviewed = "collections/mycollection/reviewed/expected/data.json";

		// When
		// we convert these to URIs
		String inProgressURI = zebedee.toUri(zebedee.path.resolve(inprogress));
		String completeURI = zebedee.toUri(zebedee.path.resolve(complete));
		String reviewedURI = zebedee.toUri(zebedee.path.resolve(reviewed));

		// Then
		// we expect the uri
		assertEquals(expectedURI, inProgressURI);
		assertEquals(expectedURI, completeURI);
		assertEquals(expectedURI, reviewedURI);
	}

	@Test
	public void toUri_givenCollectionFilePathAsString_shouldReturnUri() {
		// Given
		// a zebedee implementation
		Zebedee zebedee = new Zebedee(expectedPath);
		String expectedURI = "/expected";
		String inprogress = "collections/mycollection/inprogress/expected/data.json";
		String complete = "collections/mycollection/complete/expected/data.json";
		String reviewed = "collections/mycollection/reviewed/expected/data.json";

		// When
		// we convert these to URIs
		String inProgressURI = zebedee.toUri(zebedee.path.resolve(inprogress).toString());
		String completeURI = zebedee.toUri(zebedee.path.resolve(complete).toString());
		String reviewedURI = zebedee.toUri(zebedee.path.resolve(reviewed).toString());

		// Then
		// we expect the uri
		assertEquals(expectedURI, inProgressURI);
		assertEquals(expectedURI, completeURI);
		assertEquals(expectedURI, reviewedURI);
	}

	@Test
	public void toUri_givenPathOutsideZebedee_shouldReturnNull() {
		// Given
		// a zebedee implementation
		Zebedee zebedee = new Zebedee(expectedPath);
		String notZebedee = "/NotZebedee/data.json"; // (non zebedee path)
		Path notZebedeePath = Paths.get(notZebedee);

		// When
		// we convert these to URIs
		String notZebedeeUri = zebedee.toUri(notZebedeePath);

		// Then
		// we expect the uri to be null
		assertNull(notZebedeeUri);
	}
}
