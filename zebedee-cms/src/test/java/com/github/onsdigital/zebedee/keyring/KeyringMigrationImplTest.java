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
import java.util.HashSet;
import java.util.Set;

import static com.github.onsdigital.zebedee.keyring.KeyringImpl.COLLECTION_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.USER_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigrationImpl.COLLECTION_DESC_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigrationImpl.COLLECTION_ID_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigrationImpl.EMAIL_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigrationImpl.GET_SESSION_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigrationImpl.LEGACY_CACHE_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigrationImpl.SECRET_KEY_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigrationImpl.SESSION_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigrationImpl.USER_KEYRING_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


public class KeyringMigrationImplTest {

    static final String TEST_COLLECTION_ID = "abc123";
    static final String TEST_EMAIL = "bertandernie@sesamestreet.com";
    final boolean enabled = true;
    final boolean disabled = false;

    private Keyring keyring;

    @Mock
    private User user;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription description;

    @Mock
    private com.github.onsdigital.zebedee.json.Keyring legacyKeyring;

    @Mock
    private SecretKey secretKey;

    @Mock
    private Keyring centralKeyring;

    @Mock
    private KeyringCache legacyKeyringCache;

    @Mock
    private ApplicationKeys applicationKeys;

    @Mock
    private Sessions sessionsService;

    @Mock
    private Session session;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(collection.getDescription())
                .thenReturn(description);

        when(description.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(user.keyring())
                .thenReturn(legacyKeyring);

        when(user.getEmail())
                .thenReturn(TEST_EMAIL);

        when(sessionsService.find(TEST_EMAIL))
                .thenReturn(session);

        this.keyring = new KeyringMigrationImpl(disabled, centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);
    }

