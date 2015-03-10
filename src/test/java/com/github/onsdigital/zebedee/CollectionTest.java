package com.github.onsdigital.zebedee;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CollectionTest {

	Zebedee zebedee;
	Collection collection;
	Builder builder;

	@Before
	public void setUp() throws Exception {
		builder = new Builder(this.getClass());
		zebedee = new Zebedee(builder.zebedee);
		collection = new Collection(builder.collections.get(1), zebedee);
	}

	@After
	public void tearDown() throws Exception {
		builder.delete();
	}

	@Test
    public void shouldCreateCollection() throws IOException {

		// Given
		// The content doesn't exist at any level:
		String name = "Population Release";
        String filename = PathUtils.toFilename(name);

		// When
		Collection.create(name, zebedee);

		// Then
        Path rootPath = builder.zebedee.resolve(Zebedee.COLLECTIONS);
        Path releasePath = rootPath.resolve(filename);
        Path jsonPath = rootPath.resolve(filename + ".json");

		assertTrue(Files.exists(releasePath));
        assertTrue(Files.exists(jsonPath));
        assertTrue(Files.exists(releasePath.resolve(Collection.APPROVED)));
		assertTrue(Files.exists(releasePath.resolve(Collection.IN_PROGRESS)));
	}

    @Test
    public void shouldRenameCollection() throws IOException {

        // Given
        String name = "Population Release";
        String newName = "Economy Release";

        String filename = PathUtils.toFilename(newName);

        // When
        Collection.create(name, zebedee);
        Collection.rename(name, newName, zebedee);

        // Then
        Path rootPath = builder.zebedee.resolve(Zebedee.COLLECTIONS);
        Path releasePath = rootPath.resolve(filename);
        Path jsonPath = rootPath.resolve(filename + ".json");

        Path oldJsonPath = rootPath.resolve(PathUtils.toFilename(name) + ".json");

        assertTrue(Files.exists(releasePath));
        assertTrue(Files.exists(jsonPath));
        assertTrue(!Files.exists(oldJsonPath));
        assertTrue(Files.exists(releasePath.resolve(Collection.APPROVED)));
        assertTrue(Files.exists(releasePath.resolve(Collection.IN_PROGRESS)));
    }

	@Test(expected = IllegalArgumentException.class)
	public void shouldNotInstantiateInInvalidFolder() throws IOException {

		// Given
		// A folder that isn't a valid release:
		String name = "Population Release";
		Collection.create(name, zebedee);
		Path releasePath = builder.zebedee.resolve(Zebedee.COLLECTIONS).resolve(
				PathUtils.toFilename(name));
		FileUtils.cleanDirectory(releasePath.toFile());

		// When
		new Collection(releasePath, zebedee);

		// Then
		// We should get an exception.
	}

	@Test
	public void shouldCreate() throws IOException {

		// Given
		// The content doesn't exist at any level:
		String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";

		// When
		boolean created = collection.create(uri);

		// Then
		assertTrue(created);
		Path inProgress = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
		assertTrue(Files.exists(inProgress.resolve(uri.substring(1))));
	}

	@Test
	public void shouldNotCreateIfPublished() throws IOException {

		// Given
		// The content already exists:
		String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";
		builder.isPublished(uri);

		// When
		boolean created = collection.create(uri);

		// Then
		assertFalse(created);
		Path inProgress = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
		assertFalse(Files.exists(inProgress.resolve(uri.substring(1))));
	}

	@Test
	public void shouldNotCreateIfApproved() throws IOException {

		// Given
		// The content already exists:
		String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";
		builder.isApproved(uri);

		// When
		boolean created = collection.create(uri);

		// Then
		assertFalse(created);
		Path inProgress = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
		assertFalse(Files.exists(inProgress.resolve(uri.substring(1))));
	}

	@Test
	public void shouldNotCreateIfInProgress() throws IOException {

		// Given
		// The content already exists:
		String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";
		builder.isInProgress(uri);

		// When
		boolean created = collection.create(uri);

		// Then
		assertFalse(created);
	}

	@Test
	public void shouldEditPublished() throws IOException {

		// Given
		// The content exists publicly:
		String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
		builder.isPublished(uri);

		// When
		boolean edited = collection.edit(uri);

		// Then
		assertTrue(edited);
		Path inProgress = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
		assertTrue(Files.exists(inProgress.resolve(uri.substring(1))));
	}

	@Test
	public void shouldEditApproved() throws IOException {

		// Given
		// The content exists, has been edited and approved:
		String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
		builder.isPublished(uri);
		builder.isApproved(uri);

		// When
		boolean edited = collection.edit(uri);

		// Then
		assertTrue(edited);
		Path inProgress = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
		assertTrue(Files.exists(inProgress.resolve(uri.substring(1))));
	}

	@Test
	public void shouldNotEditIfEditingAlready() throws IOException {

		// Given
		// The content already exists:
		String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
		builder.isInProgress(uri);

		// When
		boolean edited = collection.edit(uri);

		// Then
		assertFalse(edited);
	}

	@Test
	public void shouldNotEditIfEditingElsewhere() throws IOException {

		// Given
		// The content already exists in another release:
		String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
		builder.isBeingEditedElsewhere(uri, 0);

		// When
		boolean edited = collection.edit(uri);

		// Then
		assertFalse(edited);
	}

	@Test
	public void shouldNotEditIfDoesNotExist() throws IOException {

		// Given
		// The content does not exist:
		String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";

		// When
		boolean edited = collection.edit(uri);

		// Then
		assertFalse(edited);
	}

	@Test
	public void shouldApprove() throws IOException {

		// Given
		// The content exists, has been edited and approved:
		String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
		builder.isPublished(uri);
		builder.isApproved(uri);
		builder.isInProgress(uri);

		// When
		boolean approved = collection.approve(uri);

		// Then
		assertTrue(approved);
		Path edited = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
		assertFalse(Files.exists(edited.resolve(uri.substring(1))));
	}

	@Test
	public void shouldNotApproveIfNotEditing() throws IOException {

		// Given
		// The content already exists:
		String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
		builder.isApproved(uri);

		// When
		boolean approved = collection.approve(uri);

		// Then
		assertFalse(approved);
	}

	@Test
	public void shouldCopy() throws IOException {

		// Given
		// The content exists, has been edited and approved:
		String sourceUri = "/economy/inflationandpriceindices/timeseries/raid1.html";
		String targetUri = "/economy/inflationandpriceindices/timeseries/raid2.html";
		builder.isPublished(sourceUri);

		// When
		boolean copied = collection.copy(sourceUri, targetUri);

		// Then
		assertTrue(copied);
		Path edited = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
		assertTrue(Files.exists(edited.resolve(targetUri.substring(1))));
	}

	@Test
	public void shouldNotCopyIfSourceDoesNotExist() throws IOException {

		// Given
		// The source URI does not exist:
		String sourceUri = "/economy/inflationandpriceindices/timeseries/raid1.html";
		String targetUri = "/economy/inflationandpriceindices/timeseries/raid2.html";

		// When
		boolean copied = collection.copy(sourceUri, targetUri);

		// Then
		assertFalse(copied);
	}

	@Test
	public void shouldNotCopyIfTargetApproved() throws IOException {

		// Given
		// The target URI is already approved:
		String sourceUri = "/economy/inflationandpriceindices/timeseries/raid1.html";
		String targetUri = "/economy/inflationandpriceindices/timeseries/raid2.html";
		builder.isApproved(targetUri);

		// When
		boolean copied = collection.copy(sourceUri, targetUri);

		// Then
		assertFalse(copied);
	}

	@Test
	public void shouldNotCopyIfTargetInProgress() throws IOException {

		// Given
		// The target URI is currently being edited:
		String sourceUri = "/economy/inflationandpriceindices/timeseries/raid1.html";
		String targetUri = "/economy/inflationandpriceindices/timeseries/raid2.html";
		builder.isInProgress(targetUri);

		// When
		boolean copied = collection.copy(sourceUri, targetUri);

		// Then
		assertFalse(copied);
	}

	@Test
	public void shouldNotCopyIfTargetBeingEditedElsewhere() throws IOException {

		// Given
		// The source URI is being edited in another release:
		String sourceUri = "/economy/inflationandpriceindices/timeseries/raid1.html";
		String targetUri = "/economy/inflationandpriceindices/timeseries/raid2.html";
		builder.isBeingEditedElsewhere(targetUri, 0);

		// When
		boolean copied = collection.copy(sourceUri, targetUri);

		// Then
		assertFalse(copied);
	}

	@Test
	public void shouldBeInProgress() throws IOException {

		// Given
		// The content is currently being edited:
		String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
		builder.isInProgress(uri);

		// When
		boolean inProgress = collection.isInProgress(uri);
		boolean inRelease = collection.isInCollection(uri);

		// Then
		assertTrue(inProgress);
		assertTrue(inRelease);
	}

	@Test
	public void shouldBeApproved() throws IOException {

		// Given
		// The content has been approved:
		String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
		builder.isApproved(uri);

		// When
		boolean approved = collection.isApproved(uri);
		boolean inRelease = collection.isInCollection(uri);

		// Then
		assertTrue(approved);
		assertTrue(inRelease);
	}

	@Test
	public void shouldNotBeApprovedIfInProgress() throws IOException {

		// Given
		// The content has been approved:
		String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
		builder.isApproved(uri);
		builder.isInProgress(uri);

		// When
		boolean approved = collection.isApproved(uri);
		boolean inRelease = collection.isInCollection(uri);

		// Then
		assertFalse(approved);
		assertTrue(inRelease);
	}

	@Test
	public void shouldGetPath() throws IOException {

		// Given
		// We're editing some content:
		String uri = "/economy/inflationandpriceindices/timeseries/beer.html";
		builder.isPublished(uri);
		builder.isApproved(uri);
		builder.isInProgress(uri);

		// When
		// We write some output to the content:
		Path path = collection.getInProgressPath(uri);
		try (Writer writer = Files.newBufferedWriter(path,
				Charset.forName("utf8"));) {
			writer.append("test");
		}

		// Then
		// The output should have gone to the expected copy of the file:
		Path inProgressPath = builder.collections.get(1).resolve(
				Collection.IN_PROGRESS);
		Path expectedPath = inProgressPath.resolve(uri.substring(1));
		assertTrue(Files.size(expectedPath) > 0);
	}

}
