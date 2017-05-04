package com.github.onsdigital.zebedee;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.onsdigital.zebedee.Builder.COLLECTION_ONE_NAME;
import static com.github.onsdigital.zebedee.Builder.COLLECTION_TWO_NAME;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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

	@Ignore("IGNORE: user keys concurrency defect")
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
		List<Collection> releases = zebedee.getCollections().list();

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
		String inProgressURI = zebedee.toUri(zebedee.getPath().resolve(inprogress));
		String completeURI = zebedee.toUri(zebedee.getPath().resolve(complete));
		String reviewedURI = zebedee.toUri(zebedee.getPath().resolve(reviewed));

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
		String inProgressURI = zebedee.toUri(zebedee.getPath().resolve(inprogress).toString());
		String completeURI = zebedee.toUri(zebedee.getPath().resolve(complete).toString());
		String reviewedURI = zebedee.toUri(zebedee.getPath().resolve(reviewed).toString());

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

	@Test
	public void shouldReturnCollectionThatContainsSpecifiedURIIfExists() throws IOException, CollectionNotFoundException {
		Zebedee zebedee = new Zebedee(builder.zebedee);
		Collection collectionOne = zebedee.getCollections().getCollectionByName(COLLECTION_ONE_NAME);
		Collection collectionTwo = zebedee.getCollections().getCollectionByName(COLLECTION_TWO_NAME);

		String contentPath = "/aboutus/data.json";

		// create content in collection 01.
		builder.createInProgressFile(contentPath);

		Session session = new Session();
		session.email = "makingData@greatagain.com";
		Optional<Collection> blockingCollection = zebedee.checkForCollectionBlockingChange(collectionOne, contentPath);

		assertThat(blockingCollection.isPresent(), is(true));
		assertThat(blockingCollection.get().getDescription().name, equalTo(collectionTwo.getDescription().name));
		assertThat(collectionTwo.inProgressUris().contains(contentPath), is(true));
		assertThat(collectionTwo.completeUris().contains(contentPath), is(false));
		assertThat(collectionTwo.reviewedUris().contains(contentPath), is(false));
	}

	@Test
	public void shouldReturnEmptyOptionalIfNoCollectionContainsSpecifiedURI() throws IOException, CollectionNotFoundException {
		Zebedee zebedee = new Zebedee(builder.zebedee);
		String contentPath = "/aboutus/data.json";
		Collection collectionOne = zebedee.getCollections().getCollectionByName(COLLECTION_ONE_NAME);

		Session session = new Session();
		session.email = "makingData@greatagain.com";
		Optional<Collection> blockingCollection = zebedee.checkForCollectionBlockingChange(collectionOne, contentPath);
		assertThat(blockingCollection.isPresent(), is(false));

		Collection collectionTwo = zebedee.getCollections().getCollectionByName(COLLECTION_TWO_NAME);
		assertThat(collectionTwo.inProgressUris().contains(contentPath), is(false));
		assertThat(collectionTwo.completeUris().contains(contentPath), is(false));
		assertThat(collectionTwo.reviewedUris().contains(contentPath), is(false));
	}
}