    @Test
    public void testGet_userNull_throwsException() throws Exception {
        // Given the user is null

        // When get is called
        KeyringException keyringException = assertThrows(KeyringException.class, () -> keyring.get(null, null));

        // Then an exception is thrown
        assertThat(keyringException.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testGet_collectionNull_throwsException() throws Exception {
        // Given collection is null

        // When get is called
        KeyringException keyringException = assertThrows(KeyringException.class, () -> keyring.get(user, null));

        // Then an exception is thrown
        assertThat(keyringException.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void testGet_collectionDescriptionNull_throwsException() throws Exception {
        // Given the collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        // When get is called
        KeyringException keyringException = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        // Then an exception is thrown
        assertThat(keyringException.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));
    }

    @Test
    public void testGet_collectionIDIsNull_throwsException() throws Exception {
        // Given the collection ID is null
        when(description.getId())
                .thenReturn(null);

        // When get is called
        KeyringException keyringException = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        // Then an exception is thrown
        assertThat(keyringException.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testGet_collectionIDIsEmpty_throwsException() throws Exception {
        // Given the collection ID is null
        when(collection.getDescription())
                .thenReturn(description);

        when(description.getId())
                .thenReturn("");

        // When get is called
        KeyringException keyringException = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        // Then an exception is thrown
        assertThat(keyringException.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testGetCentralKeyingNotEnabled_userKeyringNull_shouldThrowException() throws Exception {
        // Given central keyring is not enabled and the user keyring is null
        when(user.keyring())
                .thenReturn(null);

        // When get is called
        KeyringException keyringException = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        // Then an exception is thrown
        assertThat(keyringException.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
        verify(user, times(1)).keyring();
    }

    @Test
    public void testGetCentralKeyingNotEnabled_keyringReturnsNull_shouldReturnNull() throws Exception {
        // Given central keyring is not enabled and the user keyring does not contain a key for the collection
        when(legacyKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(null);

        // When get is called
        SecretKey actualKey = keyring.get(user, collection);

        // Then null is returned
        assertThat(actualKey, is(nullValue()));
        verify(user, times(1)).keyring();
        verify(legacyKeyring, times(1)).get(TEST_COLLECTION_ID);
    }

    @Test
    public void testGetCentralKeyingNotEnabled_keyringContainsKey_shouldReturnKey() throws Exception {
        // Given central keyring is not enabled and the user keyring contains a key for the collection
        when(legacyKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(secretKey);

        // When get is called
        SecretKey actualKey = keyring.get(user, collection);

        // Then the expected key is returned
        assertThat(actualKey, equalTo(secretKey));
        verify(user, times(1)).keyring();
        verify(legacyKeyring, times(1)).get(TEST_COLLECTION_ID);
    }

    @Test
    public void testGetCentralKeyingEnabled_shouldReturnNull() throws Exception {
        // Given central keyring is enabled
        keyring = new KeyringMigrationImpl(enabled, centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);

        // When get is called
        SecretKey actualKey = keyring.get(user, collection);

        // Then null is returned
        assertThat(actualKey, is(nullValue()));
        verifyZeroInteractions(user, centralKeyring);
    }

    @Test
    public void testRemove_UserIsNull_throwsException() {
        // Given the user is null

        // When remove is called
        KeyringException keyringException = assertThrows(KeyringException.class, () -> keyring.remove(null, null));

        // Then an exception is thrown
        assertThat(keyringException.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(user);
    }

    @Test
    public void testRemove_CentralKeyringEnabled_UserIsNull_throwsException() {
        // Given the user is null
        // And central keyring is enabled

        // When remove is called
        KeyringException keyringException = assertThrows(KeyringException.class, () -> keyring.remove(null, null));

        // Then an exception is thrown
        assertThat(keyringException.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(user);
    }

    @Test
    public void testRemove_CollectionIsNull_throwsException() {
        // Given the collection is null

        // When remove is called
        KeyringException keyringException = assertThrows(KeyringException.class, () -> keyring.remove(user, null));

        // Then an exception is thrown
        assertThat(keyringException.getMessage(), equalTo(COLLECTION_NULL_ERR));
        verifyZeroInteractions(user);
    }

    @Test
    public void testRemove_CollectionDescIsNull_throwsException() {
        // Given the collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        // When remove is called
        KeyringException keyringException = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertThat(keyringException.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));
        verifyZeroInteractions(user);
    }

    @Test
    public void testRemove_CollectionIDIsNull_throwsException() {
        // Given the collection description is null
        when(description.getId())
                .thenReturn(null);

        // When remove is called
        KeyringException keyringException = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertThat(keyringException.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
        verifyZeroInteractions(user);
    }

    @Test
    public void testRemove_CollectionIDIsEmpty_throwsException() {
        // Given the collection description is null
        when(description.getId())
                .thenReturn("");

        // When remove is called
        KeyringException keyringException = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertThat(keyringException.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
        verifyZeroInteractions(user);
    }

    @Test
    public void testRemove_KeyringIsNull_throwsException() {
        // Given the keyring is null
        when(user.keyring()).
                thenReturn(null);

        // When remove is called
        KeyringException keyringException = assertThrows(KeyringException.class, () -> keyring.remove(user,
                collection));

        // Then an execption is thrown
        assertThat(keyringException.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
        verify(user, times(1)).keyring();
    }

    @Test
    public void testRemove_Success_shouldNotReturnError() throws Exception {
        // Given remove is successful

        // When remove is called
        keyring.remove(user, collection);

        // Then keyring.remove is called 1 time.
        verify(user, times(1)).keyring();
        verify(legacyKeyring, times(1)).remove(TEST_COLLECTION_ID);
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testRemove_centralKeyringEnabled_RemoveShouldReturnNull() throws Exception {
        // Given central keyring is enabled
        keyring = new KeyringMigrationImpl(enabled, centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);

        // when remove is called
        keyring.remove(user, collection);

        // Then no action is taken
        verifyZeroInteractions(user, centralKeyring);
    }

    @Test
    public void testAdd_userIsNull_shouldThrowException() {
        // Given the user is null

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(null, null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testAdd_UserIsNullCentralKeyringEnabled_shouldThrowException() {
        // Given the user is null
        // And central keyring is enabled
        keyring = new KeyringMigrationImpl(enabled, centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(null, null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testAdd_collectionIsNull_shouldThrowException() {
        // Given the collection is null

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(user, null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void testAdd_CollectionIsNullCentralKeyringEnabled_shouldThrowException() {
        // Given the collection is null
        keyring = new KeyringMigrationImpl(enabled, centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(user, null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testAdd_collectionDescriptionIsNull_shouldThrowException() {
        // Given the collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));
    }

    @Test
    public void testAdd_collectionDescriptionIsNullCentralKeyringEnabled_shouldThrowException() {
        // Given the collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        keyring = new KeyringMigrationImpl(enabled, centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testAdd_collectionIDIsNull_shouldThrowException() {
        // Given the collection ID is null
        when(description.getId())
                .thenReturn(null);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testAdd_collectionIDIsNullCentralKeyringEnabled_shouldThrowException() {
        // Given the collection ID is null
        when(description.getId())
                .thenReturn(null);

        keyring = new KeyringMigrationImpl(enabled, centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testAdd_collectionIDIsEmpty_shouldThrowException() {
        // Given the collection ID is empty
        when(description.getId())
                .thenReturn("");

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testAdd_collectionIDIsEmptyCentralKeyringEnabled_shouldThrowException() {
        // Given the collection ID is empty
        when(description.getId())
                .thenReturn("");

        keyring = new KeyringMigrationImpl(enabled, centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testAdd_secretKeyIsNull_shouldThrowException() {
        // Given the secret key is null

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SECRET_KEY_NULL_ERR));
    }

    @Test
    public void testAdd_secretKeyIsNullCentralKeyringEnabled_shouldThrowException() {
        // Given the secret key is null
        keyring = new KeyringMigrationImpl(enabled, centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(user, collection, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SECRET_KEY_NULL_ERR));
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testAdd_userKeyringNull_shouldThrowException() {
        // Given user keyring is null
        when(user.keyring())
                .thenReturn(null);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(user, collection, secretKey));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testAdd_successCentralKeyringEnabled_shouldAddToLegacyKeyring() throws Exception {
        keyring.add(user, collection, secretKey);

        verify(user, times(1)).keyring();
        verify(legacyKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testAdd_successCentralKeyringEnabled_shouldDoNothing() throws Exception {
        keyring.add(user, collection, secretKey);

        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testPopulateFromUser_userNull_shouldThrowException() {
        // Given the user is null

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);
    }

    @Test
    public void testPopulateFromUser_userNullCentralKeyringEnabled_shouldThrowException() {
        // Given the user is null
        // and central keyring is enabled
        keyring = new KeyringMigrationImpl(enabled, centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);
    }

    @Test
    public void testPopulateFromUser_successCentralKeyringEnabled_shouldPopulateBothKeyrings() throws Exception {
        // Given the central keyring is enabled
        keyring = new KeyringMigrationImpl(enabled, centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);

        // When populate from user is called
        keyring.populateFromUser(user);

        // Then both the legacy and central keyrings are populated
        verify(centralKeyring, times(1)).populateFromUser(user);
        verify(sessionsService, times(1)).find(TEST_EMAIL);
        verify(legacyKeyringCache, times(1)).put(user, session);
        verify(applicationKeys, times(1)).populateCacheFromUserKeyring(legacyKeyring);
    }

    @Test
    public void testPopulateFromUser_centralKeyringError_shouldThrowException() throws Exception {
        // Given central keyring populateFromUser throws an exception
        keyring = new KeyringMigrationImpl(enabled, centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);
        doThrow(KeyringException.class)
                .when(centralKeyring)
                .populateFromUser(user);

        // When populate from user is called
        assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then both the legacy and central keyrings are populated
        verify(centralKeyring, times(1)).populateFromUser(user);
        verifyZeroInteractions(sessionsService, legacyKeyringCache, applicationKeys);
    }

    @Test
    public void testPopulateFromUser_userEmailNull_shouldThrowException() throws Exception {
        // Given the user email is null
        when(user.getEmail())
                .thenReturn(null);

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);
    }

    @Test
    public void testPopulateFromUser_userEmailEmpty_shouldThrowException() throws Exception {
        // Give the user email is empty
        when(user.getEmail())
                .thenReturn("");

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);
    }

    @Test
    public void testPopulateFromUser_getSessionError_shouldThrowException() throws Exception {
        // Given get user session returns an error
        when(sessionsService.find(TEST_EMAIL))
                .thenThrow(new IOException("Bert! Bert! You're shouting again Bert"));

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(GET_SESSION_ERR));
        verify(sessionsService, times(1)).find(TEST_EMAIL);
        verifyZeroInteractions(centralKeyring, legacyKeyringCache, applicationKeys);
    }

    @Test
    public void testPopulateFromUser_getSessionReturnsNull_shouldThrowException() throws Exception {
        // Given get user session returns null
        when(sessionsService.find(TEST_EMAIL))
                .thenReturn(null);

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SESSION_NULL_ERR));
        verify(sessionsService, times(1)).find(TEST_EMAIL);
        verifyZeroInteractions(centralKeyring, legacyKeyringCache, applicationKeys);
    }

    @Test
    public void testPopulateFromUser_legacyKeyringCacheError_shouldThrowException() throws Exception {
        // Given get legacy keyring cache throws exception
        doThrow(IOException.class)
                .when(legacyKeyringCache)
                .put(user, session);

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(LEGACY_CACHE_ERR));
        verify(sessionsService, times(1)).find(TEST_EMAIL);
        verify(legacyKeyringCache, times(1)).put(user, session);
        verifyZeroInteractions(centralKeyring, applicationKeys);
    }

    @Test
    public void testPopulateFromUser_userKeyringIsNull_shouldThrowException() throws Exception {
        // Given the user keyring is null
        when(user.keyring())
                .thenReturn(null);

        // When populate from user is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
        verifyZeroInteractions(sessionsService, legacyKeyringCache, centralKeyring, applicationKeys);
    }

    @Test
    public void testPopulateFromUser_success_shouldPopulateLegacyKeyringCache() throws Exception {
        // Given there are no errors

        // When populate from user is called
        keyring.populateFromUser(user);

        // Then the legacy keyring cache and application keys are populated from the user keyring
        verify(sessionsService, times(1)).find(TEST_EMAIL);
        verify(legacyKeyringCache, times(1)).put(user, session);
        verify(applicationKeys, times(1)).populateCacheFromUserKeyring(legacyKeyring);
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testList_userNull_shouldThrowException() {
        // Given the user is null

        // When list is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.list(null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testList_userNullCentralKeyringEnabled_shouldReturnNothing() throws KeyringException {
        // Given the user is null
        // And the central keyring is enabled
        keyring = new KeyringMigrationImpl(enabled, centralKeyring, legacyKeyringCache, applicationKeys, sessionsService);

        // When list is called
        Set<String> actual = keyring.list(null);

        // Then an exception is thrown
        assertThat(actual, is(nullValue()));
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
    public void testList_userKeyringEmpty_shouldReturnEmptySet() throws KeyringException {
        // Give user keyring is empty
        when(legacyKeyring.keySet())
                .thenReturn(new HashSet<>());

        // When list is called
        Set<String> actual = keyring.list(user);

        // Then empty set returned
        assertTrue(actual.isEmpty());
    }

    @Test
    public void testList_userKeyringIsNotEmpty_shouldReturnSet() throws KeyringException {
        // Give user keyring is empty
        when(legacyKeyring.keySet())
                .thenReturn(new HashSet<String>() {{
                    add(TEST_COLLECTION_ID);
                }});

        // When list is called
        Set<String> actual = keyring.list(user);

        // Then empty set returned
        assertThat(actual.size(), equalTo(1));
        assertTrue(actual.contains(TEST_COLLECTION_ID));
    }

}
