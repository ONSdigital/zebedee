package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
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
import java.util.HashSet;
import java.util.Set;

import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.ADD_KEY_SAVE_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.CACHE_GET_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.CACHE_PUT_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_DESC_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_ID_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.EMAIL_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.GET_SESSION_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.GET_USER_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.REMOVE_KEY_SAVE_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.SECRET_KEY_NULL_ERR;
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
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LegacyKeyringImplTest {

    static final String TEST_EMAIL = "bertandernie@sesamestreet.com";

    static final String TEST_COLLECTION_ID = "666";

    @Mock
    private User user;

    @Mock
    private Session session;

    @Mock
    private UsersService users;

    @Mock
    private com.github.onsdigital.zebedee.json.Keyring userKeyring;

    @Mock
    private com.github.onsdigital.zebedee.json.Keyring cachedUserKeyring;

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

    @Before

    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        expectedEx = new KeyringException("bork");

        when(user.getEmail())
                .thenReturn(TEST_EMAIL);

        when(user.keyring())
                .thenReturn(userKeyring);

        when(userKeyring.isUnlocked())
                .thenReturn(true);

        when(cachedUserKeyring.isUnlocked())
                .thenReturn(true);

        when(keyringCache.get(user))
                .thenReturn(cachedUserKeyring);

        when(sessionsService.find(TEST_EMAIL))
                .thenReturn(session);

        when(collection.getDescription())
                .thenReturn(collectionDescription);

        when(collectionDescription.getId())
                .thenReturn(TEST_COLLECTION_ID);

        legacyKeyring = new LegacyKeyringImpl(sessionsService, users, keyringCache, applicationKeys);
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
        when(user.getEmail())
                .thenReturn(null);

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(user, null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testGet_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(user.getEmail())
                .thenReturn("");

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(user, null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testGet_collectionNull_shouldThrowException() {
        // Given collection is null

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(user, null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void testGet_collectionDescriptionNull_shouldThrowException() {
        // Given collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(user, collection));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));
    }

    @Test
    public void testGet_collectionIDNull_shouldThrowException() {
        // Given collection ID is null
        when(collectionDescription.getId())
                .thenReturn(null);

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(user, collection));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testGet_collectionIDEmpty_shouldThrowException() {
        // Given collection ID is empty
        when(collectionDescription.getId())
                .thenReturn("");

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(user, collection));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testGet_keyringCacheError_shouldThrowException() throws Exception {
        // Given keyring cache throws an exception
        when(keyringCache.get(user))
                .thenThrow(IOException.class);

        // When get is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.get(user, collection));

        // Then null is returned
        assertThat(actual.getMessage(), equalTo(CACHE_GET_ERR));
        verify(keyringCache, times(1)).get(user);
    }

    @Test
    public void testGet_keyringCacheReturnsNull_shouldReturnNull() throws Exception {
        // Given keyring cache throws an exception
        when(keyringCache.get(user))
                .thenReturn(null);

        // When get is called
        SecretKey actual = legacyKeyring.get(user, collection);

        // Then null is returned
        assertThat(actual, is(nullValue()));
        verify(keyringCache, times(1)).get(user);
    }

    @Test
    public void testGet_userKeyringIsLocked_shouldReturnNull() throws Exception {
        // Given the user keyring is locked
        when(cachedUserKeyring.isUnlocked())
                .thenReturn(false);

        // When get is called
        SecretKey actual = legacyKeyring.get(user, collection);

        // Then null is returned
        assertThat(actual, is(nullValue()));
        verify(keyringCache, times(1)).get(user);
        verify(cachedUserKeyring, times(1)).isUnlocked();
    }

    @Test
    public void testGet_keyNotInUserKeyring_shouldReturnNull() throws Exception {
        // Given the user keyring does not contain the requested collection key
        when(cachedUserKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(null);

        // When get is called
        SecretKey actual = legacyKeyring.get(user, collection);

        // Then null is returned
        assertThat(actual, is(nullValue()));
        verify(keyringCache, times(1)).get(user);
        verify(cachedUserKeyring, times(1)).isUnlocked();
        verify(cachedUserKeyring, times(1)).get(TEST_COLLECTION_ID);
    }

    @Test
    public void testGet_success_shouldReturnKey() throws Exception {
        // Given an unlocked user keyring exists in the cache
        // and contains the requested key
        when(cachedUserKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(secretKey);

        // When get is called
        SecretKey actual = legacyKeyring.get(user, collection);

        // Then the expected key is returned
        assertThat(actual, equalTo(secretKey));
        verify(keyringCache, times(1)).get(user);
        verify(cachedUserKeyring, times(1)).isUnlocked();
        verify(cachedUserKeyring, times(1)).get(TEST_COLLECTION_ID);
    }

    @Test
    public void testCacheUserKeyring_userNull_shouldThrowException() {
        // Given user is null

        // When cache user keyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.cacheUserKeyring(null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testCacheUserKeyring_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(user.getEmail())
                .thenReturn(null);

        // When cache user keyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.cacheUserKeyring(user));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testCacheUserKeyring_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(user.getEmail())
                .thenReturn("");

        // When cacheUserKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.cacheUserKeyring(user));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testCacheUserKeyring_userKeyringNull_shouldThrowException() {
        // Given user keyring is null
        when(user.keyring())
                .thenReturn(null);

        // When cacheUserKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.cacheUserKeyring(user));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
    }

    @Test
    public void testCacheUserKeyring_getSessionError_shouldThrowException() throws IOException {
        // Given get session throws an exception
        when(sessionsService.find(TEST_EMAIL))
                .thenThrow(expectedEx);

        // When cacheUserKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.cacheUserKeyring(user));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(GET_SESSION_ERR));
        assertThat(ex.getCause(), equalTo(expectedEx));
    }

    @Test
    public void testCacheUserKeyring_getSessionReturnsNull_doNothing() throws IOException {
        // Given get session returns null
        when(sessionsService.find(TEST_EMAIL))
                .thenReturn(null);

        // When cacheUserKeyring is called
        legacyKeyring.cacheUserKeyring(user);

        // Then no keying is added to the cache
        verify(sessionsService, times(1)).find(TEST_EMAIL);
        verifyZeroInteractions(keyringCache);

        // And then applicationKeys is updated
        verify(applicationKeys, times(1)).populateCacheFromUserKeyring(userKeyring);
    }

    @Test
    public void testCacheUserKeyring_cachePutError_shouldThrowException() throws IOException {
        // Given cache put throws an exception
        doThrow(expectedEx)
                .when(keyringCache)
                .put(user, session);

        // When cacheUserKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.cacheUserKeyring(user));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(CACHE_PUT_ERR));
        assertThat(ex.getCause(), equalTo(expectedEx));
    }

    @Test
    public void testCacheUserKeyring_success_shouldPopulateCacheAndAppKeys() throws IOException {
        // Given a valid user

        // WhencacheUserKeyring is called
        legacyKeyring.cacheUserKeyring(user);

        // Then the keyring cache is updated with the users keys
        verify(sessionsService, times(1)).find(TEST_EMAIL);
        verify(keyringCache, times(1)).put(user, session);

        // And the applicate keys are updated from the user keyring
        verify(applicationKeys, times(1)).populateCacheFromUserKeyring(userKeyring);
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
        when(user.getEmail())
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(user, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(user.getEmail())
                .thenReturn("");

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(user, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_collectionNull_shouldThrowException() {
        // Given collection is null

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(user, null));

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
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(user, collection));

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
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(user, collection));

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
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(user, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_userKeyringNotInCache_shouldStillUpdateAndSaveUser() throws Exception {
        // Given the keyring cache does not contain the users keyring
        when(keyringCache.get(user))
                .thenReturn(null);

        // When remove is called
        legacyKeyring.remove(user, collection);

        // Then the user record is updated and saved
        verify(users, times(1)).removeKeyFromKeyring(TEST_EMAIL, TEST_COLLECTION_ID);

        // And no update is made the cached user keyring
        verify(keyringCache, times(1)).get(user);
        verifyZeroInteractions(cachedUserKeyring);

        // And
    }

    @Test
    public void testRemove_userKeyringNotInCacheSaveUserError_shouldThrowException() throws Exception {
        // Given the keyring cache does not contain the users keyring
        when(keyringCache.get(user))
                .thenReturn(null);

        IOException cause = new IOException("save user error");
        when(users.removeKeyFromKeyring(TEST_EMAIL, TEST_COLLECTION_ID))
                .thenThrow(cause);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.remove(user, collection));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(REMOVE_KEY_SAVE_ERR));
        assertThat(actual.getCause(), equalTo(cause));

        verify(keyringCache, times(1)).get(user);
        verify(users, times(1)).removeKeyFromKeyring(TEST_EMAIL, TEST_COLLECTION_ID);
    }

    @Test
    public void testRemove_success_shouldRemoveKeyFromCacheKeyringAndSaveUser() throws Exception {
        // Given the users cached keyring is unlocked

        // When remove is called
        legacyKeyring.remove(user, collection);

        // Then the key is removed from cached keyring
        verify(keyringCache, times(1)).get(user);
        verify(cachedUserKeyring, times(1)).remove(TEST_COLLECTION_ID);

        // And the user is updated and saved
        verify(users, times(1)).removeKeyFromKeyring(TEST_EMAIL, TEST_COLLECTION_ID);
    }

    @Test
    public void testRemove_saveChangesToUserError_shouldThrowException() throws Exception {
        // Given removing the key from the user and saving the change throws an exception
        IOException cause = new IOException("save user error");

        when(users.removeKeyFromKeyring(TEST_EMAIL, TEST_COLLECTION_ID))
                .thenThrow(cause);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.remove(user, collection));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(REMOVE_KEY_SAVE_ERR));
        assertThat(actual.getCause(), equalTo(cause));

        verify(keyringCache, times(1)).get(user);
        verify(cachedUserKeyring, times(1)).remove(TEST_COLLECTION_ID);
        verify(users, times(1)).removeKeyFromKeyring(TEST_EMAIL, TEST_COLLECTION_ID);
    }

    @Test
    public void testAdd_userNull_shouldThrowException() {
        // Given user is null

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(null, null, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(keyringCache, users, userKeyring, cachedUserKeyring);
    }

    @Test
    public void testAdd_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(user.getEmail())
                .thenReturn(null);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(user, null, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users, userKeyring, cachedUserKeyring);
    }

    @Test
    public void testAdd_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(user.getEmail())
                .thenReturn("");

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(user, null, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users, userKeyring, cachedUserKeyring);
    }

    @Test
    public void testAdd_collectionNull_shouldThrowException() {
        // Given collection is null

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(user, null, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(COLLECTION_NULL_ERR));
        verifyZeroInteractions(keyringCache, users, userKeyring, cachedUserKeyring);
    }

    @Test
    public void testAdd_collectionDescriptionNull_shouldThrowException() {
        // Given collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));
        verifyZeroInteractions(keyringCache, users, userKeyring, cachedUserKeyring);
    }

    @Test
    public void testAdd_collectionDescriptionIDNull_shouldThrowException() {
        // Given collection ID is null
        when(collectionDescription.getId())
                .thenReturn(null);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users, userKeyring, cachedUserKeyring);
    }

    @Test
    public void testAdd_collectionDescriptionIDEmpty_shouldThrowException() {
        // Given collection ID is empty
        when(collectionDescription.getId())
                .thenReturn("");

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users, userKeyring, cachedUserKeyring);
    }

    @Test
    public void testAdd_keyNull_shouldThrowException() {
        // Given key is null

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(SECRET_KEY_NULL_ERR));
        verifyZeroInteractions(keyringCache, users, userKeyring, cachedUserKeyring);
    }

    @Test
    public void testAdd_cacheKeyringNotFound_shouldUpdatedStoredUserOnly() throws Exception {
        // Given a cached keyring does not exist for the user
        when(keyringCache.get(user))
                .thenReturn(null);

        // When add is called
        legacyKeyring.add(user, collection, secretKey);

        // Then the stored user is updated
        verify(keyringCache, times(1)).get(user);
        verify(users, times(1)).addKeyToKeyring(TEST_EMAIL, TEST_COLLECTION_ID, secretKey);

        // And the cached user keyring is not updated
        verifyZeroInteractions(userKeyring, cachedUserKeyring);
    }

    @Test
    public void testAdd_cacheKeyringIsLocked_shouldUpdateBothStoredUserAndCachedKeyring() throws Exception {
        // Given the users cached keyring is locked
        when(cachedUserKeyring.isUnlocked())
                .thenReturn(false);

        // When add is called
        legacyKeyring.add(user, collection, secretKey);

        // Then the keyring cache is updated
        verify(keyringCache, times(1)).get(user);
        verify(cachedUserKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);

        // And the key is added to the store user keyring
        verify(users, times(1)).addKeyToKeyring(TEST_EMAIL, TEST_COLLECTION_ID, secretKey);
    }

    @Test
    public void testAdd_updateAndSaveUserError_shouldThrowException() throws Exception {
        // Given users add key to keyring throws an exception
        when(users.addKeyToKeyring(TEST_EMAIL, TEST_COLLECTION_ID, secretKey))
                .thenThrow(IOException.class);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class,
                () -> legacyKeyring.add(user, collection, secretKey));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(ADD_KEY_SAVE_ERR));
        assertTrue(actual.getCause() instanceof IOException);

        verify(keyringCache, times(1)).get(user);
        verify(cachedUserKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);
        verify(users, times(1)).addKeyToKeyring(TEST_EMAIL, TEST_COLLECTION_ID, secretKey);
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
        when(user.getEmail())
                .thenReturn(null);

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.list(user));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testList_userEmailEmpty_shouldThrowException() {
        // Given user email is null
        when(user.getEmail())
                .thenReturn("");

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.list(user));

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

        when(cachedUserKeyring.list())
                .thenReturn(expected);

        // When list is called
        Set<String> actual = legacyKeyring.list(user);

        // Then the expected set is returned
        assertThat(actual, equalTo(expected));
        verify(keyringCache, times(1)).get(user);
        verify(cachedUserKeyring, times(1)).list();
        verifyZeroInteractions(users);
    }

    @Test
    public void testList_cacheKeyringDoesNotExistUsersGetError_shouldThrowException() throws Exception {
        // Given the users keyring does not exists in the cache
        // And get user throws an exception
        when(keyringCache.get(user))
                .thenReturn(null);

        when(users.getUserByEmail(TEST_EMAIL))
                .thenThrow(IOException.class);

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.list(user));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(GET_USER_ERR));
        verify(keyringCache, times(1)).get(user);
        verify(users, times(1)).getUserByEmail(TEST_EMAIL);
        verifyZeroInteractions(cachedUserKeyring);
    }

    @Test
    public void testList_getUserReturnsNull_shouldThrowException() throws Exception {
        // Given the users get user returns null
        when(keyringCache.get(user))
                .thenReturn(null);

        when(users.getUserByEmail(TEST_EMAIL))
                .thenReturn(null);

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.list(user));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(USER_NULL_ERR));
        verify(keyringCache, times(1)).get(user);
        verify(users, times(1)).getUserByEmail(TEST_EMAIL);
        verifyZeroInteractions(cachedUserKeyring);
    }

    @Test
    public void testList_storedUserKeyringNull_shouldReturnEmptyResult() throws Exception {
        // Given the stored users keyring is null
        when(keyringCache.get(user))
                .thenReturn(null);

        User storedUser = mock(User.class);
        when(users.getUserByEmail(TEST_EMAIL))
                .thenReturn(storedUser);

        when(storedUser.keyring())
                .thenReturn(null);

        // When list is called
        Set<String> actual = legacyKeyring.list(user);

        // Then an empty set is returned
        assertTrue(actual.isEmpty());
        verify(keyringCache, times(1)).get(user);
        verify(users, times(1)).getUserByEmail(TEST_EMAIL);
        verifyZeroInteractions(cachedUserKeyring);
    }

    @Test
    public void testList_storedUserSuccess_shouldReturnKeys() throws Exception {
        // Given the stored users keyring is not null
        when(keyringCache.get(user))
                .thenReturn(null);

        User storedUser = mock(User.class);
        when(users.getUserByEmail(TEST_EMAIL))
                .thenReturn(storedUser);

        when(storedUser.keyring())
                .thenReturn(userKeyring);

        Set<String> expected = new HashSet<String>() {{
            add(TEST_COLLECTION_ID);
        }};

        when(userKeyring.list())
                .thenReturn(expected);

        // When list is called
        Set<String> actual = legacyKeyring.list(user);

        // Then the expected result is returned
        assertThat(actual, equalTo(expected));
        verify(keyringCache, times(1)).get(user);
        verify(users, times(1)).getUserByEmail(TEST_EMAIL);
        verifyZeroInteractions(cachedUserKeyring);
    }


}
