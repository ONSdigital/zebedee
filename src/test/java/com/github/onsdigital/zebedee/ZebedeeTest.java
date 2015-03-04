package com.github.onsdigital.zebedee;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ZebedeeTest {

	Path expectedPath;
	Builder builder;

	@Before
	public void setUp() throws Exception {
		builder = new Builder(this.getClass());
		expectedPath = builder.parent.resolve(Zebedee.ZEBEDEE);
	}

	@After
	public void tearDown() throws Exception {
		builder.delete();
	}

	@Test
	public void shouldCreate() throws IOException {

		// Given
		// No existing Zebedee structure
		FileUtils.deleteDirectory(expectedPath.toFile());

		// When
		Zebedee.create(builder.parent);

		// Then
		assertTrue(Files.exists(expectedPath));
		assertTrue(Files.exists(expectedPath.resolve(Zebedee.PUBLISHED)));
		assertTrue(Files.exists(expectedPath.resolve(Zebedee.RELEASES)));
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
		List<ChangeSet> releases = zebedee.getReleases();

		// Then
		assertEquals(builder.releases.size(), releases.size());
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
		Path approved = builder.releases.get(0).resolve(ChangeSet.APPROVED);
		Path beingEdited = approved.resolve(path);
		Files.createDirectories(beingEdited.getParent());
		Files.createFile(beingEdited);

		// When
		int actual = zebedee.isBeingEdited(builder.contentUris.get(0));

		// Then
		assertEquals(1, actual);
	}

	@Test
	public void shouldPublish() throws IOException {

		// Given
		// There is content ready to be published:
		Zebedee zebedee = new Zebedee(expectedPath);
		ChangeSet release = new ChangeSet(builder.releases.get(1), zebedee);
		String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";
		builder.isApproved(uri);

		// When
		boolean published = zebedee.publish(release);

		// Then
		assertTrue(published);
		Path publishedPath = builder.zebedee.resolve(Zebedee.PUBLISHED);
		assertTrue(Files.exists(publishedPath.resolve(uri.substring(1))));
	}

	@Test
	public void shouldNotPublishIfAnythingInProgress() throws IOException {

		// Given
		// There is content ready to be published:
		Zebedee zebedee = new Zebedee(expectedPath);
		ChangeSet release = new ChangeSet(builder.releases.get(1), zebedee);
		String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";
		builder.isInProgress(uri);

		// When
		boolean published = zebedee.publish(release);

		// Then
		assertFalse(published);
		Path publishedPath = builder.zebedee.resolve(Zebedee.PUBLISHED);
		assertFalse(Files.exists(publishedPath.resolve(uri.substring(1))));
	}

}
