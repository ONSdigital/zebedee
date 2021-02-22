package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;

import static com.github.onsdigital.zebedee.keyring.KeyringImpl.COLLECTION_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.USER_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigrationImpl.COLLECTION_DESC_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigrationImpl.COLLECTION_ID_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigrationImpl.SECRET_KEY_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigrationImpl.USER_KEYRING_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


public class KeyringMigrationImplTest {

    static final String TEST_COLLECTION_ID = "abc123";

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

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(collection.getDescription())
                .thenReturn(description);

        when(description.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(user.keyring())
                .thenReturn(legacyKeyring);

        this.keyring = new KeyringMigrationImpl(false, null);
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
        keyring = new KeyringMigrationImpl(true, centralKeyring);

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
        keyring = new KeyringMigrationImpl(true, centralKeyring);

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
        keyring = new KeyringMigrationImpl(true, centralKeyring);

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
        keyring = new KeyringMigrationImpl(true, centralKeyring);

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

        keyring = new KeyringMigrationImpl(true, centralKeyring);

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

        keyring = new KeyringMigrationImpl(true, centralKeyring);

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

        keyring = new KeyringMigrationImpl(true, centralKeyring);

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
        keyring = new KeyringMigrationImpl(true, centralKeyring);

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

}
