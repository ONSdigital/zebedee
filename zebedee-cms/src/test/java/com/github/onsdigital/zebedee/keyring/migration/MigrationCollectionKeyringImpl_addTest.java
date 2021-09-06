package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.junit.Test;

import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.ADD_KEY_ERR;
import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.ROLLBACK_FAILED_ERR;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

public class MigrationCollectionKeyringImpl_addTest extends MigrationCollectionKeyringImplTest {


    @Test
    public void add_migrateDisabled_legacyKeyringError_shouldThrowException() throws Exception {
        // Given migration is disabled
        // And legacy keyring.add is unsuccessful
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        doThrow(keyringException).
                when(legacyCollectionKeyring)
                .add(user, collection, secretKey);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.add(user, collection, secretKey));

        // Then an exception is thrown
        assertWrappedException(actual, ADD_KEY_ERR, disabled);

        verify(legacyCollectionKeyring, times(1)).add(user, collection, secretKey);
        verifyZeroInteractions(collectionKeyring);
    }

    @Test
    public void add_migrateEnabled_legacyKeyringError_shouldThrowException() throws Exception {
        // Given migration is enabled
        // And legacy keyring.add is unsuccessful
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        doThrow(keyringException).
                when(legacyCollectionKeyring)
                .add(user, collection, secretKey);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.add(user, collection, secretKey));

        // Then an exception is thrown
        assertWrappedException(actual, ADD_KEY_ERR, enabled);

        verify(legacyCollectionKeyring, times(1)).add(user, collection, secretKey);
        verifyZeroInteractions(collectionKeyring);
    }

    @Test
    public void add_migrateDisabled_rollbackUnsuccessful_shouldThrowException() throws Exception {
        // Given migration is disabled
        // And central keyring.add is unsuccessful
        // And rollback is unsuccessful
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        doThrow(keyringException).
                when(collectionKeyring)
                .add(user, collection, secretKey);

        doThrow(keyringException).
                when(legacyCollectionKeyring)
                .remove(user, collection);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.add(user, collection, secretKey));

        // Then an exception is thrown
        assertWrappedException(actual, ROLLBACK_FAILED_ERR, disabled);

        verify(legacyCollectionKeyring, times(1)).add(user, collection, secretKey);
        verify(collectionKeyring, times(1)).add(user, collection, secretKey);
        verify(legacyCollectionKeyring, times(1)).remove(user, collection);
    }

    @Test
    public void add_migrateEnabled_rollbackUnsuccessful_shouldThrowException() throws Exception {
        // Given migration is enabled
        // And central keyring.add is unsuccessful
        // And rollback is unsuccessful
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        doThrow(keyringException).
                when(collectionKeyring)
                .add(user, collection, secretKey);

        doThrow(keyringException).
                when(legacyCollectionKeyring)
                .remove(user, collection);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.add(user, collection, secretKey));

        // Then an exception is thrown
        assertWrappedException(actual, ROLLBACK_FAILED_ERR, enabled);

        verify(legacyCollectionKeyring, times(1)).add(user, collection, secretKey);
        verify(collectionKeyring, times(1)).add(user, collection, secretKey);
        verify(legacyCollectionKeyring, times(1)).remove(user, collection);
    }

    @Test
    public void add_migrateDisabled_rollbackSuccessful_shouldThrowException() throws Exception {
        // Given migration is disabled
        // And central keyring.add is unsuccessful
        // And rollback is successful
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        doThrow(keyringException).
                when(collectionKeyring)
                .add(user, collection, secretKey);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.add(user, collection, secretKey));

        // Then an exception is thrown
        assertWrappedException(actual, ADD_KEY_ERR, disabled);

        verify(legacyCollectionKeyring, times(1)).add(user, collection, secretKey);
        verify(collectionKeyring, times(1)).add(user, collection, secretKey);
        verify(legacyCollectionKeyring, times(1)).remove(user, collection);
    }

    @Test
    public void add_migrateEnabled_rollbackSuccessful_shouldThrowException() throws Exception {
        // Given migration is enabled
        // And central keyring.add is unsuccessful
        // And rollback is successful
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        doThrow(keyringException).
                when(collectionKeyring)
                .add(user, collection, secretKey);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.add(user, collection, secretKey));

        // Then an exception is thrown
        assertWrappedException(actual, ADD_KEY_ERR, enabled);

        verify(legacyCollectionKeyring, times(1)).add(user, collection, secretKey);
        verify(collectionKeyring, times(1)).add(user, collection, secretKey);
        verify(legacyCollectionKeyring, times(1)).remove(user, collection);
    }

    @Test
    public void add_migrateDisabled_success_shouldAddKeyToBothKeyrings() throws Exception {
        // Given migration is disabled
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        // When add is called
        keyring.add(user, collection, secretKey);

        // Then the key is added to both keyrings
        verify(legacyCollectionKeyring, times(1)).add(user, collection, secretKey);
        verify(collectionKeyring, times(1)).add(user, collection, secretKey);
        verifyNoMoreInteractions(legacyCollectionKeyring, collectionKeyring);
    }

    @Test
    public void add_migrateEnabled_success_shouldAddKeyToBothKeyrings() throws Exception {
        // Given migration is enabled
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        // When add is called
        keyring.add(user, collection, secretKey);

        // Then the key is added to both keyrings
        verify(legacyCollectionKeyring, times(1)).add(user, collection, secretKey);
        verify(collectionKeyring, times(1)).add(user, collection, secretKey);
        verifyNoMoreInteractions(legacyCollectionKeyring, collectionKeyring);
    }
}
