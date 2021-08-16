package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.onsdigital.zebedee.keyring.KeyringMigratorImpl.ADD_KEY_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigratorImpl.GET_KEY_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigratorImpl.KEY_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigratorImpl.LIST_KEYS_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigratorImpl.POPULATE_FROM_USER_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigratorImpl.REMOVE_KEY_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigratorImpl.ROLLBACK_FAILED_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigratorImpl.UNLOCK_KEYRING_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigratorImpl.WRAPPED_ERR_FMT;
import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class KeyringMigratorImplTest {

    @Mock
    private User user;

    @Mock
    private Collection collection;

    @Mock
    private SecretKey secretKey;

    @Mock
    private Keyring legacyKeyring;

    @Mock
    private Keyring centralKeyring;

    private Keyring keyring;

    private KeyringException keyringException;

    private boolean enabled = true;
    private boolean disabled = false;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.keyringException = new KeyringException("keyringException");
    }

    private void assertWrappedException(KeyringException actual, String expectedMsg, boolean migrationEnabled) {
        String msg = format(WRAPPED_ERR_FMT, expectedMsg, migrationEnabled);
        assertThat(actual.getMessage(), equalTo(msg));
        assertThat(actual.getCause(), is(notNullValue()));
    }

    private void assertException(KeyringException actual, String expectedMsg, boolean migrationEnabled) {
        String msg = format(WRAPPED_ERR_FMT, expectedMsg, migrationEnabled);
        assertThat(actual.getMessage(), equalTo(msg));
    }

    @Test
    public void cacheKeyring_migrateDisabled_success_shouldUpdateBothKeyrings() throws Exception {
        // Given keyring migration is disabled
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        // When cacheKeyring is called
        keyring.cacheKeyring(user);

        // Then both the legacy and central keyrings are updated
        verify(legacyKeyring, times(1)).cacheKeyring(user);
        verify(centralKeyring, times(1)).cacheKeyring(user);
    }

    @Test
    public void cacheKeyring_migrateEnabled_success_shouldUpdateBothKeyrings() throws Exception {
        // Given keyring migration is enabled
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        // When cacheKeyring is called
        keyring.cacheKeyring(user);

        // Then both the legacy and central keyrings are updated
        verify(legacyKeyring, times(1)).cacheKeyring(user);
        verify(centralKeyring, times(1)).cacheKeyring(user);
    }

    @Test
    public void cacheKeyring_migrateDisabled_legacyKeyringException_shouldThrowException() throws Exception {
        // Given keyring migration is disabled
        // And legacy keyring errors when called.
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        doThrow(KeyringException.class)
                .when(legacyKeyring)
                .cacheKeyring(user);

        // When cacheKeyring is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.cacheKeyring(user));

        // Then an exception is thrown
        assertWrappedException(actual, POPULATE_FROM_USER_ERR, disabled);

        // And the legacy keyring is called 1 time
        verify(legacyKeyring, times(1)).cacheKeyring(user);

        // And the central keyring is never called
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void cacheKeyring_migrateEnabled_legacyKeyringException_shouldThrowException() throws Exception {
        // Given keyring migration is enabled
        // And legacy keyring errors when called.
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        doThrow(KeyringException.class)
                .when(legacyKeyring)
                .cacheKeyring(user);

        // When cacheKeyring is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.cacheKeyring(user));

        // Then an exception is thrown
        assertWrappedException(actual, POPULATE_FROM_USER_ERR, enabled);

        // And the legacy keyring is called 1 time
        verify(legacyKeyring, times(1)).cacheKeyring(user);

        // And the central keyring is never called
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void cacheKeyring_migrateDisabled_centralKeyringException_shouldThrowException() throws Exception {
        // Given keyring migration is disabled
        // And central keyring errors when called.
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        doThrow(KeyringException.class)
                .when(centralKeyring)
                .cacheKeyring(user);

        // When cacheKeyring is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.cacheKeyring(user));

        // Then an exception is thrown
        assertWrappedException(actual, POPULATE_FROM_USER_ERR, disabled);

        // And the legacy keyring is called 1 time
        verify(legacyKeyring, times(1)).cacheKeyring(user);

        // And the central keyring is called 1 time
        verify(centralKeyring, times(1)).cacheKeyring(user);
    }

    @Test
    public void cacheKeyring_migrateEnabled_centralKeyringException_shouldThrowException() throws Exception {
        // Given keyring migration is enabled
        // And central keyring errors when called.
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        doThrow(KeyringException.class)
                .when(centralKeyring)
                .cacheKeyring(user);

        // When cacheKeyring is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.cacheKeyring(user));

        // Then an exception is thrown
        assertWrappedException(actual, POPULATE_FROM_USER_ERR, enabled);

        // And the legacy keyring is called 1 time
        verify(legacyKeyring, times(1)).cacheKeyring(user);

        // And the central keyring is called 1 time
        verify(centralKeyring, times(1)).cacheKeyring(user);
    }

    @Test
    public void get_migrateDisabled_success_shouldReadFromLegacyKeyring() throws Exception {
        // Given keyring migration is disabled
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        when(legacyKeyring.get(user, collection))
                .thenReturn(secretKey);

        // When Get is called
        SecretKey actual = keyring.get(user, collection);

        // Then the expected key is returned
        assertThat(actual, equalTo(secretKey));
        verify(legacyKeyring, times(1)).get(user, collection);
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void get_migrateDisabled_keyringError_shouldThrowException() throws Exception {
        // Given keyring migration is disabled
        // And legacy keyring returns an error
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        when(legacyKeyring.get(user, collection))
                .thenThrow(KeyringException.class);

        // When Get is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        // Then an exception is returned
        assertWrappedException(actual, GET_KEY_ERR, disabled);

        // And the legacy keyring is called 1 times
        verify(legacyKeyring, times(1)).get(user, collection);

        // And the central keyring is never called
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void get_migrateEnabled_success_shouldReadFromCentralKeyring() throws Exception {
        // Given keyring migration is enabled
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        when(centralKeyring.get(user, collection))
                .thenReturn(secretKey);

        // When Get is called
        SecretKey actual = keyring.get(user, collection);

        // Then the expected key is returned
        assertThat(actual, equalTo(secretKey));
        verify(centralKeyring, times(1)).get(user, collection);
        verifyZeroInteractions(legacyKeyring);
    }

    @Test
    public void get_migrateEnabled_keyringError_shouldThrowException() throws Exception {
        // Given keyring migration is enabled
        // And central keyring returns an error
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        when(centralKeyring.get(user, collection))
                .thenThrow(KeyringException.class);

        // When Get is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        // Then an exception is returned
        assertWrappedException(actual, GET_KEY_ERR, enabled);

        // And the central keyring is called 1 times
        verify(centralKeyring, times(1)).get(user, collection);

        // And the legacy keyring is never called
        verifyZeroInteractions(legacyKeyring);
    }

    @Test
    public void remove_migrateDisabled_getKeyError_shouldThrownException() throws Exception {
        // Given migration is disabled
        // And get key returns an error
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        when(legacyKeyring.get(user, collection))
                .thenThrow(keyringException);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(ex, GET_KEY_ERR, disabled);
        verify(legacyKeyring, times(1)).get(user, collection);
        verify(legacyKeyring, never()).remove(user, collection);
        verify(centralKeyring, never()).remove(user, collection);
    }

    @Test
    public void remove_migrateEnabled_getKeyError_shouldThrownException() throws Exception {
        // Given migration is enabled
        // And get key returns an error
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        when(centralKeyring.get(user, collection))
                .thenThrow(keyringException);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(ex, GET_KEY_ERR, enabled);
        verify(centralKeyring, times(1)).get(user, collection);
        verify(legacyKeyring, never()).remove(user, collection);
        verify(legacyKeyring, never()).remove(user, collection);
    }

    @Test
    public void remove_migrateDisabled_keyNull_shouldThrowException() throws Exception {
        // Given migration is disabled
        // And get key returns null
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        when(legacyKeyring.get(user, collection))
                .thenReturn(null);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertException(actual, KEY_NULL_ERR, disabled);
        verify(legacyKeyring, times(1)).get(user, collection);
        verify(centralKeyring, never()).remove(user, collection);
        verify(centralKeyring, never()).remove(user, collection);
    }

    @Test
    public void remove_migrateEnabled_keyNull_shouldThrowException() throws Exception {
        // Given migration is enabled
        // And get key returns null
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        when(centralKeyring.get(user, collection))
                .thenReturn(null);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertException(actual, KEY_NULL_ERR, enabled);
        verify(centralKeyring, times(1)).get(user, collection);
        verify(legacyKeyring, never()).remove(user, collection);
        verify(legacyKeyring, never()).remove(user, collection);
    }

    @Test
    public void remove_migrateDisabled_centralKeyringRemoveException_shouldThrowException() throws KeyringException {
        // Given migration is disabled
        // And central keyring returns an error
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        when(legacyKeyring.get(user, collection))
                .thenReturn(secretKey);

        doThrow(KeyringException.class)
                .when(centralKeyring)
                .remove(user, collection);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(actual, REMOVE_KEY_ERR, disabled);
        verify(legacyKeyring, times(1)).get(user, collection);
        verify(centralKeyring, times(1)).remove(user, collection);
        verifyNoMoreInteractions(legacyKeyring);
    }

    @Test
    public void remove_migrateEnabled_centralKeyringRemoveException_shouldThrowException() throws KeyringException {
        // Given migration is enabled
        // And central keyring returns an error
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        when(centralKeyring.get(user, collection))
                .thenReturn(secretKey);

        doThrow(KeyringException.class)
                .when(centralKeyring)
                .remove(user, collection);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(actual, REMOVE_KEY_ERR, enabled);
        verify(centralKeyring, times(1)).get(user, collection);
        verify(centralKeyring, times(1)).remove(user, collection);
        verifyZeroInteractions(legacyKeyring);
    }

    @Test
    public void remove_migrateDisabled_legacyKeyringRemoveException_shouldThrowException() throws KeyringException {
        // Given migration is disabled
        // And legacy keyring returns an error
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        when(legacyKeyring.get(user, collection))
                .thenReturn(secretKey);

        doThrow(KeyringException.class)
                .when(legacyKeyring)
                .remove(user, collection);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(actual, REMOVE_KEY_ERR, disabled);
        verify(legacyKeyring, times(1)).get(user, collection);
        verify(legacyKeyring, times(1)).remove(user, collection);
        verify(centralKeyring, times(1)).add(user, collection, secretKey);
    }

    @Test
    public void remove_migrateEnabled_legacyKeyringRemoveException_shouldThrowException() throws KeyringException {
        // Given migration is enabled
        // And legacy keyring returns an error
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        when(centralKeyring.get(user, collection))
                .thenReturn(secretKey);

        doThrow(keyringException)
                .when(legacyKeyring)
                .remove(user, collection);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(actual, REMOVE_KEY_ERR, enabled);
        verify(centralKeyring, times(1)).get(user, collection);
        verify(centralKeyring, times(1)).remove(user, collection);
        verify(legacyKeyring, times(1)).remove(user, collection);
        verify(centralKeyring, times(1)).add(user, collection, secretKey);
    }

    @Test
    public void remove_migrateDisabled_removeRollbackFails_shouldThrowException() throws KeyringException {
        // Given migration is disabled
        // And legacy keyring returns an error
        // And remove rollback fails
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        when(legacyKeyring.get(user, collection))
                .thenReturn(secretKey);

        doThrow(keyringException)
                .when(legacyKeyring)
                .remove(user, collection);

        doThrow(keyringException)
                .when(centralKeyring)
                .add(user, collection, secretKey);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(actual, ROLLBACK_FAILED_ERR, disabled);
        verify(legacyKeyring, times(1)).get(user, collection);
        verify(centralKeyring, times(1)).remove(user, collection);
        verify(legacyKeyring, times(1)).remove(user, collection);
        verify(centralKeyring, times(1)).add(user, collection, secretKey);
    }

    @Test
    public void remove_migrateEnabled_removeRollbackFails_shouldThrowException() throws KeyringException {
        // Given migration is enabled
        // And legacy keyring returns an error
        // And remove rollback fails
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        when(centralKeyring.get(user, collection))
                .thenReturn(secretKey);

        doThrow(keyringException)
                .when(legacyKeyring)
                .remove(user, collection);

        doThrow(keyringException)
                .when(centralKeyring)
                .add(user, collection, secretKey);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(actual, ROLLBACK_FAILED_ERR, enabled);
        verify(centralKeyring, times(1)).get(user, collection);
        verify(centralKeyring, times(1)).remove(user, collection);
        verify(legacyKeyring, times(1)).remove(user, collection);
        verify(centralKeyring, times(1)).add(user, collection, secretKey);
    }

    @Test
    public void remove_migrateDisabled_success_shouldRemoveKeyFromBothKeyrings() throws Exception {
        // Given migration is disabled
        // And remove is successful
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        when(legacyKeyring.get(user, collection))
                .thenReturn(secretKey);

        // When remove is called
        keyring.remove(user, collection);

        // Then both the legacy and central keyrings are updated
        verify(legacyKeyring, times(1)).get(user, collection);
        verify(centralKeyring, times(1)).remove(user, collection);
        verify(legacyKeyring, times(1)).remove(user, collection);
        verify(centralKeyring, never()).add(user, collection, secretKey);
    }

    @Test
    public void remove_migrateEnabled_success_shouldRemoveKeyFromBothKeyrings() throws Exception {
        // Given migration is enabled
        // And remove is successful
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        when(centralKeyring.get(user, collection))
                .thenReturn(secretKey);

        // When remove is called
        keyring.remove(user, collection);

        // Then both the legacy and central keyrings are updated
        verify(centralKeyring, times(1)).get(user, collection);
        verify(centralKeyring, times(1)).remove(user, collection);
        verify(legacyKeyring, times(1)).remove(user, collection);
        verify(centralKeyring, never()).add(user, collection, secretKey);
    }

    @Test
    public void add_migrateDisabled_legacyKeyringError_shouldThrowException() throws Exception {
        // Given migration is disabled
        // And legacy keyring.add is unsuccessful
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        doThrow(keyringException).
                when(legacyKeyring)
                .add(user, collection, secretKey);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.add(user, collection, secretKey));

        // Then an exception is thrown
        assertWrappedException(actual, ADD_KEY_ERR, disabled);

        verify(legacyKeyring, times(1)).add(user, collection, secretKey);
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void add_migrateEnabled_legacyKeyringError_shouldThrowException() throws Exception {
        // Given migration is enabled
        // And legacy keyring.add is unsuccessful
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        doThrow(keyringException).
                when(legacyKeyring)
                .add(user, collection, secretKey);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.add(user, collection, secretKey));

        // Then an exception is thrown
        assertWrappedException(actual, ADD_KEY_ERR, enabled);

        verify(legacyKeyring, times(1)).add(user, collection, secretKey);
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void add_migrateDisabled_rollbackUnsuccessful_shouldThrowException() throws Exception {
        // Given migration is disabled
        // And central keyring.add is unsuccessful
        // And rollback is unsuccessful
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        doThrow(keyringException).
                when(centralKeyring)
                .add(user, collection, secretKey);

        doThrow(keyringException).
                when(legacyKeyring)
                .remove(user, collection);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.add(user, collection, secretKey));

        // Then an exception is thrown
        assertWrappedException(actual, ROLLBACK_FAILED_ERR, disabled);

        verify(legacyKeyring, times(1)).add(user, collection, secretKey);
        verify(centralKeyring, times(1)).add(user, collection, secretKey);
        verify(legacyKeyring, times(1)).remove(user, collection);
    }

    @Test
    public void add_migrateEnabled_rollbackUnsuccessful_shouldThrowException() throws Exception {
        // Given migration is enabled
        // And central keyring.add is unsuccessful
        // And rollback is unsuccessful
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        doThrow(keyringException).
                when(centralKeyring)
                .add(user, collection, secretKey);

        doThrow(keyringException).
                when(legacyKeyring)
                .remove(user, collection);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.add(user, collection, secretKey));

        // Then an exception is thrown
        assertWrappedException(actual, ROLLBACK_FAILED_ERR, enabled);

        verify(legacyKeyring, times(1)).add(user, collection, secretKey);
        verify(centralKeyring, times(1)).add(user, collection, secretKey);
        verify(legacyKeyring, times(1)).remove(user, collection);
    }

    @Test
    public void add_migrateDisabled_rollbackSuccessful_shouldThrowException() throws Exception {
        // Given migration is disabled
        // And central keyring.add is unsuccessful
        // And rollback is successful
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        doThrow(keyringException).
                when(centralKeyring)
                .add(user, collection, secretKey);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.add(user, collection, secretKey));

        // Then an exception is thrown
        assertWrappedException(actual, ADD_KEY_ERR, disabled);

        verify(legacyKeyring, times(1)).add(user, collection, secretKey);
        verify(centralKeyring, times(1)).add(user, collection, secretKey);
        verify(legacyKeyring, times(1)).remove(user, collection);
    }

    @Test
    public void add_migrateEnabled_rollbackSuccessful_shouldThrowException() throws Exception {
        // Given migration is enabled
        // And central keyring.add is unsuccessful
        // And rollback is successful
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        doThrow(keyringException).
                when(centralKeyring)
                .add(user, collection, secretKey);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.add(user, collection, secretKey));

        // Then an exception is thrown
        assertWrappedException(actual, ADD_KEY_ERR, enabled);

        verify(legacyKeyring, times(1)).add(user, collection, secretKey);
        verify(centralKeyring, times(1)).add(user, collection, secretKey);
        verify(legacyKeyring, times(1)).remove(user, collection);
    }

    @Test
    public void add_migrateDisabled_success_shouldAddKeyToBothKeyrings() throws Exception {
        // Given migration is disabled
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        // When add is called
        keyring.add(user, collection, secretKey);

        // Then the key is added to both keyrings
        verify(legacyKeyring, times(1)).add(user, collection, secretKey);
        verify(centralKeyring, times(1)).add(user, collection, secretKey);
        verifyNoMoreInteractions(legacyKeyring, centralKeyring);
    }

    @Test
    public void add_migrateEnabled_success_shouldAddKeyToBothKeyrings() throws Exception {
        // Given migration is enabled
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        // When add is called
        keyring.add(user, collection, secretKey);

        // Then the key is added to both keyrings
        verify(legacyKeyring, times(1)).add(user, collection, secretKey);
        verify(centralKeyring, times(1)).add(user, collection, secretKey);
        verifyNoMoreInteractions(legacyKeyring, centralKeyring);
    }

    @Test
    public void list_migrateDisabled_listError_shouldThrowException() throws Exception {
        // Given migrate is disabled
        // And legacy keyring.list throws an exception
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        when(legacyKeyring.list(user))
                .thenThrow(keyringException);

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.list(user));

        // Then an exception is thrown
        assertWrappedException(actual, LIST_KEYS_ERR, disabled);
        verify(legacyKeyring, times(1)).list(user);
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void list_migrateEnabled_listError_shouldThrowException() throws Exception {
        // Given migrate is enabled
        // And legacy keyring.list throws an exception
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        when(centralKeyring.list(user))
                .thenThrow(keyringException);

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.list(user));

        // Then an exception is thrown
        assertWrappedException(actual, LIST_KEYS_ERR, enabled);
        verify(centralKeyring, times(1)).list(user);
        verifyZeroInteractions(legacyKeyring);
    }

    @Test
    public void list_migrateDisabled_success_shouldListKeys() throws Exception {
        // Given migrate is disabled
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        Set<String> keys = new HashSet<String>() {{
            add("1234567890");
        }};

        when(legacyKeyring.list(user))
                .thenReturn(keys);

        // When list is called
        Set<String> actual = keyring.list(user);

        // Then the expected key list is returned
        assertThat(actual, equalTo(keys));
        verify(legacyKeyring, times(1)).list(user);
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void list_migrateEnabled_success_shouldListKeys() throws Exception {
        // Given migrate is enabled
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        Set<String> keys = new HashSet<String>() {{
            add("1234567890");
        }};

        when(centralKeyring.list(user))
                .thenReturn(keys);

        // When list is called
        Set<String> actual = keyring.list(user);

        // Then the expected key list is returned
        assertThat(actual, equalTo(keys));
        verify(centralKeyring, times(1)).list(user);
        verifyZeroInteractions(legacyKeyring);
    }

    @Test
    public void unlock_migrateDisbaled_shouldCallLegacyKeyringUnlock() throws Exception {
        // Given migrate is disabled
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        // When unlocked is called
        keyring.unlock(user, "1234");

        // Then legacy keyring is unlocked
        verify(legacyKeyring, times(1)).unlock(user, "1234");
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void unlock_migrateEnabled_shouldCallLegacyKeyringOnly() throws Exception {
        // Given migrate is enabled
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        // When unlocked is called
        keyring.unlock(user, "1234");

        // Then central keyring is unlocked
        verify(legacyKeyring, times(1)).unlock(user, "1234");
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void unlock_migrateDisabled_unlockErrorShouldThrowException() throws Exception {
        // Given migrate is disabled
        // And legacy keyring unlock throws an exception
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        String password = "1234";

        doThrow(KeyringException.class)
                .when(legacyKeyring)
                .unlock(user, password);

        // When unlocked is called
        KeyringException actual = assertThrows(KeyringException.class, ()->keyring.unlock(user, password));

        // Then an exception is thrown
        assertWrappedException(actual, UNLOCK_KEYRING_ERR, disabled);

        verify(legacyKeyring, times(1)).unlock(user, password);
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void unlock_migrateEnabled_unlockErrorShouldThrowException() throws Exception {
        // Given migrate is enabled
        // And legacy keyring unlock throws an exception
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        String password = "1234";

        doThrow(KeyringException.class)
                .when(legacyKeyring)
                .unlock(user, password);

        // When unlocked is called
        KeyringException actual = assertThrows(KeyringException.class, ()->keyring.unlock(user, password));

        // Then an exception is thrown
        assertWrappedException(actual, UNLOCK_KEYRING_ERR, enabled);

        verify(legacyKeyring, times(1)).unlock(user, password);
        verifyZeroInteractions(centralKeyring);
    }


    @Test
    public void testAssignTo_shouldCallLegacyKeyring() throws Exception {
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        User src = mock(User.class);
        User target = mock(User.class);
        List<CollectionDescription> assignments = mock(List.class);

        keyring.assignTo(src, target, assignments);

        verify(legacyKeyring, times(1)).assignTo(src, target, assignments);
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testRemoveFrom_shouldCallLegacyKeyring() throws Exception {
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        User target = mock(User.class);
        List<CollectionDescription> removals = mock(List.class);

        keyring.revokeFrom(target, removals);

        verify(legacyKeyring, times(1)).revokeFrom(target, removals);
        verifyZeroInteractions(centralKeyring);
    }
}
