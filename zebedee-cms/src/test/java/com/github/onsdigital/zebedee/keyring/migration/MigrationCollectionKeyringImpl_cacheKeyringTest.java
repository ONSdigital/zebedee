package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.junit.Test;

import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.POPULATE_FROM_USER_ERR;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class MigrationCollectionKeyringImpl_cacheKeyringTest extends MigrationCollectionKeyringImplTest {

    @Test
    public void cacheKeyring_migrateDisabled_success_shouldUpdateBothKeyrings() throws Exception {
        // Given keyring migration is disabled
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        // When cacheKeyring is called
        keyring.cacheKeyring(user);

        // Then both the legacy and central keyrings are updated
        verify(legacyCollectionKeyring, times(1)).cacheKeyring(user);
        verify(collectionKeyring, times(1)).cacheKeyring(user);
    }

    @Test
    public void cacheKeyring_migrateEnabled_success_shouldUpdateBothKeyrings() throws Exception {
        // Given keyring migration is enabled
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        // When cacheKeyring is called
        keyring.cacheKeyring(user);

        // Then both the legacy and central keyrings are updated
        verify(legacyCollectionKeyring, times(1)).cacheKeyring(user);
        verify(collectionKeyring, times(1)).cacheKeyring(user);
    }

    @Test
    public void cacheKeyring_migrateDisabled_legacyKeyringException_shouldThrowException() throws Exception {
        // Given keyring migration is disabled
        // And legacy keyring errors when called.
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        doThrow(KeyringException.class)
                .when(legacyCollectionKeyring)
                .cacheKeyring(user);

        // When cacheKeyring is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.cacheKeyring(user));

        // Then an exception is thrown
        assertWrappedException(actual, POPULATE_FROM_USER_ERR, disabled);

        // And the legacy keyring is called 1 time
        verify(legacyCollectionKeyring, times(1)).cacheKeyring(user);

        // And the central keyring is never called
        verifyZeroInteractions(collectionKeyring);
    }

    @Test
    public void cacheKeyring_migrateEnabled_legacyKeyringException_shouldThrowException() throws Exception {
        // Given keyring migration is enabled
        // And legacy keyring errors when called.
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        doThrow(KeyringException.class)
                .when(legacyCollectionKeyring)
                .cacheKeyring(user);

        // When cacheKeyring is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.cacheKeyring(user));

        // Then an exception is thrown
        assertWrappedException(actual, POPULATE_FROM_USER_ERR, enabled);

        // And the legacy keyring is called 1 time
        verify(legacyCollectionKeyring, times(1)).cacheKeyring(user);

        // And the central keyring is never called
        verifyZeroInteractions(collectionKeyring);
    }

    @Test
    public void cacheKeyring_migrateDisabled_centralKeyringException_shouldThrowException() throws Exception {
        // Given keyring migration is disabled
        // And central keyring errors when called.
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        doThrow(KeyringException.class)
                .when(collectionKeyring)
                .cacheKeyring(user);

        // When cacheKeyring is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.cacheKeyring(user));

        // Then an exception is thrown
        assertWrappedException(actual, POPULATE_FROM_USER_ERR, disabled);

        // And the legacy keyring is called 1 time
        verify(legacyCollectionKeyring, times(1)).cacheKeyring(user);

        // And the central keyring is called 1 time
        verify(collectionKeyring, times(1)).cacheKeyring(user);
    }

    @Test
    public void cacheKeyring_migrateEnabled_centralKeyringException_shouldThrowException() throws Exception {
        // Given keyring migration is enabled
        // And central keyring errors when called.
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        doThrow(KeyringException.class)
                .when(collectionKeyring)
                .cacheKeyring(user);

        // When cacheKeyring is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.cacheKeyring(user));

        // Then an exception is thrown
        assertWrappedException(actual, POPULATE_FROM_USER_ERR, enabled);

        // And the legacy keyring is called 1 time
        verify(legacyCollectionKeyring, times(1)).cacheKeyring(user);

        // And the central keyring is called 1 time
        verify(collectionKeyring, times(1)).cacheKeyring(user);
    }
}
