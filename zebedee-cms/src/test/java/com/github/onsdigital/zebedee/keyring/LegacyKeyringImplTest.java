package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.io.IOException;

import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.CACHE_GET_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.CACHE_PUT_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_DESC_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_ID_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.EMAIL_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.GET_SESSION_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.USER_KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.USER_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
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
    private com.github.onsdigital.zebedee.json.Keyring userKeyring;

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

        when(keyringCache.get(user))
                .thenReturn(userKeyring);

        when(sessionsService.find(TEST_EMAIL))
                .thenReturn(session);

        when(collection.getDescription())
                .thenReturn(collectionDescription);

        when(collectionDescription.getId())
                .thenReturn(TEST_COLLECTION_ID);

        legacyKeyring = new LegacyKeyringImpl(sessionsService, keyringCache, applicationKeys);
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
    public void testGet_collecitonNull_shouldThrowException() {
        // Given collection is null

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(user, null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void testGet_collecitonDescriptionNull_shouldThrowException() {
        // Given collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(user, collection));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));
    }

    @Test
    public void testGet_collecitonIDNull_shouldThrowException() {
        // Given collection ID is null
        when(collectionDescription.getId())
                .thenReturn(null);

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.get(user, collection));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testGet_collecitonIDEmpty_shouldThrowException() {
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
        when(userKeyring.isUnlocked())
                .thenReturn(false);

        // When get is called
        SecretKey actual = legacyKeyring.get(user, collection);

        // Then null is returned
        assertThat(actual, is(nullValue()));
        verify(keyringCache, times(1)).get(user);
        verify(userKeyring, times(1)).isUnlocked();
    }

    @Test
    public void testGet_keyNotInUserKeyring_shouldReturnNull() throws Exception {
        // Given the user keyring does not contain the requested collection key
        when(userKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(null);

        // When get is called
        SecretKey actual = legacyKeyring.get(user, collection);

        // Then null is returned
        assertThat(actual, is(nullValue()));
        verify(keyringCache, times(1)).get(user);
        verify(userKeyring, times(1)).isUnlocked();
        verify(userKeyring, times(1)).get(TEST_COLLECTION_ID);
    }

    @Test
    public void testGet_success_shouldReturnKey() throws Exception {
        // Given an unlocked user keyring exists in the cache
        // and contains the requested key
        when(userKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(secretKey);

        // When get is called
        SecretKey actual = legacyKeyring.get(user, collection);

        // Then the expected key is returned
        assertThat(actual, equalTo(secretKey));
        verify(keyringCache, times(1)).get(user);
        verify(userKeyring, times(1)).isUnlocked();
        verify(userKeyring, times(1)).get(TEST_COLLECTION_ID);
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

/*
    @Test
    public void testRemove_userNull_shouldThrowException() {
        // Given user is null

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testRemove_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(user.getEmail())
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testRemove_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(user.getEmail())
                .thenReturn("");

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testRemove_userKeyringNull_shouldThrowException() {
        // Given user keyring is null
        when(user.keyring())
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
    }

    @Test
    public void testRemove_userKeyringLocked_shouldThrowException() {
        // Given user keyring is locked
        when(userKeyring.isUnlocked())
                .thenReturn(false);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_LOCKED_ERR));
    }

    @Test
    public void testRemove_collectionNull_shouldThrowException() {
        // Given collection is null

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void testRemove_collectionDescriptionNull_shouldThrowException() {
        // Given collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));
    }

    @Test
    public void testRemove_collectionIDNull_shouldThrowException() {
        // Given collection ID is null
        when(collectionDescription.getId())
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testRemove_collectionIDEmpty_shouldThrowException() {
        // Given collection ID is empty
        when(collectionDescription.getId())
                .thenReturn("");

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testRemove_success_shouldCallKeyringRemove() throws Exception {
        // Given a valid user and colleciton

        // When remove is called
        keyring.remove(user, collection);

        // Then user keyring.remove is called with the expected parameters
        verify(user, times(3)).keyring();
        verify(userKeyring, times(1)).remove(TEST_COLLECTION_ID);
    }

    @Test
    public void testAdd_userNull_shouldThrowException() {
        // Given user is null

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, ()-> keyring.add(null, null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testAdd_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(user.getEmail())
                .thenReturn(null);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, ()-> keyring.add(user, null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testAdd_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(user.getEmail())
                .thenReturn("");

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, ()-> keyring.add(user, null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testAdd_userKeyringNull_shouldThrowException() {
        // Given user keyring is null
        when(user.keyring())
                .thenReturn(null);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, ()-> keyring.add(user, null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
    }

    @Test
    public void testAdd_userKeyringLocked_shouldThrowException() {
        // Given user keyring is locked
        when(userKeyring.isUnlocked())
                .thenReturn(false);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, ()-> keyring.add(user, null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_LOCKED_ERR));
    }

    @Test
    public void testAdd_collectionNull_shouldThrowException() {
        // Given collection is null

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, ()-> keyring.add(user, null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void testAdd_collectionDescriptionNull_shouldThrowException() {
        // Given collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, ()-> keyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));
    }

    @Test
    public void testAdd_collectionIDNull_shouldThrowException() {
        // Given collection ID is null
        when(collectionDescription.getId())
                .thenReturn(null);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, ()-> keyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testAdd_collectionIDEmpty_shouldThrowException() {
        // Given collection ID is empty
        when(collectionDescription.getId())
                .thenReturn("");

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, ()-> keyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testAdd_secretKeyNull_shouldThrowException() {
        // Given secret key is null

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, ()-> keyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SECRET_KEY_NULL_ERR));
    }

    @Test
    public void testAdd_success_shouldAddKeyToUserKeyring() throws Exception {
        // Given a valid user, collection and secret key

        // When add is called
        keyring.add(user, collection, secretKey);

        // Then the key is added to the user keyring.
        verify(user, times(3)).keyring();
        verify(userKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);
    }

    @Test
    public void testList_userNull_shouldThrowException() {
        // Given user is null

        // When list is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.list(null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testList_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(user.getEmail())
                .thenReturn(null);

        // When list is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.list(user));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testList_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(user.getEmail())
                .thenReturn("");

        // When list is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.list(user));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testList_userKeyringNull_shouldThrowException() {
        // Given user keyring is null
        when(user.keyring())
                .thenReturn(null);

        // When list is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.list(user));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
    }

    @Test
    public void testList_userKeyringLocked_shouldThrowException() {
        // Given user keyring is locked
        when(userKeyring.isUnlocked())
                .thenReturn(false);

        // When list is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.list(user));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_LOCKED_ERR));
    }

    @Test
    public void testList_success_shouldListKeys() throws KeyringException {
        // Given a valid user
        Set<String> expectedKeys = new HashSet<String>() {{
            add("1234");
            add("5679");
        }};

        when(userKeyring.list())
                .thenReturn(expectedKeys);

        // When list is called
        Set<String> actual = keyring.list(user);

        // Then the expected values are returned
        assertThat(actual, equalTo(expectedKeys));
        verify(user, times(3)).keyring();
        verify(userKeyring, times(1)).list();
    }
}*/

}
