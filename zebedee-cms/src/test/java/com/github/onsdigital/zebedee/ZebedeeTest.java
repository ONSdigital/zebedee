package com.github.onsdigital.zebedee;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.io.FileUtils;
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


public class ZebedeeTest extends ZebedeeTestBaseFixture {

	Path expectedPath;
	Map<String, String> env;

	public void setUp() throws Exception {
		env = Root.env;
		Root.env = new HashMap<>();
		expectedPath = builder.parent;
	}

	@Override
	public void tearDown() throws Exception {
		// do nothing.
	}

	@Test
	public void shouldInstantiate() throws IOException {

		// Given
		// An existing Zebedee structure

		// When
		new Zebedee(new ZebedeeConfiguration(expectedPath, false));

		// Then
		// No error should occur.
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldNotInstantiate() throws IOException {

		// Given
		// No Zebedee structure
		FileUtils.deleteDirectory(expectedPath.toFile());

		// When
		new Zebedee(new ZebedeeConfiguration(expectedPath, false));

		// Then
		// An exception should be thrown
	}

	@Test
	public void shouldListReleases() throws IOException {

		// Given
		Zebedee zebedee = new Zebedee(new ZebedeeConfiguration(expectedPath, false));

		// When
		List<Collection> releases = zebedee.getCollections().list();

		// Then
		assertEquals(builder.collections.size(), releases.size());
	}

	@Test
	public void shouldNotBeBeingEdited() throws IOException {

		// Given
		Zebedee zebedee = new Zebedee(new ZebedeeConfiguration(expectedPath, false));

		// When
		int actual = zebedee.isBeingEdited(builder.contentUris.get(0));

		// Then
		assertEquals(0, actual);
	}

	@Test
	public void shouldBeBeingEdited() throws IOException {

		// Given
		Zebedee zebedee = new Zebedee(new ZebedeeConfiguration(expectedPath, false));
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
	public void toUri_givenCollectionFilePath_shouldReturnUri() throws IOException {
		// Given
		// a zebedee implementation
		Zebedee zebedee = new Zebedee(new ZebedeeConfiguration(expectedPath, false));
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
	public void toUri_givenCollectionFilePathAsString_shouldReturnUri() throws IOException {
		// Given
		// a zebedee implementation
		Zebedee zebedee = new Zebedee(new ZebedeeConfiguration(expectedPath, false));
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
	public void toUri_givenPathOutsideZebedee_shouldReturnNull() throws IOException {
		// Given
		// a zebedee implementation
		Zebedee zebedee = new Zebedee(new ZebedeeConfiguration(expectedPath, false));
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
		Zebedee zebedee = new Zebedee(new ZebedeeConfiguration(expectedPath, false));
		Collection collectionOne = zebedee.getCollections().getCollectionByName(COLLECTION_ONE_NAME);
		Collection collectionTwo = zebedee.getCollections().getCollectionByName(COLLECTION_TWO_NAME);

		String contentPath = "/aboutus/data.json";

		// create content in collection 01.
		builder.createInProgressFile(contentPath);

		Session session = new Session();
		session.setEmail("makingData@greatagain.com");
		Optional<Collection> blockingCollection = zebedee.checkForCollectionBlockingChange(collectionOne, contentPath);

		assertThat(blockingCollection.isPresent(), is(true));
		assertThat(blockingCollection.get().getDescription().getName(), equalTo(collectionTwo.getDescription().getName()));
		assertThat(collectionTwo.inProgressUris().contains(contentPath), is(true));
		assertThat(collectionTwo.completeUris().contains(contentPath), is(false));
		assertThat(collectionTwo.reviewedUris().contains(contentPath), is(false));
	}

	@Test
	public void shouldReturnEmptyOptionalIfNoCollectionContainsSpecifiedURI() throws IOException, CollectionNotFoundException {
		Zebedee zebedee = new Zebedee(new ZebedeeConfiguration(expectedPath, false));
		String contentPath = "/aboutus/data.json";
		Collection collectionOne = zebedee.getCollections().getCollectionByName(COLLECTION_ONE_NAME);

		Session session = new Session();
		session.setEmail("makingData@greatagain.com");
		Optional<Collection> blockingCollection = zebedee.checkForCollectionBlockingChange(collectionOne, contentPath);
		assertThat(blockingCollection.isPresent(), is(false));

		Collection collectionTwo = zebedee.getCollections().getCollectionByName(COLLECTION_TWO_NAME);
		assertThat(collectionTwo.inProgressUris().contains(contentPath), is(false));
		assertThat(collectionTwo.completeUris().contains(contentPath), is(false));
		assertThat(collectionTwo.reviewedUris().contains(contentPath), is(false));
	}
}
