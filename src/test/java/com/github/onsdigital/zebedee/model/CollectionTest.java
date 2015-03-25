package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;


public class CollectionTest {

    Zebedee zebedee;
    Collection collection;
    Builder builder;
    String email = "patricia@example.com";

    @Before
    public void setUp() throws Exception {
        builder = new Builder(this.getClass());
        zebedee = new Zebedee(builder.zebedee);
        Root.zebedee = zebedee;
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
        CollectionDescription collectionDescription = new CollectionDescription(name);
        String filename = PathUtils.toFilename(name);

        // When
        Collection.create(collectionDescription, zebedee);

        // Then
        Path rootPath = builder.zebedee.resolve(Zebedee.COLLECTIONS);
        Path releasePath = rootPath.resolve(filename);
        Path jsonPath = rootPath.resolve(filename + ".json");

        assertTrue(StringUtils.isNotEmpty(collectionDescription.id));

        assertTrue(Files.exists(releasePath));
        assertTrue(Files.exists(jsonPath));
        assertTrue(Files.exists(releasePath.resolve(Collection.APPROVED)));
        assertTrue(Files.exists(releasePath.resolve(Collection.COMPLETE)));
        assertTrue(Files.exists(releasePath.resolve(Collection.IN_PROGRESS)));

        CollectionDescription createdCollectionDescription;
        try (InputStream inputStream = Files.newInputStream(jsonPath)) {
            createdCollectionDescription = Serialiser.deserialise(inputStream, CollectionDescription.class);
        }

        assertNotNull(createdCollectionDescription);
        assertEquals(collectionDescription.name, createdCollectionDescription.name);
        assertEquals(collectionDescription.publishDate, createdCollectionDescription.publishDate);
    }

