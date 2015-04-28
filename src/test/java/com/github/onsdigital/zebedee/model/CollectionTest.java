package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.ContentEventType;
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
import java.util.List;

import static org.junit.Assert.*;

public class CollectionTest {

    Zebedee zebedee;
    Collection collection;
    Builder builder;
    String email;

    @Before
    public void setUp() throws Exception {
        builder = new Builder(this.getClass());
        zebedee = new Zebedee(builder.zebedee);
        collection = new Collection(builder.collections.get(1), zebedee);
        email = builder.publisher1.email;
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
        assertTrue(Files.exists(releasePath.resolve(Collection.REVIEWED)));
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
        assertTrue(Files.exists(releasePath.resolve(Collection.REVIEWED)));
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

        // check an event has been created for the content being created.
        collection.description.eventsByUri.get(uri).hasEventForType(ContentEventType.CREATED);
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
    public void shouldNotCreateIfReviewed() throws IOException {

        // Given
        // The content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/abmi.html";
        builder.createReviewedFile(uri);

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
        builder.createReviewedFile(uri);

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
    public void shouldDeleteAllFilesFromInProgressDirectory() throws IOException {

        // Given a content instance with a json file and csv file in it.
        String jsonFile = Random.id() + ".json";
        String csvFile = Random.id() + ".csv";

        builder.createInProgressFile("/" + jsonFile);
        builder.createInProgressFile("/" + csvFile);

        Path inProgress = builder.collections.get(1).resolve(Collection.IN_PROGRESS);

        // When the delete method is called on the json file
        boolean result = collection.deleteContent(email, jsonFile);

        // Then both the json file and csv file are deleted.
        assertTrue(result);
        assertFalse(Files.exists(inProgress.resolve(jsonFile)));
        assertFalse(Files.exists(inProgress.resolve(csvFile)));
        // check an event has been created for the content being deleted.
        collection.description.eventsByUri.get(jsonFile).hasEventForType(ContentEventType.DELETED);
    }

    @Test
    public void shouldDeleteAllFilesFromCompleteDirectory() throws IOException {

        // Given a content instance with a json file and csv file in it.
        String jsonFile = Random.id() + ".json";
        String csvFile = Random.id() + ".csv";

        builder.createCompleteFile("/" + jsonFile);
        builder.createCompleteFile("/" + csvFile);

        Path root = builder.collections.get(1).resolve(Collection.COMPLETE);

        // When the delete method is called on the json file
        boolean result = collection.deleteContent(email, jsonFile);

        // Then both the json file and csv file are deleted.
        assertTrue(result);
        assertFalse(Files.exists(root.resolve(jsonFile)));
        assertFalse(Files.exists(root.resolve(csvFile)));
        collection.description.eventsByUri.get(jsonFile).hasEventForType(ContentEventType.DELETED);
    }

    @Test
    public void shouldDeleteAllFilesFromReviewedDirectory() throws IOException {

        // Given a content instance with a json file and csv file in it.
        String jsonFile = Random.id() + ".json";
        String csvFile = Random.id() + ".csv";

        builder.createReviewedFile("/" + jsonFile);
        builder.createReviewedFile("/" + csvFile);

        Path root = builder.collections.get(1).resolve(Collection.REVIEWED);

        // When the delete method is called on the json file
        boolean result = collection.deleteContent(email, jsonFile);

        // Then both the json file and csv file are deleted.
        assertTrue(result);
        assertFalse(Files.exists(root.resolve(jsonFile)));
        assertFalse(Files.exists(root.resolve(csvFile)));
        collection.description.eventsByUri.get(jsonFile).hasEventForType(ContentEventType.DELETED);
    }

    @Test
    public void shouldDeleteOnlyGivenFileFromReviewedDirectory() throws IOException {

        // Given a content instance with a json file and csv file in it.
        String jsonFile = Random.id() + ".json";
        String csvFile = Random.id() + ".csv";

        builder.createReviewedFile("/" + jsonFile);
        builder.createReviewedFile("/" + csvFile);

        Path root = builder.collections.get(1).resolve(Collection.REVIEWED);

        // When the delete method is called on the json file
        boolean result = collection.deleteFile(jsonFile);

        // Then both the json file and csv file are deleted.
        assertTrue(result);
        assertFalse(Files.exists(root.resolve(jsonFile)));
        assertTrue(Files.exists(root.resolve(csvFile)));
    }

    @Test
    public void shouldDeleteOnlyGivenFileFromCompleteDirectory() throws IOException {

        // Given a content instance with a json file and csv file in it.
        String jsonFile = Random.id() + ".json";
        String csvFile = Random.id() + ".csv";

        builder.createCompleteFile("/" + jsonFile);
        builder.createCompleteFile("/" + csvFile);

        Path root = builder.collections.get(1).resolve(Collection.COMPLETE);

        // When the delete method is called on the json file
        boolean result = collection.deleteFile(jsonFile);

        // Then both the json file and csv file are deleted.
        assertTrue(result);
        assertFalse(Files.exists(root.resolve(jsonFile)));
        assertTrue(Files.exists(root.resolve(csvFile)));
    }

    @Test
    public void shouldDeleteOnlyGivenFileFromInProgressDirectory() throws IOException {

        // Given a content instance with a json file and csv file in it.
        String jsonFile = Random.id() + ".json";
        String csvFile = Random.id() + ".csv";

        builder.createInProgressFile("/" + jsonFile);
        builder.createInProgressFile("/" + csvFile);

        Path root = builder.collections.get(1).resolve(Collection.IN_PROGRESS);

        // When the delete method is called on the json file
        boolean result = collection.deleteFile(jsonFile);

        // Then both the json file and csv file are deleted.
        assertTrue(result);
        assertFalse(Files.exists(root.resolve(jsonFile)));
        assertTrue(Files.exists(root.resolve(csvFile)));
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

        // check an event has been created for the content being created.
        collection.description.eventsByUri.get(uri).hasEventForType(ContentEventType.EDITED);
    }

    @Test
    public void shouldEditComplete() throws IOException {

        // Given
        // The content exists, has been edited and completed:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        builder.createPublishedFile(uri);
        builder.createCompleteFile(uri);

        // When
        boolean edited = collection.edit(email, uri);

        // Then
        // It should be edited
        assertTrue(edited);

        // It should be in in progress
        Path inProgress = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
        assertTrue(Files.exists(inProgress.resolve(uri.substring(1))));

        // check the file no longer exists in complete, the previous version is no longer wanted.
        Path complete = builder.collections.get(1).resolve(Collection.COMPLETE);
        assertFalse(Files.exists(complete.resolve(uri.substring(1))));
    }

    @Test
    public void shouldEditReviewed() throws IOException {

        // Given
        // The content exists, has been edited and reviewed:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        builder.createPublishedFile(uri);
        builder.createReviewedFile(uri);

        // When
        boolean edited = collection.edit(email, uri);

        // Then
        // It should be edited
        assertTrue(edited);

        // It should be in in progress
        Path inProgress = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
        assertTrue(Files.exists(inProgress.resolve(uri.substring(1))));

        // check the file no longer exists in reviewed, the previous version is no longer wanted.
        Path reviewed = builder.collections.get(1).resolve(Collection.REVIEWED);
        assertFalse(Files.exists(reviewed.resolve(uri.substring(1))));
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
    public void shouldReviewWithReviewer() throws IOException {

        // Given
        // The content exists, has been edited and complete:
        String uri = CreateCompleteContent();

        // When
        // One of the digital publishing team reviews it
        boolean reviewed = collection.review(builder.publisher2.email, uri);

        // Then
        // The content should be reviewed and no longer located in "in progress"
        assertTrue(reviewed);
        Path edited = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
        assertFalse(Files.exists(edited.resolve(uri.substring(1))));

        // check an event has been created for the content being created.
        collection.description.eventsByUri.get(uri).hasEventForType(ContentEventType.REVIEWED);
    }

    @Test
    public void shouldNotReviewAsPublisher() throws IOException {

        // Given
        // The content exists, has been edited and complete:
        String uri = CreateCompleteContent();

        // When the original content creator attempts to review the content
        boolean reviewed = collection.review(email, uri);

        // Then
        assertFalse(reviewed);
        Path complete = builder.collections.get(1).resolve(Collection.COMPLETE);
        assertTrue(Files.exists(complete.resolve(uri.substring(1))));
    }

    @Test
    public void shouldReviewIfInProgressAsReviewer() throws IOException {

        // Given some content that has been edited and completed by a publisher:
        String uri = CreateCompleteContent();

        // When
        // One of the digital publishing team edits and reviews it
        collection.edit(email, uri);
        boolean reviewed = collection.review(builder.publisher2.email, uri);

        // Then
        // The content is set to reviewed without going through completion.
        assertTrue(reviewed);
        Path edited = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
        assertFalse(Files.exists(edited.resolve(uri.substring(1))));

        // check an event has been created for the content being created.
        collection.description.eventsByUri.get(uri).hasEventForType(ContentEventType.REVIEWED);
    }

    private String CreatePublishedContent() throws IOException {
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        builder.createPublishedFile(uri);
        return uri;
    }

    private String CreateEditedContent() throws IOException {
        String uri = CreatePublishedContent();
        collection.edit(email, uri);
        return uri;
    }

    private String CreateCompleteContent() throws IOException {
        String uri = CreateEditedContent();
        collection.complete(email, uri);
        return uri;
    }

    @Test
    public void shouldNotReviewIfInProgressAsPublisher() throws IOException {

        // Given some content that has been edited and completed by a publisher:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        builder.createPublishedFile(uri);
        collection.edit(email, uri);
        collection.complete(email, uri);
        collection.edit(email, uri);

        // When - A second publisher edits and reviews content
        boolean reviewed = collection.review(email, uri);

        // Then - the content is set to reviewed without going through completion.
        assertFalse(reviewed);
        Path edited = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
        assertTrue(Files.exists(edited.resolve(uri.substring(1))));
    }

    @Test
    public void shouldNotReviewIfContentHasNotBeenCompleted() throws IOException {

        // Given some content that has been edited by a publisher:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        builder.createPublishedFile(uri);
        collection.edit(email, uri);

        // When - A reviewer edits reviews content
        boolean reviewed = collection.review(builder.reviewer1.email, uri);

        // Then - the content is not set to reviewed.
        assertFalse(reviewed);
        Path edited = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
        assertTrue(Files.exists(edited.resolve(uri.substring(1))));
    }

    @Test
    public void shouldComplete() throws IOException {

        // Given
        // The content exists, has been edited and complete:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        builder.createPublishedFile(uri);
        builder.createInProgressFile(uri);

        // When
        boolean complete = collection.complete(email, uri);

        // Then
        assertTrue(complete);
        Path edited = builder.collections.get(1).resolve(Collection.IN_PROGRESS);
        assertFalse(Files.exists(edited.resolve(uri.substring(1))));

        // check an event has been created for the content being created.
        collection.description.eventsByUri.get(uri).hasEventForType(ContentEventType.COMPLETED);
    }

    @Test
    public void shouldNotCompleteIfReviewed() throws IOException {

        // Given
        // The content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        builder.createReviewedFile(uri);

        // When
        boolean isComplete = collection.complete(email, uri);

        // Then
        assertFalse(isComplete);
    }

    @Test
    public void shouldNotCompleteIfAlreadyComplete() throws IOException {

        // Given
        // The content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        builder.createCompleteFile(uri);

        // When
        boolean isComplete = collection.complete(email, uri);

        // Then
        assertFalse(isComplete);
    }

    @Test
    public void shouldNotCompleteIfNotEditing() throws IOException {

        // Given
        // The content already exists:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";
        builder.createCompleteFile(uri);

        // When
        boolean isComplete = collection.complete(email, uri);

        // Then
        assertFalse(isComplete);
    }

    @Test
    public void shouldNotReviewIfNotEditing() throws IOException {

        // Given
        // The content already exists:
        String uri = CreateCompleteContent();
        builder.createReviewedFile(uri);

        // When
        boolean reviewed = collection.review(email, uri);

        // Then
        assertFalse(reviewed);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotReviewIfNotPreviouslyCompleted() throws IOException {

        // Given
        // Some content:
        String uri = "/economy/inflationandpriceindices/timeseries/a9er.html";

        // When content is trying to be reviewed before being completed
        boolean reviewed = collection.review(email, uri);

        // Then the expected exception is thrown.
    }

    @Test
    public void shouldBeInProgress() throws IOException {

        // Given
        // The content is currently being edited:
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        builder.createInProgressFile(uri);

        // When
        boolean inProgress = collection.isInProgress(uri);
        boolean isInCollection = collection.isInCollection(uri);

        // Then
        assertTrue(inProgress);
        assertTrue(isInCollection);
    }

    @Test
    public void shouldBeComplete() throws IOException {

        // Given
        // The content has been completed:
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        builder.createCompleteFile(uri);

        // When
        boolean complete = collection.isComplete(uri);
        boolean isInCollection = collection.isInCollection(uri);

        // Then
        assertTrue(complete);
        assertTrue(isInCollection);
    }

    @Test
    public void shouldBeReviewed() throws IOException {

        // Given
        // The content has been reviewed:
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        builder.createReviewedFile(uri);

        // When
        boolean reviewed = collection.isReviewed(uri);
        boolean isInCollection = collection.isInCollection(uri);

        // Then
        assertTrue(reviewed);
        assertTrue(isInCollection);
    }

    @Test
    public void shouldNotBeCompleteIfInProgress() throws IOException {

        // Given
        // The content has been reviewed:
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        builder.createCompleteFile(uri);
        builder.createInProgressFile(uri);

        // When
        boolean isComplete = collection.isComplete(uri);
        boolean isInCollection = collection.isInCollection(uri);

        // Then
        assertFalse(isComplete);
        assertTrue(isInCollection);
    }

    @Test
    public void shouldNotBeReviewedIfComplete() throws IOException {

        // Given
        // The content has been complete:
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        builder.createReviewedFile(uri);
        builder.createCompleteFile(uri);

        // When
        boolean reviewed = collection.isReviewed(uri);
        boolean isInCollection = collection.isInCollection(uri);

        // Then
        assertFalse(reviewed);
        assertTrue(isInCollection);
    }

    @Test
    public void shouldNotBeReviewedIfInProgress() throws IOException {

        // Given
        // The content has been reviewed:
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        builder.createReviewedFile(uri);
        builder.createInProgressFile(uri);

        // When
        boolean reviewed = collection.isReviewed(uri);
        boolean inRelease = collection.isInCollection(uri);

        // Then
        assertFalse(reviewed);
        assertTrue(inRelease);
    }

    @Test
    public void shouldGetPath() throws IOException {

        // Given
        // We're editing some content:
        String uri = "/economy/inflationandpriceindices/timeseries/beer.html";
        builder.createPublishedFile(uri);
        builder.createReviewedFile(uri);
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
        // We have different content in each of published, reviewed and in progress
        String uri = "/economy/inflationandpriceindices/timeseries/permissions.html";
        Path published = builder.createPublishedFile(uri);
        Path reviewed = builder.createReviewedFile(uri);
        Path inProgress = builder.createInProgressFile(uri);
        String publishedContent = Random.id();
        String reviewedContent = Random.id();
        String inProgressContent = Random.id();
        FileUtils.writeStringToFile(published.toFile(), publishedContent);
        FileUtils.writeStringToFile(reviewed.toFile(), reviewedContent);
        FileUtils.writeStringToFile(inProgress.toFile(), inProgressContent);

        // When
        // A user without permissions attempts to locate the content
        Path found = collection.find("user.without.permissions@example.com", uri);
        String foundContent = FileUtils.readFileToString(found.toFile());

        // Then
        // The published content should be returned, not reviewed or in progress
        assertEquals(publishedContent, foundContent);
    }

    @Test
    public void shouldReturnInProgressUris() throws IOException {
        // Given
        // There are these files in progress:
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        String uri2 = "/economy/someotherthing/timeseries/e4c4.html";
        builder.createInProgressFile(uri);
        builder.createInProgressFile(uri2);

        // When
        // We attempt to get the in progress files.
        List<String> uris = collection.inProgressUris();

        // Then
        // We get out the expected in progress files.
        assertTrue(uris.contains(uri));
        assertTrue(uris.contains(uri2));

        // and the uri lists for other states are empty.
        assertTrue(collection.completeUris().isEmpty());
        assertTrue(collection.reviewedUris().isEmpty());
    }

    @Test
    public void shouldReturnCompleteUris() throws IOException {
        // Given
        // There are these files complete:
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        String uri2 = "/economy/someotherthing/timeseries/e4c4.html";
        builder.createCompleteFile(uri);
        builder.createCompleteFile(uri2);

        // When
        // We attempt to get the complete files.
        List<String> uris = collection.completeUris();

        // Then
        // We get out the expected complete files.
        assertTrue(uris.contains(uri));
        assertTrue(uris.contains(uri2));

        // and the uri lists for other states are empty.
        assertTrue(collection.inProgressUris().isEmpty());
        assertTrue(collection.reviewedUris().isEmpty());
    }

    @Test
    public void shouldReturnReviewedUris() throws IOException {
        // Given
        // There are these files reviewed:
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        String uri2 = "/economy/someotherthing/timeseries/e4c4.html";
        builder.createReviewedFile(uri);
        builder.createReviewedFile(uri2);

        // When
        // We attempt to get the reviewed files.
        List<String> uris = collection.reviewedUris();

        // Then
        // We get out the expected reviewed files.
        assertTrue(uris.contains(uri));
        assertTrue(uris.contains(uri2));

        // and the uri lists for other states are empty.
        assertTrue(collection.inProgressUris().isEmpty());
        assertTrue(collection.completeUris().isEmpty());
    }

    @Test
    public void shouldFindInProgressUri() throws IOException {
        // Given
        // There is a file in progress
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        builder.createInProgressFile(uri);

        // When
        // We attempt to find the file.
        Path path = collection.find(builder.publisher1.email, uri);

        // Then
        // We get the path to the in progress file.
        assertTrue(path.toString().contains("/" + Collection.IN_PROGRESS + "/"));
    }

    @Test
    public void shouldFindCompleteUri() throws IOException {
        // Given
        // There is a file in progress
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        builder.createCompleteFile(uri);

        // When
        // We attempt to find the file.
        Path path = collection.find(builder.publisher1.email, uri);

        // Then
        // We get the path to the in progress file.
        assertTrue(path.toString().contains("/" + Collection.COMPLETE + "/"));
    }

    @Test
    public void shouldFindReviewedUri() throws IOException {
        // Given
        // There is a file in progress
        String uri = "/economy/inflationandpriceindices/timeseries/d7g7.html";
        builder.createReviewedFile(uri);

        // When
        // We attempt to find the file.
        Path path = collection.find(builder.publisher1.email, uri);

        // Then
        // We get the path to the in progress file.
        assertTrue(path.toString().contains("/" + Collection.REVIEWED + "/"));
    }
}
