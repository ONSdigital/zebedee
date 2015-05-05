package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.DirectoryListing;
import com.github.onsdigital.zebedee.json.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static org.junit.Assert.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

public class CollectionsTest {

    Zebedee zebedee;
    Builder builder;

    @Before
    public void setUp() throws Exception {
        builder = new Builder(this.getClass());
        zebedee = new Zebedee(builder.zebedee);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }

    @Test
    public void shouldFindCollection() throws IOException {
        Collections.CollectionList collections = new Collections.CollectionList();

        Collection firstCollection = Collection.create(
                new CollectionDescription("FirstCollection"), zebedee);
        Collection secondCollection = Collection.create(
                new CollectionDescription("SecondCollection"), zebedee);

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
    public void shouldReturnNullIfNotFound() throws IOException {

        Collections.CollectionList collections = new Collections.CollectionList();

        Collection firstCollection = Collection.create(
                new CollectionDescription("FirstCollection"), zebedee);

        collections.add(firstCollection);

        assertNull(collections.getCollection("SecondCollection"));
    }

    @Test
    public void shouldHaveCollectionForName() throws IOException {
        Collections.CollectionList collectionList = new Collections.CollectionList();

        Collection firstCollection = Collection.create(
                new CollectionDescription("FirstCollection"), zebedee);
        Collection secondCollection = Collection.create(
                new CollectionDescription("SecondCollection"), zebedee);

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
            ConflictException {

        // Given
        // A null collection
        Collection collection = null;
        Session session = zebedee.sessions.create(builder.administrator.email);

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
        Session session = zebedee.sessions.create(builder.administrator.email);

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
        Session session = zebedee.sessions.create(builder.administrator.email);

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
        Session session = zebedee.sessions.create(builder.administrator.email);

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
        Session session = zebedee.sessions.create(builder.administrator.email);
        HttpServletResponse response = null;

        // When
        // We attempt to read content
        zebedee.collections.readContent(collection, uri, session, response);

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
        Session session = zebedee.sessions.create(builder.administrator.email);
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
        Session session = zebedee.sessions.create(builder.administrator.email);

        // When
        // We attempt to call the method
        zebedee.collections.deleteContent(collection, uri, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnApprove()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException {

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
        zebedee.collections.readContent(collection, uri, session, response);

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


    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestIfNoUriOnReadContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null session
        Session session = zebedee.sessions.create(builder.publisher1.email);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        String uri = null;
        HttpServletResponse response = null;

        // When
        // We attempt to read content
        zebedee.collections.readContent(collection, uri, session, response);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestIfNoUriOnWriteContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A null session
        Session session = zebedee.sessions.create(builder.publisher1.email);
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
        Session session = zebedee.sessions.create(builder.publisher1.email);
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
        Session session = zebedee.sessions.create(builder.publisher1.email);
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
        Session session = zebedee.sessions.create(builder.publisher1.email);
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
        Session session = zebedee.sessions.create(builder.publisher1.email);
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
        Session session = zebedee.sessions.create(builder.publisher1.email);
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
        Session session = zebedee.sessions.create(builder.publisher1.email);
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
        Session session = zebedee.sessions.create(builder.publisher1.email);
        Collection collection = new Collection(builder.collections.get(0), zebedee);

        // When
        // We attempt to approve
        zebedee.collections.approve(collection, session);

        // Then
        // The collection should be approved (reloading to make sure it's saved)
        assertTrue(new Collection(builder.collections.get(0), zebedee).description.approvedStatus);
    }

    @Test(expected = NotFoundException.class)
    public void shouldGetNotFoundIfAttemptingToListNonexistentDirectory()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A directory that doesn't exist
        String uri = "/this/directory/doesnt/exist";
        Session session = zebedee.sessions.create(builder.publisher1.email);
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
        Session session = zebedee.sessions.create(builder.publisher1.email);
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
        Session session = zebedee.sessions.create(builder.publisher1.email);
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

    @Test(expected = BadRequestException.class)
    public void shouldNotDeleteCollectionIfNotEmpty()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given
        // A collection with some content in it
        String uri = "/this/is/some/content.json";
        Session session = zebedee.sessions.create(builder.publisher1.email);
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
        Session session = zebedee.sessions.create(builder.publisher1.email);
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
        Session session = zebedee.sessions.create(builder.publisher1.email);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        HttpServletResponse response = null;

        // When
        // We attempt to read the file
        zebedee.collections.readContent(collection, uri, session, response);

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
        Session session = zebedee.sessions.create(builder.publisher1.email);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.email, uri + "file.json"));
        HttpServletResponse response = null;

        // When
        // We attempt to read the file
        zebedee.collections.readContent(collection, uri, session, response);

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
        Session session = zebedee.sessions.create(builder.publisher1.email);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.email, uri));
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // When
        // We attempt to read the file
        zebedee.collections.readContent(collection, uri, session, response);

        // Then
        // Check the expected interactions
        verify(response).setContentType("application/json");
        verify(response, atLeastOnce()).getOutputStream();
    }
}
