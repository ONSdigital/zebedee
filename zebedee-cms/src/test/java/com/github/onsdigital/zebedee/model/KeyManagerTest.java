package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.httpino.Serialiser;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by thomasridd on 18/11/15.
 */
@Ignore("IGNORE: user keys concurrency defect")
public class KeyManagerTest {
    Zebedee zebedee;
    Builder builder;

    @Mock
    private ZebedeeCmsService zebedeeHelperMock;

    @Mock
    private Session mockSession;

    @Mock
    private Zebedee zebedeeMock;

    @Mock
    private Collection collectionMock;

    @Mock
    private KeyringCache keyringCacheMock;

    @Mock
    private Keyring keyringMock;

    @Mock
    private SecretKey secretKeyMock;

    @Mock
    private Permissions permissionsMock;

    @Mock
    private User userOneMock;

    @Mock
    private User userTwoMock;

    @Mock
    private Users usersMock;

    @Mock
    private SessionsService sessionsServiceMock;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        builder = new Builder();
        zebedee = new Zebedee(builder.zebedee, false);
        KeyManager.setZebedeeCmsService(zebedeeHelperMock);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }

    @Test
    public void isEncrypted_whenCollectionGenerated_isSetToTrue() throws ZebedeeException, IOException {
        // Given
        // a collection is created
        Session session = zebedee.openSession(builder.publisher1Credentials);
        CollectionDescription collectionDescription = collectionDescription();
        Collection.create(collectionDescription, zebedee, session);

        // When
        // we reload it
        Collection reloaded = zebedee.getCollections().list().getCollection(collectionDescription.id);

        // Then
        // isEncrypted is false
        assertTrue(reloaded.description.isEncrypted);
    }

    @Test
    public void isEncrypted_whenSetToTrue_persists() throws IOException, ZebedeeException {
        // Given
        // a collection is created, isEncrypted is set, and is set to true
        CollectionDescription collectionDescription = createCollection(true);


        // When
        // we reload the collection
        Collection reloaded = zebedee.getCollections().list().getCollection(collectionDescription.id);

        // Then
        // isEncrypted is true
        assertEquals(true, reloaded.description.isEncrypted);
    }

    private CollectionDescription createCollection(boolean isEncrypted) throws IOException, ZebedeeException {
        Session session = zebedee.openSession(builder.publisher1Credentials);
        CollectionDescription collectionDescription = collectionDescription();
        collectionDescription.isEncrypted = isEncrypted;
        Collection.create(collectionDescription, zebedee, session);
        return collectionDescription;
    }

    @Test
    public void userKeyring_whenCollectionGenerated_hasKeyForCollection() throws ZebedeeException, IOException {
        // Given
        // a user session
        Session session = zebedee.openSession(builder.publisher1Credentials);
        assertEquals(0, builder.publisher1.keyring.size());

        // When
        // we generate the collection
        Collection.create(collectionDescription(), zebedee, session);

        // Then
        // the user has a key for the collection
        User user = zebedee.getUsers().get(session.getEmail());
        assertEquals(1, user.keyring.size());

        // and in the keyringCache
        assertEquals(1, zebedee.getKeyringCache().get(session).keys.size());
    }

    @Test
    public void otherPublisherKeyring_whenCollectionGenerated_hasKeyForCollection() throws ZebedeeException, IOException {
        // Given
        // publisher A
        Session session = zebedee.openSession(builder.publisher1Credentials);
        assertEquals(0, builder.publisher2.keyring.size());

        // When
        // a collection is generated by publisher A
        Collection.create(collectionDescription(), zebedee, session);

        // Then
        // publisher B gets a key for the collection
        User user = zebedee.getUsers().get(builder.publisher2.email);
        assertEquals(1, user.keyring.size());
    }

    private CollectionDescription publishCollection(Session session) throws IOException, ZebedeeException {
        CollectionDescription collectionDescription = collectionDescription();
        Collection.create(collectionDescription, zebedee, session);
        return collectionDescription;
    }

    @Test
    public void publisherKeyring_whenPasswordReset_receivesAllCollections() throws ZebedeeException, IOException {
        // Given
        // publisher A and details for publisher B
        Session sessionA = zebedee.openSession(builder.administratorCredentials);
        CollectionDescription collection = publishCollection(sessionA);

        assertEquals(1, zebedee.getUsers().get(builder.administrator.email).keyring().size());
        assertEquals(1, zebedee.getUsers().get(builder.publisher1.email).keyring().size());


        // When
        // publisher A resets password

        Credentials credentials = builder.publisher1Credentials;
        credentials.password = "Adam Bob Charlie Danny";
        zebedee.getUsers().setPassword(sessionA, credentials);

        // Then
        // publisher A retains keys
        User user = zebedee.getUsers().get(builder.publisher1.email);
        assertTrue(user.keyring.unlock(credentials.password));
        assertEquals(1, user.keyring().size());

    }

    @Test
    public void publisherKeyring_whenPasswordResetByAdmin_receivesNewPublicKey() throws ZebedeeException, IOException {
        // Given
        // admin A and details for publisher B
        Session sessionA = zebedee.openSession(builder.administratorCredentials);
        PublicKey initialPublicKey = builder.publisher2.keyring.getPublicKey();

        // When
        // admin A resets password
        Credentials credentials = builder.publisher2Credentials;
        credentials.password = "Adam Bob Charlie Danny";
        zebedee.getUsers().setPassword(sessionA, credentials);

        // Then
        // publisher B gets a new public key
        PublicKey secondPublicKey = zebedee.getUsers().get(builder.publisher2.email).keyring.getPublicKey();
        assertNotEquals(initialPublicKey.toString(), secondPublicKey.toString());
    }

    @Test
    public void publisherKeyring_whenPasswordResetBySelf_reencryptsKey() throws ZebedeeException, IOException {
        // Given
        // publisher A
        Session sessionA = zebedee.openSession(builder.publisher1Credentials);
        String oldPassword = builder.publisher1Credentials.password;
        assertTrue(builder.publisher1.keyring().unlock(oldPassword));

        // When
        // A resets own password
        Credentials credentials = builder.publisher1Credentials;
        credentials.oldPassword = credentials.password;
        credentials.password = "Adam Bob Charlie Danny";
        zebedee.getUsers().setPassword(sessionA, credentials);

        // Then
        // A can unlock their keyring with the new password and not the old
        User reloaded = zebedee.getUsers().get(builder.publisher1.email);
        assertTrue(reloaded.keyring.unlock(credentials.password));
        assertFalse(reloaded.keyring.unlock(oldPassword));
    }

    @Test
    public void assignKeyToUser_givenUserWithoutKeyring_doesNothing() throws IOException, ZebedeeException {
        // Given
        // a publisher user without a key
        Session session = zebedee.openSession(builder.administratorCredentials);
        User user = Serialiser.deserialise("{\"name\":\"Alison Davies\",\"email\":\"a.davies@ons.gov.uk\",\"passwordHash\":\"VewEkE+p3X4zuLQP6fMBkhrPgY99y2ajXwWfTAYifH71CfROf3I8XU/K0Ps0dakJ\"}", User.class);
        zebedee.getUsers().create(user, builder.administrator.email);
        zebedee.getPermissions().addEditor(user.email, session);

        // When
        // we publish a collection
        Collection.create(collectionDescription(), zebedee, session);

        // Then
        // they dont get a key
        user = zebedee.getUsers().get(user.email);
        assertNull(user.keyring);
    }

    @Test
    public void publisherKeyring_onCreation_receivesAllCollections() throws ZebedeeException, IOException {
        // Given
        // An administrator and a collection
        Session sessionA = zebedee.openSession(builder.administratorCredentials);
        Collection collection = Collection.create(collectionDescription(), zebedee, sessionA);
        assertEquals(1, zebedee.getUsers().get(builder.administrator.email).keyring().size());

        // When
        // a new user is created and assigned Publisher permissions
        User test = new User();
        test.name = "Test User";
        test.email = Random.id() + "@example.com";
        test.inactive = false;
        zebedee.getUsers().create(sessionA, test);

        Credentials credentials = new Credentials();
        credentials.email = test.email;
        credentials.password = "password";
        zebedee.getUsers().setPassword(sessionA, credentials);
        when(zebedeeHelperMock.getCollection(anyString()))
                .thenReturn(collection);

        zebedee.getPermissions().addEditor(test.email, sessionA);

        // Then
        // publisher A retains keys
        User user = zebedee.getUsers().get(test.email);
        assertTrue(user.keyring.unlock("password"));
        assertEquals(1, user.keyring().size());

    }

    @Test
    public void schedulerKeyring_whenUserLogsIn_populates() throws IOException, ZebedeeException {
        // Given
        // an instance of zebedee with two collections but an empty scheduler key cache
        // (this simulates when zebedee restarts)
        Session sessionA = zebedee.openSession(builder.administratorCredentials);
        publishCollection(sessionA);
        publishCollection(sessionA);
        zebedee.getKeyringCache().schedulerCache = new ConcurrentHashMap<>();
        assertEquals(0, zebedee.getKeyringCache().schedulerCache.size());

        // When
        // a publisher signs in
        Session sessionB = zebedee.openSession(builder.publisher1Credentials);

        // Then
        // the key cache recovers the secret keys
        assertEquals(2, zebedee.getKeyringCache().schedulerCache.size());

    }

    @Test
    public void schedulerKeyring_whenCollectionCreated_getsSecretKey() throws IOException, ZebedeeException {
        // Given
        // a user that can create publications
        Session sessionA = zebedee.openSession(builder.administratorCredentials);
        assertEquals(0, zebedee.getKeyringCache().schedulerCache.size());

        // When
        // they create a couple of collections
        publishCollection(sessionA);
        publishCollection(sessionA);

        // Then
        // keys are added to the schedulerCache keyring
        assertEquals(2, zebedee.getKeyringCache().schedulerCache.size());

    }

    @Test
    public void shouldDistributeNewKeyToExpectedUsers() throws Exception {
        CollectionDescription collectionDescription = collectionDescription();
        collectionDescription.id = "0001";
        List<User> keyRecipients = new ImmutableList.Builder<User>().add(userOneMock).build();

        when(collectionMock.getDescription())
                .thenReturn(collectionDescription);
        when(zebedeeMock.getKeyringCache())
                .thenReturn(keyringCacheMock);
        when(keyringCacheMock.get(mockSession))
                .thenReturn(keyringMock);
        when(keyringMock.get(collectionDescription.id))
                .thenReturn(secretKeyMock);
        when(zebedeeMock.getPermissions())
                .thenReturn(permissionsMock);
        when(permissionsMock.getCollectionAccessMapping(zebedeeMock, collectionMock))
                .thenReturn(keyRecipients);
        when(userOneMock.keyring())
                .thenReturn(keyringMock);
        when(zebedeeMock.getUsers())
                .thenReturn(usersMock);
        when(zebedeeMock.getSessionsService())
                .thenReturn(sessionsServiceMock);
        when(sessionsServiceMock.find(anyString()))
                .thenReturn(mockSession);

        KeyManager.distributeCollectionKey(zebedeeMock, mockSession, collectionMock, true);

        verify(zebedeeMock, times(3)).getKeyringCache();
        verify(keyringCacheMock, times(2)).get(mockSession);
        verify(keyringMock, times(1)).get(collectionDescription.id);
        verify(zebedeeMock, times(1)).getPermissions();
        verify(permissionsMock, times(1)).getCollectionAccessMapping(zebedeeMock, collectionMock);
        verify(usersMock, times(1)).updateKeyring(userOneMock);
        verify(zebedeeMock, times(1)).getSessionsService();
        verify(keyringMock, times(2)).put("0001", secretKeyMock);

        verify(keyringMock, never()).remove("0001");
    }

    private CollectionDescription collectionDescription() {
        CollectionDescription collectionDescription = new CollectionDescription(this.getClass().getSimpleName() + "-"
                + Random.id());
        collectionDescription.publishDate = new Date();
        collectionDescription.type = CollectionType.scheduled;
        return collectionDescription;
    }
}