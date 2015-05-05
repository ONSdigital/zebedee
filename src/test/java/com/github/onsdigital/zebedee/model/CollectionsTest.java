package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

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
		Collection collection = new Collection( builder.collections.get(0), zebedee);

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
		Collection collection = new Collection( builder.collections.get(0), zebedee);
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
		Collection collection = new Collection( builder.collections.get(0), zebedee);
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
		Collection collection = new Collection( builder.collections.get(0), zebedee);
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
		Collection collection = new Collection( builder.collections.get(0), zebedee);
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
		Collection collection = new Collection( builder.collections.get(0), zebedee);
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
		Collection collection = new Collection( builder.collections.get(0), zebedee);
		String uri = "test.json";

		// When
		// We attempt to call the method
		zebedee.collections.deleteContent(collection, uri, session);

		// Then
		// We should get the expected exception, not a null pointer.
	}
}
