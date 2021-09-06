package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.junit.Test;

import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.GET_KEY_ERR;
import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.KEY_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.REMOVE_KEY_ERR;
import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.ROLLBACK_FAILED_ERR;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MigrationCollectionKeyringImpl_removeTest extends MigrationCollectionKeyringImplTest {

    @Test
    public void remove_migrateDisabled_getKeyError_shouldThrownException() throws Exception {
        // Given migration is disabled
        // And get key returns an error
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        when(legacyCollectionKeyring.get(user, collection))
                .thenThrow(keyringException);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(ex, GET_KEY_ERR, disabled);
        verify(legacyCollectionKeyring, times(1)).get(user, collection);
        verify(legacyCollectionKeyring, never()).remove(user, collection);
        verify(collectionKeyring, never()).remove(user, collection);
    }

    @Test
    public void remove_migrateEnabled_getKeyError_shouldThrownException() throws Exception {
        // Given migration is enabled
        // And get key returns an error
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        when(collectionKeyring.get(user, collection))
                .thenThrow(keyringException);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(ex, GET_KEY_ERR, enabled);
        verify(collectionKeyring, times(1)).get(user, collection);
        verify(legacyCollectionKeyring, never()).remove(user, collection);
        verify(legacyCollectionKeyring, never()).remove(user, collection);
    }

    @Test
    public void remove_migrateDisabled_keyNull_shouldThrowException() throws Exception {
        // Given migration is disabled
        // And get key returns null
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        when(legacyCollectionKeyring.get(user, collection))
                .thenReturn(null);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertException(actual, KEY_NULL_ERR, disabled);
        verify(legacyCollectionKeyring, times(1)).get(user, collection);
        verify(collectionKeyring, never()).remove(user, collection);
        verify(collectionKeyring, never()).remove(user, collection);
    }

    @Test
    public void remove_migrateEnabled_keyNull_shouldThrowException() throws Exception {
        // Given migration is enabled
        // And get key returns null
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        when(collectionKeyring.get(user, collection))
                .thenReturn(null);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertException(actual, KEY_NULL_ERR, enabled);
        verify(collectionKeyring, times(1)).get(user, collection);
        verify(legacyCollectionKeyring, never()).remove(user, collection);
        verify(legacyCollectionKeyring, never()).remove(user, collection);
    }

    @Test
    public void remove_migrateDisabled_centralKeyringRemoveException_shouldThrowException() throws KeyringException {
        // Given migration is disabled
        // And central keyring returns an error
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        when(legacyCollectionKeyring.get(user, collection))
                .thenReturn(secretKey);

        doThrow(KeyringException.class)
                .when(collectionKeyring)
                .remove(user, collection);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(actual, REMOVE_KEY_ERR, disabled);
        verify(legacyCollectionKeyring, times(1)).get(user, collection);
        verify(collectionKeyring, times(1)).remove(user, collection);
        verifyNoMoreInteractions(legacyCollectionKeyring);
    }

    @Test
    public void remove_migrateEnabled_centralKeyringRemoveException_shouldThrowException() throws KeyringException {
        // Given migration is enabled
        // And central keyring returns an error
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        when(collectionKeyring.get(user, collection))
                .thenReturn(secretKey);

        doThrow(KeyringException.class)
                .when(collectionKeyring)
                .remove(user, collection);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(actual, REMOVE_KEY_ERR, enabled);
        verify(collectionKeyring, times(1)).get(user, collection);
        verify(collectionKeyring, times(1)).remove(user, collection);
        verifyZeroInteractions(legacyCollectionKeyring);
    }

    @Test
    public void remove_migrateDisabled_legacyKeyringRemoveException_shouldThrowException() throws KeyringException {
        // Given migration is disabled
        // And legacy keyring returns an error
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        when(legacyCollectionKeyring.get(user, collection))
                .thenReturn(secretKey);

        doThrow(KeyringException.class)
                .when(legacyCollectionKeyring)
                .remove(user, collection);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(actual, REMOVE_KEY_ERR, disabled);
        verify(legacyCollectionKeyring, times(1)).get(user, collection);
        verify(legacyCollectionKeyring, times(1)).remove(user, collection);
        verify(collectionKeyring, times(1)).add(user, collection, secretKey);
    }

    @Test
    public void remove_migrateEnabled_legacyKeyringRemoveException_shouldThrowException() throws KeyringException {
        // Given migration is enabled
        // And legacy keyring returns an error
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        when(collectionKeyring.get(user, collection))
                .thenReturn(secretKey);

        doThrow(keyringException)
                .when(legacyCollectionKeyring)
                .remove(user, collection);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(actual, REMOVE_KEY_ERR, enabled);
        verify(collectionKeyring, times(1)).get(user, collection);
        verify(collectionKeyring, times(1)).remove(user, collection);
        verify(legacyCollectionKeyring, times(1)).remove(user, collection);
        verify(collectionKeyring, times(1)).add(user, collection, secretKey);
    }

    @Test
    public void remove_migrateDisabled_removeRollbackFails_shouldThrowException() throws KeyringException {
        // Given migration is disabled
        // And legacy keyring returns an error
        // And remove rollback fails
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        when(legacyCollectionKeyring.get(user, collection))
                .thenReturn(secretKey);

        doThrow(keyringException)
                .when(legacyCollectionKeyring)
                .remove(user, collection);

        doThrow(keyringException)
                .when(collectionKeyring)
                .add(user, collection, secretKey);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(actual, ROLLBACK_FAILED_ERR, disabled);
        verify(legacyCollectionKeyring, times(1)).get(user, collection);
        verify(collectionKeyring, times(1)).remove(user, collection);
        verify(legacyCollectionKeyring, times(1)).remove(user, collection);
        verify(collectionKeyring, times(1)).add(user, collection, secretKey);
    }

    @Test
    public void remove_migrateEnabled_removeRollbackFails_shouldThrowException() throws KeyringException {
        // Given migration is enabled
        // And legacy keyring returns an error
        // And remove rollback fails
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        when(collectionKeyring.get(user, collection))
                .thenReturn(secretKey);

        doThrow(keyringException)
                .when(legacyCollectionKeyring)
                .remove(user, collection);

        doThrow(keyringException)
                .when(collectionKeyring)
                .add(user, collection, secretKey);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertWrappedException(actual, ROLLBACK_FAILED_ERR, enabled);
        verify(collectionKeyring, times(1)).get(user, collection);
        verify(collectionKeyring, times(1)).remove(user, collection);
        verify(legacyCollectionKeyring, times(1)).remove(user, collection);
        verify(collectionKeyring, times(1)).add(user, collection, secretKey);
    }

    @Test
    public void remove_migrateDisabled_success_shouldRemoveKeyFromBothKeyrings() throws Exception {
        // Given migration is disabled
        // And remove is successful
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        when(legacyCollectionKeyring.get(user, collection))
                .thenReturn(secretKey);

        // When remove is called
        keyring.remove(user, collection);

        // Then both the legacy and central keyrings are updated
        verify(legacyCollectionKeyring, times(1)).get(user, collection);
        verify(collectionKeyring, times(1)).remove(user, collection);
        verify(legacyCollectionKeyring, times(1)).remove(user, collection);
        verify(collectionKeyring, never()).add(user, collection, secretKey);
    }

    @Test
    public void remove_migrateEnabled_success_shouldRemoveKeyFromBothKeyrings() throws Exception {
        // Given migration is enabled
        // And remove is successful
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        when(collectionKeyring.get(user, collection))
                .thenReturn(secretKey);

        // When remove is called
        keyring.remove(user, collection);

        // Then both the legacy and central keyrings are updated
        verify(collectionKeyring, times(1)).get(user, collection);
        verify(collectionKeyring, times(1)).remove(user, collection);
        verify(legacyCollectionKeyring, times(1)).remove(user, collection);
        verify(collectionKeyring, never()).add(user, collection, secretKey);
    }
}