    @Test
    public void shouldRenameCollection() throws IOException {

        // Given
        String name = "Population Release";
        CollectionDescription collectionDescription = new CollectionDescription(name);
        String newName = "Economy Release";

        String filename = PathUtils.toFilename(newName);

        // When
        Collection.create(collectionDescription, zebedee);
        Collection.rename(collectionDescription, newName, zebedee);

        // Then
        Path rootPath = builder.zebedee.resolve(Zebedee.COLLECTIONS);
        Path releasePath = rootPath.resolve(filename);
        Path jsonPath = rootPath.resolve(filename + ".json");

        Path oldJsonPath = rootPath.resolve(PathUtils.toFilename(name) + ".json");

        assertTrue(Files.exists(releasePath));
        assertTrue(Files.exists(jsonPath));
        assertTrue(!Files.exists(oldJsonPath));
        assertTrue(Files.exists(releasePath.resolve(Collection.APPROVED)));
        assertTrue(Files.exists(releasePath.resolve(Collection.COMPLETE)));
        assertTrue(Files.exists(releasePath.resolve(Collection.IN_PROGRESS)));

        CollectionDescription renamedCollectionDescription;
        try (InputStream inputStream = Files.newInputStream(jsonPath)) {
            renamedCollectionDescription = Serialiser.deserialise(inputStream, CollectionDescription.class);
        }

        assertNotNull(renamedCollectionDescription);
        assertNotEquals(collectionDescription.id, renamedCollectionDescription.id);
        assertEquals(filename, renamedCollectionDescription.id);
        assertEquals(newName, renamedCollectionDescription.name);
        assertEquals(collectionDescription.publishDate, renamedCollectionDescription.publishDate);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotInstantiateInInvalidFolder() throws IOException {

        // Given
        // A folder that isn't a valid release:
        String name = "Population Release";
        CollectionDescription collectionDescription = new CollectionDescription(name);
        Collection.create(collectionDescription, zebedee);
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
        boolean created = collection.create(email, uri);

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
        builder.createPublishedFile(uri);

        // When
        boolean created = collection.create(email, uri);

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
        builder.createApprovedFile(uri);

        // When
        boolean created = collection.create(email, uri);

        // Then
        assertFalse(created);
        Path inProgress = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
        assertFalse(Files.exists(inProgress.resolve(uri.substring(1))));
    }

    @Test
    public void shouldNotCreateIfComplete() throws IOException {

        // Given
        // The content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";
        builder.createApprovedFile(uri);

        // When
        boolean created = collection.create(email, uri);

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
        builder.createInProgressFile(uri);

        // When
        boolean created = collection.create(email, uri);

        // Then
        assertFalse(created);
    }

    @Test
    public void shouldEditPublished() throws IOException {

        // Given
        // The content exists publicly:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        builder.createPublishedFile(uri);

        // When
        boolean edited = collection.edit(email, uri);

        // Then
        assertTrue(edited);
        Path inProgress = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
        assertTrue(Files.exists(inProgress.resolve(uri.substring(1))));

        Path published = builder.zebedee.resolve(Zebedee.PUBLISHED);
        Path content = published.resolve(uri.substring(1));
        assertTrue(Files.exists(content));
    }

    @Test
    public void shouldEditApproved() throws IOException {

        // Given
        // The content exists, has been edited and approved:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        builder.createPublishedFile(uri);
        builder.createApprovedFile(uri);

        // When
        boolean edited = collection.edit(email, uri);

        // Then

        // It should be edited
        assertTrue(edited);

        // It should be in in progress
        Path inProgress = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
        assertTrue(Files.exists(inProgress.resolve(uri.substring(1))));

        // The approved copy should still be there in case we need to roll back
        Path approved = builder.collections.get(1).resolve(Collection.APPROVED);
        assertTrue(Files.exists(approved.resolve(uri.substring(1))));
    }

    @Test
    public void shouldEditIfEditingAlready() throws IOException {

        // Given
        // The content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        builder.createInProgressFile(uri);

        // When
        boolean edited = collection.edit(email, uri);

        // Then
        assertTrue(edited);
    }

    @Test
    public void shouldNotEditIfEditingElsewhere() throws IOException {

        // Given
        // The content already exists in another release:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        builder.isBeingEditedElsewhere(uri, 0);

        // When
        boolean edited = collection.edit(email, uri);

        // Then
        assertFalse(edited);
    }

    @Test
    public void shouldNotEditIfDoesNotExist() throws IOException {

        // Given
        // The content does not exist:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";

        // When
        boolean edited = collection.edit(email, uri);

        // Then
        assertFalse(edited);
    }

    @Test
    public void shouldApprove() throws IOException {

        // Given
        // The content exists, has been edited and approved:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        builder.createPublishedFile(uri);
        builder.createApprovedFile(uri);
        builder.createInProgressFile(uri);

        // When
        boolean approved = collection.approve(email, uri);

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
        builder.createApprovedFile(uri);

        // When
        boolean approved = collection.approve(email, uri);

        // Then
        assertFalse(approved);
    }

    @Test
    public void shouldBeInProgress() throws IOException {

        // Given
        // The content is currently being edited:
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        builder.createInProgressFile(uri);

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
        builder.createApprovedFile(uri);

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
        builder.createApprovedFile(uri);
        builder.createInProgressFile(uri);

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
        builder.createPublishedFile(uri);
        builder.createApprovedFile(uri);
        builder.createInProgressFile(uri);

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

    @Test
    public void shouldFilterOnPermissions() throws IOException {

        // Given
        // We have different content in each of published, approved and in progress
        String uri = "/economy/inflationandpriceindices/timeseries/permissions.html";
        Path published = builder.createPublishedFile(uri);
        Path approved = builder.createApprovedFile(uri);
        Path inProgress = builder.createInProgressFile(uri);
        String publishedContent = Random.id();
        String approvedContent = Random.id();
        String inProgressContent = Random.id();
        FileUtils.writeStringToFile(published.toFile(), publishedContent);
        FileUtils.writeStringToFile(approved.toFile(), approvedContent);
        FileUtils.writeStringToFile(inProgress.toFile(), inProgressContent);

        // When
        // A user without permissions attempts to locate the content
        Path found = collection.find("user.without.permissions@example.com", uri);
        String foundContent = FileUtils.readFileToString(found.toFile());

        // Then
        // The published content should be returned, not approved or in progress
        assertEquals(publishedContent, foundContent);
    }

}
