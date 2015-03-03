package com.github.onsdigital.zebedee;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReleaseTest {

	Zebedee zebedee;
	Release release;
	Builder builder;

	@Before
	public void setUp() throws Exception {
		builder = new Builder(this.getClass());
		zebedee = new Zebedee(builder.zebedee);
		release = new Release(builder.releases.get(1), zebedee);
	}

	@After
	public void tearDown() throws Exception {
		builder.delete();
	}

	@Test
	public void shouldCreateRelease() throws IOException {

		// Given
		// The content doesn't exist at any level:
		String name = "Population Release";

		// When
		Release.create(name, zebedee);

		// Then
		Path releasePath = builder.zebedee.resolve(Zebedee.RELEASES).resolve(
				PathUtils.toFilename(name));
		assertTrue(Files.exists(releasePath));
		assertTrue(Files.exists(releasePath.resolve(Release.APPROVED)));
		assertTrue(Files.exists(releasePath.resolve(Release.IN_PROGRESS)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldNotInstantiateInInvalidFolder() throws IOException {

		// Given
		// A folder that isn't a valid release:
		String name = "Population Release";
		Release.create(name, zebedee);
		Path releasePath = builder.zebedee.resolve(Zebedee.RELEASES).resolve(
				PathUtils.toFilename(name));
		FileUtils.cleanDirectory(releasePath.toFile());

		// When
		new Release(releasePath, zebedee);

		// Then
		// We should get an exception.
	}

	@Test
	public void shouldCreate() throws IOException {

		// Given
		// The content doesn't exist at any level:
		String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";

		// When
		boolean created = release.create(uri);

		// Then
		assertTrue(created);
		Path inProgress = builder.releases.get(1).resolve(Release.IN_PROGRESS);
		assertTrue(Files.exists(inProgress.resolve(uri.substring(1))));
	}

	@Test
	public void shouldNotCreateIfPublished() throws IOException {

		// Given
		// The content already exists:
		String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";
		isPublished(uri);

		// When
		boolean created = release.create(uri);

		// Then
		assertFalse(created);
		Path inProgress = builder.releases.get(1).resolve(Release.IN_PROGRESS);
		assertFalse(Files.exists(inProgress.resolve(uri.substring(1))));
	}

	@Test
	public void shouldNotCreateIfApproved() throws IOException {

		// Given
		// The content already exists:
		String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";
		isApproved(uri);

		// When
		boolean created = release.create(uri);

		// Then
		assertFalse(created);
		Path inProgress = builder.releases.get(1).resolve(Release.IN_PROGRESS);
		assertFalse(Files.exists(inProgress.resolve(uri.substring(1))));
	}

	@Test
	public void shouldNotCreateIfInProgress() throws IOException {

		// Given
		// The content already exists:
		String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";
		isInProgress(uri);

		// When
		boolean created = release.create(uri);

		// Then
		assertFalse(created);
	}

	@Test
	public void shouldEditPublished() throws IOException {

		// Given
		// The content exists publicly:
		String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
		isPublished(uri);

		// When
		boolean edited = release.edit(uri);

		// Then
		assertTrue(edited);
		Path inProgress = builder.releases.get(1).resolve(Release.IN_PROGRESS);
		assertTrue(Files.exists(inProgress.resolve(uri.substring(1))));
	}

	@Test
	public void shouldEditApproved() throws IOException {

		// Given
		// The content exists, has been edited and approved:
		String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
		isPublished(uri);
		isApproved(uri);

		// When
		boolean edited = release.edit(uri);

		// Then
		assertTrue(edited);
		Path inProgress = builder.releases.get(1).resolve(Release.IN_PROGRESS);
		assertTrue(Files.exists(inProgress.resolve(uri.substring(1))));
	}

	@Test
	public void shouldNotEditIfEditingAlready() throws IOException {

		// Given
		// The content already exists:
		String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
		isInProgress(uri);

		// When
		boolean edited = release.edit(uri);

		// Then
		assertFalse(edited);
	}

	@Test
	public void shouldNotEditIfEditingElsewhere() throws IOException {

		// Given
		// The content already exists in another release:
		String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
		isBeingEditedElsewhere(uri);

		// When
		boolean edited = release.edit(uri);

		// Then
		assertFalse(edited);
	}

	@Test
	public void shouldNotEditIfDoesNotExist() throws IOException {

		// Given
		// The content does not exist:
		String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";

		// When
		boolean edited = release.edit(uri);

		// Then
		assertFalse(edited);
	}

	@Test
	public void shouldApprove() throws IOException {

		// Given
		// The content exists, has been edited and approved:
		String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
		isPublished(uri);
		isApproved(uri);
		isInProgress(uri);

		// When
		boolean approved = release.approve(uri);

		// Then
		assertTrue(approved);
		Path edited = builder.releases.get(1).resolve(Release.IN_PROGRESS);
		assertFalse(Files.exists(edited.resolve(uri.substring(1))));
	}

	@Test
	public void shouldNotApproveIfNotEditing() throws IOException {

		// Given
		// The content already exists:
		String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
		isApproved(uri);

		// When
		boolean approved = release.approve(uri);

		// Then
		assertFalse(approved);
	}

	@Test
	public void shouldCopy() throws IOException {

		// Given
		// The content exists, has been edited and approved:
		String sourceUri = "/economy/inflationandpriceindices/timeseries/raid1.html";
		String targetUri = "/economy/inflationandpriceindices/timeseries/raid2.html";
		isPublished(sourceUri);

		// When
		boolean copied = release.copy(sourceUri, targetUri);

		// Then
		assertTrue(copied);
		Path edited = builder.releases.get(1).resolve(Release.IN_PROGRESS);
		assertTrue(Files.exists(edited.resolve(targetUri.substring(1))));
	}

	@Test
	public void shouldNotCopyIfSourceDoesNotExist() throws IOException {

		// Given
		// The source URI does not exist:
		String sourceUri = "/economy/inflationandpriceindices/timeseries/raid1.html";
		String targetUri = "/economy/inflationandpriceindices/timeseries/raid2.html";

		// When
		boolean copied = release.copy(sourceUri, targetUri);

		// Then
		assertFalse(copied);
	}

	@Test
	public void shouldNotCopyIfTargetApproved() throws IOException {

		// Given
		// The source URI does not exist:
		String sourceUri = "/economy/inflationandpriceindices/timeseries/raid1.html";
		String targetUri = "/economy/inflationandpriceindices/timeseries/raid2.html";
		isApproved(targetUri);

		// When
		boolean copied = release.copy(sourceUri, targetUri);

		// Then
		assertFalse(copied);
	}

	@Test
	public void shouldNotCopyIfTargetInProgress() throws IOException {

		// Given
		// The source URI does not exist:
		String sourceUri = "/economy/inflationandpriceindices/timeseries/raid1.html";
		String targetUri = "/economy/inflationandpriceindices/timeseries/raid2.html";
		isInProgress(targetUri);

		// When
		boolean copied = release.copy(sourceUri, targetUri);

		// Then
		assertFalse(copied);
	}

	@Test
	public void shouldNotCopyIfTargetBeingEditedElsewhere() throws IOException {

		// Given
		// The source URI does not exist:
		String sourceUri = "/economy/inflationandpriceindices/timeseries/raid1.html";
		String targetUri = "/economy/inflationandpriceindices/timeseries/raid2.html";
		isBeingEditedElsewhere(targetUri);

		// When
		boolean copied = release.copy(sourceUri, targetUri);

		// Then
		assertFalse(copied);
	}

	@Test
	public void shouldBeInProgress() throws IOException {

		// Given
		// The content exists, has been edited and approved:
		String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
		isInProgress(uri);

		// When
		boolean inProgress = release.isInProgress(uri);
		boolean inRelease = release.isInRelease(uri);

		// Then
		assertTrue(inProgress);
		assertTrue(inRelease);
	}

	@Test
	public void shouldBeApproved() throws IOException {

		// Given
		// The content exists, has been edited and approved:
		String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
		isApproved(uri);

		// When
		boolean approved = release.isApproved(uri);
		boolean inRelease = release.isInRelease(uri);

		// Then
		assertTrue(approved);
		assertTrue(inRelease);
	}

	@Test
	public void shouldGetPath() throws IOException {

		// Given
		// We're editing some content:
		String uri = "/economy/inflationandpriceindices/timeseries/beer.html";
		isPublished(uri);
		isApproved(uri);
		isInProgress(uri);

		// When
		// We write some output to the content:
		Path path = release.getPath(uri);
		try (Writer writer = Files.newBufferedWriter(path,
				Charset.forName("utf8"));) {
			writer.append("test");
		}

		// Then
		// The output should have gone to the expected copy of the file:
		Path inProgressPath = builder.releases.get(1).resolve(Release.IN_PROGRESS);
		Path expectedPath = inProgressPath.resolve(uri.substring(1));
		assertTrue(Files.size(expectedPath) > 0);
	}

	// -----------------------------------------

	/**
	 * Creates a published file.
	 * 
	 * @param uri
	 *            The URI to be created.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	private void isPublished(String uri) throws IOException {

		Path published = builder.zebedee.resolve(Zebedee.PUBLISHED);
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
	private void isApproved(String uri) throws IOException {

		Path approved = builder.releases.get(1).resolve(Release.APPROVED);
		Path content = approved.resolve(uri.substring(1));
		Files.createDirectories(content.getParent());
		Files.createFile(content);
	}

	/**
	 * Creates an approved file in a different release.
	 * 
	 * @param uri
	 *            The URI to be created.
	 * @throws IOException
	 *             If a filesystem error occurs.
	 */
	private void isBeingEditedElsewhere(String uri) throws IOException {

		Path approved = builder.releases.get(0).resolve(Release.APPROVED);
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
	private void isInProgress(String uri) throws IOException {

		Path inProgress = builder.releases.get(1).resolve(Release.IN_PROGRESS);
		Path content = inProgress.resolve(uri.substring(1));
		Files.createDirectories(content.getParent());
		Files.createFile(content);
	}

}
