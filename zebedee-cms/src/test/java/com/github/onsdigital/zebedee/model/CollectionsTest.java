package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.data.json.DirectoryListing;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.Session;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CollectionsTest {

    Zebedee zebedee;
    Builder builder;

    @Before
    public void setUp() throws Exception {
        builder = new Builder(this.getClass());
        zebedee = new Zebedee(builder.zebedee, false);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }

    @Test
    public void shouldFindCollection() throws Exception {
        zebedee.openSession(builder.administratorCredentials);
        Collections.CollectionList collections = new Collections.CollectionList();

        Collection firstCollection = Collection.create(
                new CollectionDescription("FirstCollection"), zebedee, builder.administrator.email);
        Collection secondCollection = Collection.create(
                new CollectionDescription("SecondCollection"), zebedee, builder.administrator.email);

        collections.add(firstCollection);
        collections.add(secondCollection);

        Collection firstCollectionFound = collections
                .getCollection(firstCollection.description.id);
        Collection secondCollectionFound = collections
                .getCollection(secondCollection.description.id);

        assertEquals(firstCollection.description.id,
                firstCollectionFound.description.id);
        assertEquals(firstCollection.description.name,
                firstCollectionFound.description.name);
        assertEquals(secondCollection.description.id,
                secondCollectionFound.description.id);
        assertEquals(secondCollection.description.name,
                secondCollectionFound.description.name);
    }

    @Test
    public void shouldReturnNullIfNotFound() throws Exception {

        Collections.CollectionList collections = new Collections.CollectionList();
        zebedee.openSession(builder.administratorCredentials);

        Collection firstCollection = Collection.create(
                new CollectionDescription("FirstCollection"), zebedee, builder.administrator.email);

        collections.add(firstCollection);

        assertNull(collections.getCollection("SecondCollection"));
    }

    @Test
    public void shouldHaveCollectionForName() throws Exception {
        Collections.CollectionList collectionList = new Collections.CollectionList();
        zebedee.openSession(builder.administratorCredentials);

        Collection firstCollection = Collection.create(
                new CollectionDescription("FirstCollection"), zebedee, builder.administrator.email);
        Collection secondCollection = Collection.create(
                new CollectionDescription("SecondCollection"), zebedee, builder.administrator.email);

        collectionList.add(firstCollection);
        collectionList.add(secondCollection);

        assertTrue(collectionList.hasCollection("FirstCollection"));
        assertTrue(collectionList.hasCollection("SecondCollection"));
        assertFalse(collectionList
                .hasCollection("SomeCollectionThatDoesNotExist"));
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnApprove()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null collection
        Collection collection = null;
        Session session = zebedee.openSession(builder.administratorCredentials);

        // When
        // We attempt to approve
        zebedee.collections.approve(collection, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnListDirectory()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null collection
        Collection collection = null;
        String uri = "test.json";
        Session session = zebedee.openSession(builder.administratorCredentials);

        // When
        // We attempt to list directory
        zebedee.collections.listDirectory(collection, uri, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnComplete()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null collection
        Collection collection = null;
        String uri = "test.json";
        Session session = zebedee.openSession(builder.administratorCredentials);

        // When
        // We attempt to complete
        zebedee.collections.complete(collection, uri, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnDelete()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null collection
        Collection collection = null;
        String uri = "test.json";
        Session session = zebedee.openSession(builder.administratorCredentials);

        // When
        // We attempt to delete
        zebedee.collections.delete(collection, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnReadContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null collection
        Collection collection = null;
        String uri = "test.json";
        Session session = zebedee.openSession(builder.administratorCredentials);
        HttpServletResponse response = null;

        // When
        // We attempt to read content
        zebedee.collections.readContent(collection, uri, false, session, response);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnWriteContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null collection
        Collection collection = null;
        String uri = "test.json";
        Session session = zebedee.openSession(builder.administratorCredentials);
        HttpServletRequest request = null;
        InputStream inputStream = null;

        // When
        // We attempt to call the method
        zebedee.collections.writeContent(collection, uri, session, request,
                inputStream);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnDeleteContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null collection
        Collection collection = null;
        String uri = "test.json";
        Session session = zebedee.openSession(builder.administratorCredentials);

        // When
        // We attempt to call the method
        zebedee.collections.deleteContent(collection, uri, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnMoveContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given a null collection
        Collection collection = null;
        String uri = "test.json";
        String toUri = "testnew.json";
        Session session = zebedee.openSession(builder.administratorCredentials);

        // When we attempt to call the method
        zebedee.collections.moveContent(session, collection, uri, toUri);

        // Then we should get the expected exception, not a null pointer.
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForBlankUriOnMoveContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given an empty URI.
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        String uri = "";
        String toUri = "testnew.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);

        // When we attempt to call the method
        zebedee.collections.moveContent(session, collection, uri, toUri);

        // Then we should get the expected exception, not a null pointer.
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForBlankToUriOnMoveContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given an empty to URI.
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        String uri = "test";
        String toUri = "";
        Session session = zebedee.openSession(builder.publisher1Credentials);

        // When we attempt to call the method
        zebedee.collections.moveContent(session, collection, uri, toUri);

        // Then we should get the expected exception, not a null pointer.
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnApprove()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null session
        Session session = null;
        Collection collection = new Collection(builder.collections.get(0), zebedee);

        // When
        // We attempt to approve
        zebedee.collections.approve(collection, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnListDirectory()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null session
        Session session = null;
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        String uri = "test.json";

        // When
        // We attempt to list directory
        zebedee.collections.listDirectory(collection, uri, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnComplete()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null session
        Session session = null;
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        String uri = "test.json";

        // When
        // We attempt to complete
        zebedee.collections.complete(collection, uri, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnDelete()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null session
        Session session = null;
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        String uri = "test.json";

        // When
        // We attempt to delete
        zebedee.collections.delete(collection, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnReadContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null session
        Session session = null;
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        String uri = "test.json";
        HttpServletResponse response = null;

        // When
        // We attempt to read content
        zebedee.collections.readContent(collection, uri, false, session, response);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnWriteContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null session
        Session session = null;
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        String uri = "test.json";
        HttpServletRequest request = null;
        InputStream inputStream = null;

        // When
        // We attempt to call the method
        zebedee.collections.writeContent(collection, uri, session, request,
                inputStream);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnDeleteContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null session
        Session session = null;
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        String uri = "test.json";

        // When
        // We attempt to call the method
        zebedee.collections.deleteContent(collection, uri, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnMoveContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given a null session
        Session session = null;
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        String uri = "test.json";
        String toUri = "testnew.json";

        // When we attempt to call the method
        zebedee.collections.moveContent(session, collection, uri, toUri);

        // Then we should get the expected exception, not a null pointer.
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestIfNoUriOnReadContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null session
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        String uri = null;
        HttpServletResponse response = null;

        // When
        // We attempt to read content
        zebedee.collections.readContent(collection, uri, false, session, response);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestIfNoUriOnWriteContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null session
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        String uri = null;
        HttpServletRequest request = null;
        InputStream inputStream = null;

        // When
        // We attempt to call the method
        zebedee.collections.writeContent(collection, uri, session, request,
                inputStream);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestIfNoUriOnDeleteContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null session
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        String uri = null;

        // When
        // We attempt to call the method
        zebedee.collections.deleteContent(collection, uri, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundIfUriNotInProgressOnComplete()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A URI that is not in progress
        String uri = "/this/content/is/not/in/progress.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);

        // When
        // We attempt to call the method
        zebedee.collections.complete(collection, uri, session);

        // Then
        // We should get the expected exception
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestIfUriIsADirectoryOnComplete()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A URI that indicates a directory
        String uri = "/this/is/a/directory";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.email, uri + "/file.json"));

        // When
        // We attempt to call the method
        zebedee.collections.complete(collection, uri, session);

        // Then
        // We should get the expected exception
    }

    @Test
    public void shouldCompleteContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A URI that indicates a file
        String uri = "/this/is/valid/content.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.email, uri));

        // When
        // We attempt to call the method
        zebedee.collections.complete(collection, uri, session);

        // Then
        assertTrue(collection.isComplete(uri));
    }

    @Test(expected = ConflictException.class)
    public void shouldNotApproveIfAUriIsInProgress()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A URI that is in progress
        String uri = "/this/is/in/progress.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.email, uri));

        // When
        // We attempt to approve
        zebedee.collections.approve(collection, session);

        // Then
        // We should get the expected exception
    }

    @Test(expected = ConflictException.class)
    public void shouldNotApproveIfAUriIsComplete()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A URI that is in progress
        String uri = "/this/is/complete.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.email, uri));
        assertTrue(collection.complete(builder.publisher1.email, uri));

        // When
        // We attempt to approve
        zebedee.collections.approve(collection, session);

        // Then
        // We should get the expected exception
    }

    @Test
    public void shouldApproveCollection()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A collection that's ready to approve
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);

        // When
        // We attempt to approve
        zebedee.collections.approve(collection, session);

        // Then
        // The collection should be approved (reloading to make sure it's saved)
        assertTrue(new Collection(builder.collections.get(0), zebedee).description.approvedStatus);
        assertTrue(collection.description.events.hasEventForType(EventType.APPROVED));
    }

    @Test
    public void shouldUnlockCollection()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A collection that's approved.
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        zebedee.collections.approve(collection, session);

        // When
        // We attempt to unlock
        zebedee.collections.unlock(collection, session);

        // Then
        // The collection should be unlocked (approved = false)
        assertFalse(new Collection(builder.collections.get(0), zebedee).description.approvedStatus);

        // And an unlocked event should exist.
        assertTrue(collection.description.events.hasEventForType(EventType.UNLOCKED));
    }

    @Test
    public void shouldUnlockWithoutAddingEventIfAlreadyUnlocked()
            throws Exception {

        // Given
        // A collection that's not been approved.
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);

        // When
        // We attempt to unlock
        zebedee.collections.unlock(collection, session);

        // Then
        // The collection should be unlocked (approved = false)
        assertFalse(new Collection(builder.collections.get(0), zebedee).description.approvedStatus);
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnUnlock()
            throws Exception {

        // Given
        // A null session
        Session session = null;
        Collection collection = new Collection(builder.collections.get(0), zebedee);

        // When
        // We attempt to unlock
        zebedee.collections.unlock(collection, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForNullCollectionOnUnlock()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null collection
        Collection collection = null;
        Session session = zebedee.openSession(builder.administratorCredentials);

        // When
        // We attempt to approve
        zebedee.collections.unlock(collection, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = NotFoundException.class)
    public void shouldGetNotFoundIfAttemptingToListNonexistentDirectory()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A directory that doesn't exist
        String uri = "/this/directory/doesnt/exist";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);

        // When
        // We attempt to list the directory
        zebedee.collections.listDirectory(collection, uri, session);

        // Then
        // We should get the expected exception
    }

    @Test(expected = BadRequestException.class)
    public void shouldGetBadRequestIfAttemptingToListDirectoryOnAFile()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A URI that points to a file
        String uri = "/this/is/a/file.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.email, uri));

        // When
        // We attempt to list the file as a directory
        zebedee.collections.listDirectory(collection, uri, session);

        // Then
        // We should get the expected exception
    }

    @Test
    public void shouldListDirectory()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A URI that points to a valid directory
        String uri = "/this/is/a/directory";
        String file = "file.json";
        String folder = "folder";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.email, uri + "/" + file));
        assertTrue(collection.create(builder.publisher1.email, uri + "/" + folder + "/" + file));

        // When
        // We attempt to list the directory
        DirectoryListing directoryListing = zebedee.collections.listDirectory(collection, uri, session);

        // Then
        // We should get the file
        assertEquals(1, directoryListing.files.size());
        assertEquals(1, directoryListing.folders.size());
    }

    @Test
    public void shouldListDirectoryOverlayed()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A URI that points to a valid directory
        String uri = "/this/is/a/directory";
        String file = "file.json";
        String folder = "folder";
        Session session = zebedee.openSession(builder.publisher1Credentials);

        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.email, uri + "/" + file));
        assertTrue(collection.create(builder.publisher1.email, uri + "/" + folder + "/" + file));

        builder.createPublishedFile(uri + "/" + file); // this file is in both published and the collection so there should be only one entry for it
        builder.createPublishedFile(uri + "/publishedFile.json"); // This file is only in published and should be in the over layed listing.
        builder.createPublishedFile(uri + "/publishedFolder/" + file); // This folder is only in published and should be in the overlayed listing

        // When
        // We attempt to list the directory
        DirectoryListing directoryListing = zebedee.collections.listDirectoryOverlayed(collection, uri, session);

        // Then
        // We should get the file
        assertEquals(2, directoryListing.files.size());
        assertEquals(2, directoryListing.folders.size());
    }

    @Test(expected = BadRequestException.class)
    public void shouldNotDeleteCollectionIfNotEmpty()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A collection with some content in it
        String uri = "/this/is/some/content.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.email, uri));

        // When
        // We attempt to delete the collection
        zebedee.collections.delete(collection, session);

        // Then
        // We should get the expected exception
    }

    @Test
    public void shouldDeleteCollection()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // An empty collection
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);

        // When
        // We attempt to delete the collection
        zebedee.collections.delete(collection, session);

        // Then
        // The collection folder should have been deleted
        assertFalse(Files.exists(builder.collections.get(0)));
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundForReadingNonexistentFile()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A nonexisten file
        String uri = "/this/file/doesnt/exist.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        HttpServletResponse response = null;

        // When
        // We attempt to read the file
        zebedee.collections.readContent(collection, uri, false, session, response);

        // Then
        // We should get the expected exception
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForReadingADirectoryAsAFile()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A nonexisten file
        String uri = "/this/is/a/directory/";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.email, uri + "file.json"));
        HttpServletResponse response = null;

        // When
        // We attempt to read the file
        zebedee.collections.readContent(collection, uri, false, session, response);

        // Then
        // We should get the expected exception
    }

    @Test
    public void shouldReadFile()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A nonexisten file
        String uri = "/file.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.email, uri));
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // When
        // We attempt to read the file
        zebedee.collections.readContent(collection, uri, false, session, response);

        // Then
        // Check the expected interactions
        verify(response).setContentType("application/json");
        verify(response, atLeastOnce()).getOutputStream();
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForWritingADirectoryAsAFile()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A directory instead of a file
        String uri = "/this/is/a/directory/";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.email, uri + "file.json"));
        HttpServletRequest request = null;
        InputStream inputStream = null;

        // When
        // We attempt to write to the directory as if it were a file
        zebedee.collections.writeContent(collection, uri, session, request, inputStream);

        // Then
        // We should get the expected exception
    }

    @Test(expected = ConflictException.class)
    public void shouldThrowConflictForCreatingFileBeingEditedElsewhere()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A file in a different collection
        String uri = "/this/is/a/file/in/another/collection.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        Collection otherCollection = new Collection(builder.collections.get(1), zebedee);
        assertTrue(otherCollection.create(builder.publisher1.email, uri));
        HttpServletRequest request = null;
        InputStream inputStream = null;

        // When
        // We attempt to write to the directory as if it were a file
        zebedee.collections.writeContent(collection, uri, session, request, inputStream);

        // Then
        // We should get the expected exception
    }

    @Test(expected = ConflictException.class)
    public void shouldThrowConflictForEditingFileBeingEditedElsewhere()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A file being edited in a different collection
        String uri = "/this/is/a/file/in/another/collection.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        Collection otherCollection = new Collection(builder.collections.get(1), zebedee);
        Path path = zebedee.published.toPath(uri);
        Files.createDirectories(path.getParent());
        Files.createFile(path);
        assertTrue(otherCollection.edit(builder.publisher1.email, uri));
        HttpServletRequest request = null;
        InputStream inputStream = null;

        // When
        // We attempt to write to the directory as if it were a file
        zebedee.collections.writeContent(collection, uri, session, request, inputStream);

        // Then
        // We should get the expected exception
    }

    @Test
    public void shouldWriteContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A new file
        String uri = "/this/a/file.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        HttpServletRequest request = mock(HttpServletRequest.class);
        InputStream inputStream = mock(InputStream.class);
        when(inputStream.read(any(byte[].class))).thenReturn(-1);

        // When
        // We attempt to write to the directory as if it were a file
        zebedee.collections.writeContent(collection, uri, session, request, inputStream);

        // Then
        // We should see the file
        Path path = collection.getInProgressPath(uri);
        assertNotNull(path);
        assertTrue(Files.exists(path));
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundForDeletingNonexistentFile()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A file that doesn't exist in the collection
        String uri = "/this/file/does/not/exist.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);

        // When
        // We attempt to delete the nonexistent file
        zebedee.collections.deleteContent(collection, uri, session);

        // Then
        // We should get the expected exception
    }

    @Test
    public void shouldDeleteFile()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A file that doesn't exist in the collection
        String uri = "/this/is/a/file.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        collection.create(builder.publisher1.email, uri);

        // When
        // We attempt to delete the nonexistent file
        boolean result = zebedee.collections.deleteContent(collection, uri, session);

        // Then
        // The file should be gone
        assertTrue(result);
        assertNull(collection.find(builder.publisher1.email, uri));
    }

    @Test
    public void shouldDeleteFolderRecursively()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A file that doesn't exist in the collection
        String folderUri = "/this/is/a/folder/";
        String fileUri = folderUri + "file.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        collection.create(builder.publisher1.email, fileUri);

        // When
        // We attempt to delete the nonexistent file
        boolean result = zebedee.collections.deleteContent(collection, folderUri, session);

        // Then
        // The file should be gone
        // NB current behaviour is that files in the folder are deleted,
        // but not the folder itself. This may need to be reviewed,
        // however this test has been written to reflect current behaviour.
        assertTrue(result);
        assertNull(collection.find(builder.publisher1.email, fileUri));
        assertNotNull(collection.find(builder.publisher1.email, folderUri));
    }

    @Test(expected = ConflictException.class)
    public void shouldThrowConflictExceptionOnCreateContentIfAlreadyPublished() throws Exception {
        String uri = "/this/is/a/directory/file.json";

        Collection collection = new Collection(builder.collections.get(0), zebedee);

        builder.createPublishedFile(uri);
        zebedee.collections.createContent(collection, uri, null, null, null);
    }

    @Test(expected = ConflictException.class)
    public void shouldThrowConflictExceptionOnCreateContentIfAlreadyInCollection() throws Exception {
        String uri = "/this/is/a/directory/file.json";

        Collection collection = new Collection(builder.collections.get(0), zebedee);

        builder.createInProgressFile(uri);
        zebedee.collections.createContent(collection, uri, null, null, null);
    }

    @Test
    public void shouldEditCollectionConcurrently() throws Exception {

        // Given
        // A collection
        zebedee.openSession(builder.administratorCredentials);
        Collection collection = Collection.create(
                new CollectionDescription("collection"), zebedee, builder.administrator.email);

        // When the collection is updated concurrently.
        ExecutorService executor = Executors.newCachedThreadPool();
        List<UpdateCollection> runnables = new ArrayList<>();

        long startTime = System.currentTimeMillis();


        for (int i = 0; i < 20; ++i) {
            runnables.add(new UpdateCollection(collection));
        }

        for (UpdateCollection runnable : runnables) {
            executor.execute(runnable);
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
        }


        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
        System.out.println("duration: " + duration);

        // Then the collection file should still be readable.
        for (UpdateCollection runnable : runnables) {
            assertFalse(runnable.failed);
        }
    }

    @Test
    public void shouldWriteEncryptedContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A new file
        String uri = "/this/a/file.json";
        Session session = zebedee.openSession(builder.publisher1Credentials);

        Collection collection = new Collection(builder.collections.get(0), zebedee);
        HttpServletRequest request = mock(HttpServletRequest.class);
        InputStream inputStream = mock(InputStream.class);
        when(inputStream.read(any(byte[].class))).thenReturn(-1);

        // When
        // We attempt to write to the directory as if it were a file
        zebedee.collections.writeContent(collection, uri, session, request, inputStream);

        // Then
        // We should see the file
        Path path = collection.getInProgressPath(uri);
        assertNotNull(path);
        assertTrue(Files.exists(path));
    }


    public class UpdateCollection implements Runnable {
        public boolean failed = false;
        private Collection collection;

        public UpdateCollection(Collection collection) throws IOException {
            this.collection = collection;
        }

        @Override
        public void run() {
            try {
                // When
                // We attempt to get the session

                Collection collectionToUpdate = collection.zebedee.collections.list().getCollection(collection.description.id);
                collectionToUpdate.addEvent("/", new Event(new Date(), EventType.EDITED, "fred@testing.com"));
                collectionToUpdate.save();
                Collection updatedCollection = collection.zebedee.collections.list().getCollection(collection.description.id);

                // Then
                // The expected session should be returned
                if (updatedCollection == null || !StringUtils.equals(updatedCollection.description.id, this.collection.description.id))
                    failed = true;

            } catch (Exception e) {
                failed = true;
                e.printStackTrace();
                System.out.println();
            }
        }
    }

}
