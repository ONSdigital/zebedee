package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.ADD_KEY_SAVE_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.CACHE_GET_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.CACHE_PUT_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_DESC_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_ID_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.EMAIL_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.GET_ACCESS_MAPPING_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.GET_SESSION_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.GET_USER_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.PASSWORD_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.REMOVE_KEY_SAVE_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.SECRET_KEY_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.UNLOCK_KEYRING_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.USER_KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.USER_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LegacyKeyringImplTest {

    static final String BERT_EMAIL = "bert@sesamestreet.com";
    static final String ERNIE_EMAIL = "ernie@sesamestreet.com";
    static final String TEST_COLLECTION_ID = "666";
    static final String TEST_PASSWORD = "1234567890";

    @Mock
    private User userBert, userErnie, storedUserBert, storedUserErnier;

    @Mock
    private Session bertSession, ernieSession;

    @Mock
    private UsersService users;

    @Mock
    private PermissionsService permissions;

    @Mock
    private com.github.onsdigital.zebedee.json.Keyring bertsKeyring, ernieKeyring, bertStoredKeyring, ernieStoredKeyring;

    @Mock
    private Sessions sessionsService;

    @Mock
    private ApplicationKeys applicationKeys;

    @Mock
    private KeyringCache keyringCache;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription collectionDescription;

    @Mock
    private SecretKey secretKey;

    private Keyring legacyKeyring;
    private KeyringException expectedEx;

    private List<User> accessMapping;

    @Before

    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        expectedEx = new KeyringException("bork");

        accessMapping = new ArrayList<User>() {{
            add(userBert);
            add(userErnie);
        }};

        // set up user Bert
        when(userBert.getEmail())
                .thenReturn(BERT_EMAIL);

        when(userBert.keyring())
                .thenReturn(bertsKeyring);

        when(bertsKeyring.unlock(TEST_PASSWORD))
                .thenReturn(true);

        when(bertsKeyring.isUnlocked())
                .thenReturn(true);

        when(keyringCache.get(userBert))
                .thenReturn(bertsKeyring);

        when(sessionsService.find(BERT_EMAIL))
                .thenReturn(bertSession);

        when(users.getUserByEmail(BERT_EMAIL))
                .thenReturn(storedUserBert);


        // set up user Ernie
        when(userErnie.getEmail())
                .thenReturn(ERNIE_EMAIL);

        when(userErnie.keyring())
                .thenReturn(ernieKeyring);

        when(ernieKeyring.unlock(TEST_PASSWORD))
                .thenReturn(true);

        when(ernieKeyring.isUnlocked())
                .thenReturn(true);

        when(keyringCache.get(userErnie))
                .thenReturn(ernieKeyring);

        when(sessionsService.find(ERNIE_EMAIL))
                .thenReturn(ernieSession);

        when(users.getUserByEmail(ERNIE_EMAIL))
                .thenReturn(storedUserErnier);



        when(collection.getDescription())
                .thenReturn(collectionDescription);

        when(collectionDescription.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissions.getCollectionAccessMapping(collection))
                .thenReturn(accessMapping);

        legacyKeyring = new LegacyKeyringImpl(sessionsService, users, permissions, keyringCache, applicationKeys);
    }


    @Test
    public void testGet_userNull_shouldThrowException() {
        // Given user is null

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(null, null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testGet_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(userBert.getEmail())
                .thenReturn(null);

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(userBert, null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testGet_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(userBert.getEmail())
                .thenReturn("");

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(userBert, null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testGet_collectionNull_shouldThrowException() {
        // Given collection is null

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(userBert, null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void testGet_collectionDescriptionNull_shouldThrowException() {
        // Given collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(userBert, collection));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));
    }

    @Test
    public void testGet_collectionIDNull_shouldThrowException() {
        // Given collection ID is null
        when(collectionDescription.getId())
                .thenReturn(null);

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(userBert, collection));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testGet_collectionIDEmpty_shouldThrowException() {
        // Given collection ID is empty
        when(collectionDescription.getId())
                .thenReturn("");

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(userBert, collection));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testGet_keyringCacheError_shouldThrowException() throws Exception {
        // Given keyring cache throws an exception
        when(keyringCache.get(userBert))
                .thenThrow(IOException.class);

        // When get is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.get(userBert, collection));

        // Then null is returned
        assertThat(actual.getMessage(), equalTo(CACHE_GET_ERR));
        verify(keyringCache, times(1)).get(userBert);
    }

    @Test
    public void testGet_keyringCacheReturnsNull_shouldReturnNull() throws Exception {
        // Given keyring cache throws an exception
        when(keyringCache.get(userBert))
                .thenReturn(null);

        // When get is called
        SecretKey actual = legacyKeyring.get(userBert, collection);

        // Then null is returned
        assertThat(actual, is(nullValue()));
        verify(keyringCache, times(1)).get(userBert);
    }

    @Test
    public void testGet_userKeyringIsLocked_shouldReturnNull() throws Exception {
        // Given the user keyring is locked
        when(bertsKeyring.isUnlocked())
                .thenReturn(false);

        // When get is called
        SecretKey actual = legacyKeyring.get(userBert, collection);

        // Then null is returned
        assertThat(actual, is(nullValue()));
        verify(keyringCache, times(1)).get(userBert);
        verify(bertsKeyring, times(1)).isUnlocked();
    }

    @Test
    public void testGet_keyNotInUserKeyring_shouldReturnNull() throws Exception {
        // Given the user keyring does not contain the requested collection key
        when(bertsKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(null);

        // When get is called
        SecretKey actual = legacyKeyring.get(userBert, collection);

        // Then null is returned
        assertThat(actual, is(nullValue()));
        verify(keyringCache, times(1)).get(userBert);
        verify(bertsKeyring, times(1)).isUnlocked();
        verify(bertsKeyring, times(1)).get(TEST_COLLECTION_ID);
    }

    @Test
    public void testGet_success_shouldReturnKey() throws Exception {
        // Given an unlocked user keyring exists in the cache
        // and contains the requested key
        when(bertsKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(secretKey);

        // When get is called
        SecretKey actual = legacyKeyring.get(userBert, collection);

        // Then the expected key is returned
        assertThat(actual, equalTo(secretKey));
        verify(keyringCache, times(1)).get(userBert);
        verify(bertsKeyring, times(1)).isUnlocked();
        verify(bertsKeyring, times(1)).get(TEST_COLLECTION_ID);
    }

    @Test
    public void testCacheKeyring_userNull_shouldThrowException() {
        // Given user is null

        // When cacheKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.cacheKeyring(null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testCacheKeyring_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(userBert.getEmail())
                .thenReturn(null);

        // When cacheKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.cacheKeyring(userBert));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testCacheKeyring_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(userBert.getEmail())
                .thenReturn("");

        // When cacheKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.cacheKeyring(userBert));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testCacheKeyring_userKeyringNull_shouldThrowException() {
        // Given user keyring is null
        when(userBert.keyring())
                .thenReturn(null);

        // When cacheKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.cacheKeyring(userBert));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
    }

    @Test
    public void testCacheKeyring_getSessionError_shouldThrowException() throws IOException {
        // Given get session throws an exception
        when(sessionsService.find(BERT_EMAIL))
                .thenThrow(expectedEx);

        // When cacheKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.cacheKeyring(userBert));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(GET_SESSION_ERR));
        assertThat(ex.getCause(), equalTo(expectedEx));
    }

    @Test
    public void testCacheKeyring_getSessionReturnsNull_doNothing() throws IOException {
        // Given get session returns null
        when(sessionsService.find(BERT_EMAIL))
                .thenReturn(null);

        // When cacheKeyring is called
        legacyKeyring.cacheKeyring(userBert);

        // Then no keying is added to the cache
        verify(sessionsService, times(1)).find(BERT_EMAIL);
        verifyZeroInteractions(keyringCache);

        // And then applicationKeys is updated
        verify(applicationKeys, times(1)).populateCacheFromUserKeyring(bertsKeyring);
    }

    @Test
    public void testCacheKeyring_cachePutError_shouldThrowException() throws IOException {
        // Given cache put throws an exception
        doThrow(expectedEx)
                .when(keyringCache)
                .put(userBert, bertSession);

        // When cacheKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.cacheKeyring(userBert));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(CACHE_PUT_ERR));
        assertThat(ex.getCause(), equalTo(expectedEx));
    }

    @Test
    public void testCacheKeyring_success_shouldPopulateCacheAndAppKeys() throws IOException {
        // Given a valid user

        // Whenc cacheKeyring is called
        legacyKeyring.cacheKeyring(userBert);

        // Then the keyring cache is updated with the users keys
        verify(sessionsService, times(1)).find(BERT_EMAIL);
        verify(keyringCache, times(1)).put(userBert, bertSession);

        // And the applicate keys are updated from the user keyring
        verify(applicationKeys, times(1)).populateCacheFromUserKeyring(bertsKeyring);
    }

    @Test
    public void testRemove_userNull_shouldThrowException() {
        // Given user is null

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(userBert.getEmail())
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(userBert, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(userBert.getEmail())
                .thenReturn("");

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(userBert, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_collectionNull_shouldThrowException() {
        // Given collection is null

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(userBert, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_collectionDescriptionNull_shouldThrowException() {
        // Given collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(userBert, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_collectionIDNull_shouldThrowException() {
        // Given collection ID is null
        when(collectionDescription.getId())
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(userBert, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_collectionIDEmpty_shouldThrowException() {
        // Given collection ID is empty
        when(collectionDescription.getId())
                .thenReturn("");

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(userBert, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_userKeyringNotInCache_shouldStillUpdateAndSaveUser() throws Exception {
        // Given the keyring cache does not contain the users keyring
        when(keyringCache.get(userBert))
                .thenReturn(null);

        // When remove is called
        legacyKeyring.remove(userBert, collection);

        // Then the user record is updated and saved
        verify(users, times(1)).removeKeyFromKeyring(BERT_EMAIL, TEST_COLLECTION_ID);

        // And no update is made the cached user keyring
        verify(keyringCache, times(1)).get(userBert);
        verifyZeroInteractions(bertsKeyring);

        // And
    }

    @Test
    public void testRemove_userKeyringNotInCacheSaveUserError_shouldThrowException() throws Exception {
        // Given the keyring cache does not contain the users keyring
        when(keyringCache.get(userBert))
                .thenReturn(null);

        IOException cause = new IOException("save user error");
        when(users.removeKeyFromKeyring(BERT_EMAIL, TEST_COLLECTION_ID))
                .thenThrow(cause);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.remove(userBert, collection));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(REMOVE_KEY_SAVE_ERR));
        assertThat(actual.getCause(), equalTo(cause));

        verify(keyringCache, times(1)).get(userBert);
        verify(users, times(1)).removeKeyFromKeyring(BERT_EMAIL, TEST_COLLECTION_ID);
    }

    @Test
    public void testRemove_success_shouldRemoveKeyFromCacheKeyringAndSaveUser() throws Exception {
        // Given the users cached keyring is unlocked

        // When remove is called
        legacyKeyring.remove(userBert, collection);

        // Then the key is removed from cached keyring
        verify(keyringCache, times(1)).get(userBert);
        verify(bertsKeyring, times(1)).remove(TEST_COLLECTION_ID);

        // And the user is updated and saved
        verify(users, times(1)).removeKeyFromKeyring(BERT_EMAIL, TEST_COLLECTION_ID);
    }

    @Test
    public void testRemove_saveChangesToUserError_shouldThrowException() throws Exception {
        // Given removing the key from the user and saving the change throws an exception
        IOException cause = new IOException("save user error");

        when(users.removeKeyFromKeyring(BERT_EMAIL, TEST_COLLECTION_ID))
                .thenThrow(cause);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.remove(userBert, collection));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(REMOVE_KEY_SAVE_ERR));
        assertThat(actual.getCause(), equalTo(cause));

        verify(keyringCache, times(1)).get(userBert);
        verify(bertsKeyring, times(1)).remove(TEST_COLLECTION_ID);
        verify(users, times(1)).removeKeyFromKeyring(BERT_EMAIL, TEST_COLLECTION_ID);
    }

    @Test
    public void testAdd_userNull_shouldThrowException() {
        // Given user is null

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(null, null, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(keyringCache, users, bertsKeyring, bertsKeyring);
    }

    @Test
    public void testAdd_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(userBert.getEmail())
                .thenReturn(null);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(userBert, null, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users, bertsKeyring, bertsKeyring);
    }

    @Test
    public void testAdd_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(userBert.getEmail())
                .thenReturn("");

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(userBert, null, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users, bertsKeyring, bertsKeyring);
    }

    @Test
    public void testAdd_collectionNull_shouldThrowException() {
        // Given collection is null

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(userBert, null, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(COLLECTION_NULL_ERR));
        verifyZeroInteractions(keyringCache, users, bertsKeyring, bertsKeyring);
    }

    @Test
    public void testAdd_collectionDescriptionNull_shouldThrowException() {
        // Given collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(userBert, collection, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));
        verifyZeroInteractions(keyringCache, users, bertsKeyring, bertsKeyring);
    }

    @Test
    public void testAdd_collectionDescriptionIDNull_shouldThrowException() {
        // Given collection ID is null
        when(collectionDescription.getId())
                .thenReturn(null);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(userBert, collection, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users, bertsKeyring, bertsKeyring);
    }

    @Test
    public void testAdd_collectionDescriptionIDEmpty_shouldThrowException() {
        // Given collection ID is empty
        when(collectionDescription.getId())
                .thenReturn("");

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(userBert, collection, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users, bertsKeyring, bertsKeyring);
    }

    @Test
    public void testAdd_keyNull_shouldThrowException() {
        // Given key is null

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(userBert, collection, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(SECRET_KEY_NULL_ERR));
        verifyZeroInteractions(keyringCache, users, bertsKeyring, bertsKeyring);
    }

    @Test
    public void testAdd_cacheKeyringNotFound_shouldUpdatedStoredUserOnly() throws Exception {
        // Given a cached keyring does not exist for the user
        when(keyringCache.get(userBert))
                .thenReturn(null);

        // When add is called
        legacyKeyring.add(userBert, collection, secretKey);

        // Then the stored user is updated
        verify(keyringCache, times(1)).get(userBert);
        verify(users, times(1)).addKeyToKeyring(BERT_EMAIL, TEST_COLLECTION_ID, secretKey);

        // And the cached user keyring is not updated
        verifyZeroInteractions(bertsKeyring);
    }

    @Test
    public void testAdd_cacheKeyringIsLocked_shouldUpdateBothStoredUserAndCachedKeyring() throws Exception {
        // Given the users cached keyring is locked
        when(bertsKeyring.isUnlocked())
                .thenReturn(false);

        // When add is called
        legacyKeyring.add(userBert, collection, secretKey);

        // Then the keyring cache is updated
        verify(keyringCache, times(1)).get(userBert);
        verify(keyringCache, times(1)).get(userErnie);

        verify(bertsKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);
        verify(ernieKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);

        // And the key is added to the store user keyring
        verify(users, times(1)).addKeyToKeyring(BERT_EMAIL, TEST_COLLECTION_ID, secretKey);
        verify(users, times(1)).addKeyToKeyring(ERNIE_EMAIL, TEST_COLLECTION_ID, secretKey);
    }

    @Test
    public void testAdd_updateAndSaveUserError_shouldThrowException() throws Exception {
        // Given users add key to keyring throws an exception
        when(users.addKeyToKeyring(BERT_EMAIL, TEST_COLLECTION_ID, secretKey))
                .thenThrow(IOException.class);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class,
                () -> legacyKeyring.add(userBert, collection, secretKey));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(ADD_KEY_SAVE_ERR));
        assertTrue(actual.getCause() instanceof IOException);

        verify(keyringCache, times(1)).get(userBert);
        verify(bertsKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);
        verify(users, times(1)).addKeyToKeyring(BERT_EMAIL, TEST_COLLECTION_ID, secretKey);
    }

    @Test
    public void testAdd_collectionAccessMappingError_shouldThrowException() throws Exception {
        // Given permissions get collection access mapping throws an exception
        when(permissions.getCollectionAccessMapping(collection))
                .thenThrow(IOException.class);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class,
                () -> legacyKeyring.add(userBert, collection, secretKey));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(GET_ACCESS_MAPPING_ERR));
        assertTrue(actual.getCause() instanceof IOException);

        verify(keyringCache, times(1)).get(userBert);
        verify(bertsKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);
        verify(users, times(1)).addKeyToKeyring(BERT_EMAIL, TEST_COLLECTION_ID, secretKey);
        verify(permissions, times(1)).getCollectionAccessMapping(collection);
    }

    @Test
    public void testAdd_collectionAccessMappingNull_shouldNotDistrubuteKey() throws Exception {
        // Given permissions get collection access mapping returns null
        when(permissions.getCollectionAccessMapping(collection))
                .thenReturn(null);

        // When add is called
        legacyKeyring.add(userBert, collection, secretKey);

        // Then the key is not assigned to any other users
        verify(keyringCache, times(1)).get(userBert);
        verify(bertsKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);
        verify(users, times(1)).addKeyToKeyring(BERT_EMAIL, TEST_COLLECTION_ID, secretKey);
        verify(permissions, times(1)).getCollectionAccessMapping(collection);
        verifyNoMoreInteractions(keyringCache, users);
    }

    @Test
    public void testAdd_collectionAccessMappingEmpty_shouldNotDistrubuteKey() throws Exception {
        // Given permissions get collection access mapping returns an empty list
        when(permissions.getCollectionAccessMapping(collection))
                .thenReturn(new ArrayList<>());

        // When add is called
        legacyKeyring.add(userBert, collection, secretKey);

        // Then the key is not assigned to any other users
        verify(keyringCache, times(1)).get(userBert);
        verify(bertsKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);
        verify(users, times(1)).addKeyToKeyring(BERT_EMAIL, TEST_COLLECTION_ID, secretKey);
        verify(permissions, times(1)).getCollectionAccessMapping(collection);
        verifyNoMoreInteractions(keyringCache, users);
    }

    @Test
    public void testAdd_addKeyToUserFailsWhenDistributingKey_shouldThrowException() throws Exception {
        // Given user addKeyToKeyring throws an exception when distributing the key to other users
        when(users.addKeyToKeyring(ERNIE_EMAIL, TEST_COLLECTION_ID, secretKey))
                .thenThrow(IOException.class);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class,
                () -> legacyKeyring.add(userBert, collection, secretKey));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(ADD_KEY_SAVE_ERR));
        assertTrue(actual.getCause() instanceof IOException);


        verify(keyringCache, times(1)).get(userBert);
        verify(keyringCache, times(1)).get(userErnie);

        verify(bertsKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);
        verify(ernieKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);

        verify(users, times(1)).addKeyToKeyring(BERT_EMAIL, TEST_COLLECTION_ID, secretKey);
        verify(users, times(1)).addKeyToKeyring(ERNIE_EMAIL, TEST_COLLECTION_ID, secretKey);

        verify(permissions, times(1)).getCollectionAccessMapping(collection);
        verifyNoMoreInteractions(keyringCache, users, bertsKeyring, ernieKeyring);
    }

    @Test
    public void testList_userNull_shouldThrowException() {
        // Given user is null

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.list(null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testList_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(userBert.getEmail())
                .thenReturn(null);

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.list(userBert));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testList_userEmailEmpty_shouldThrowException() {
        // Given user email is null
        when(userBert.getEmail())
                .thenReturn("");

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.list(userBert));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testList_cacheKeyringExists_shouldListCachedKeyringEntries() throws Exception {
        // Given the users keyring exists in the cache
        Set<String> expected = new HashSet<String>() {{
            add(TEST_COLLECTION_ID);
        }};

        when(bertsKeyring.list())
                .thenReturn(expected);

        // When list is called
        Set<String> actual = legacyKeyring.list(userBert);

        // Then the expected set is returned
        assertThat(actual, equalTo(expected));
        verify(keyringCache, times(1)).get(userBert);
        verify(bertsKeyring, times(1)).list();
        verifyZeroInteractions(users);
    }

    @Test
    public void testList_cacheKeyringDoesNotExistUsersGetError_shouldThrowException() throws Exception {
        // Given the users keyring does not exists in the cache
        // And get user throws an exception
        when(keyringCache.get(userBert))
                .thenReturn(null);

        when(users.getUserByEmail(BERT_EMAIL))
                .thenThrow(IOException.class);

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.list(userBert));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(GET_USER_ERR));
        verify(keyringCache, times(1)).get(userBert);
        verify(users, times(1)).getUserByEmail(BERT_EMAIL);
        verifyZeroInteractions(bertsKeyring);
    }

    @Test
    public void testList_getUserReturnsNull_shouldThrowException() throws Exception {
        // Given the users get user returns null
        when(keyringCache.get(userBert))
                .thenReturn(null);

        when(users.getUserByEmail(BERT_EMAIL))
                .thenReturn(null);

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.list(userBert));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(USER_NULL_ERR));
        verify(keyringCache, times(1)).get(userBert);
        verify(users, times(1)).getUserByEmail(BERT_EMAIL);
        verifyZeroInteractions(bertsKeyring);
    }

    @Test
    public void testList_storedUserKeyringNull_shouldReturnEmptyResult() throws Exception {
        // Given the stored users keyring is null
        when(keyringCache.get(userBert))
                .thenReturn(null);

        User storedUser = mock(User.class);
        when(users.getUserByEmail(BERT_EMAIL))
                .thenReturn(storedUser);

        when(storedUser.keyring())
                .thenReturn(null);

        // When list is called
        Set<String> actual = legacyKeyring.list(userBert);

        // Then an empty set is returned
        assertTrue(actual.isEmpty());
        verify(keyringCache, times(1)).get(userBert);
        verify(users, times(1)).getUserByEmail(BERT_EMAIL);
        verifyZeroInteractions(bertsKeyring);
    }

    @Test
    public void testList_storedUserSuccess_shouldReturnKeys() throws Exception {
        // Given the stored users keyring is not null
        when(keyringCache.get(userBert))
                .thenReturn(null);

        when(users.getUserByEmail(BERT_EMAIL))
                .thenReturn(storedUserBert);

        when(storedUserBert.keyring())
                .thenReturn(bertStoredKeyring);

        Set<String> expected = new HashSet<String>() {{
            add(TEST_COLLECTION_ID);
        }};

        when(bertStoredKeyring.list())
                .thenReturn(expected);

        // When list is called
        Set<String> actual = legacyKeyring.list(userBert);

        // Then the expected result is returned
        assertThat(actual, equalTo(expected));
        verify(keyringCache, times(1)).get(userBert);
        verify(users, times(1)).getUserByEmail(BERT_EMAIL);
        verify(bertStoredKeyring, times(1)).list();
    }

    @Test
    public void testUnlock_userNull_shouldThrowException() {
        // Given user is null

        // When unlock is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.unlock(null, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testUnlock_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(userBert.getEmail())
                .thenReturn(null);

        // When unlock is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.unlock(userBert, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testUnlock_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(userBert.getEmail())
                .thenReturn("");

        // When unlock is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.unlock(userBert, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testUnlock_userKeyringNull_shouldThrowException() {
        // Given user keyring is null
        when(userBert.keyring())
                .thenReturn(null);

        // When unlock is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.unlock(userBert, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
    }

    @Test
    public void testUnlock_passwordNull_shouldThrowException() {
        // Given password is null

        // When unlock is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.unlock(userBert, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(PASSWORD_EMPTY_ERR));
        verifyZeroInteractions(bertsKeyring);
    }

    @Test
    public void testUnlock_unlockFailed_shouldThrowException() {
        // Given unlocking the user keying is unsuccessful
        when(bertsKeyring.unlock(TEST_PASSWORD))
                .thenReturn(false);

        // When unlock is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.unlock(userBert, TEST_PASSWORD));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(UNLOCK_KEYRING_ERR));
        verify(bertsKeyring, times(1)).unlock(TEST_PASSWORD);
    }

    @Test
    public void testUnlock_success_shouldUnlockTheUserKeyring() throws Exception {
        // Given unlocking the keyring is successful

        // When unlock is called
        legacyKeyring.unlock(userBert, TEST_PASSWORD);

        // Then no error is returned
        // And the user keyring is unlocked
        verify(bertsKeyring, times(1)).unlock(TEST_PASSWORD);
    }


}
