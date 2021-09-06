package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.junit.Test;

import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.UNLOCK_KEYRING_ERR;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class MigrationCollectionKeyringImpl_unlockTest extends MigrationCollectionKeyringImplTest {

    @Test
    public void unlock_migrateDisbaled_shouldCallLegacyKeyringUnlock() throws Exception {
        // Given migrate is disabled
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        // When unlocked is called
        keyring.unlock(user, "1234");

        // Then legacy keyring is unlocked
        verify(legacyCollectionKeyring, times(1)).unlock(user, "1234");
        verifyZeroInteractions(collectionKeyring);
    }

    @Test
    public void unlock_migrateEnabled_shouldCallLegacyKeyringOnly() throws Exception {
        // Given migrate is enabled
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        // When unlocked is called
        keyring.unlock(user, "1234");

        // Then central keyring is unlocked
        verify(legacyCollectionKeyring, times(1)).unlock(user, "1234");
        verifyZeroInteractions(collectionKeyring);
    }

    @Test
    public void unlock_migrateDisabled_unlockErrorShouldThrowException() throws Exception {
        // Given migrate is disabled
        // And legacy keyring unlock throws an exception
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        String password = "1234";

        doThrow(KeyringException.class)
                .when(legacyCollectionKeyring)
                .unlock(user, password);

        // When unlocked is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.unlock(user, password));

        // Then an exception is thrown
        assertWrappedException(actual, UNLOCK_KEYRING_ERR, disabled);

        verify(legacyCollectionKeyring, times(1)).unlock(user, password);
        verifyZeroInteractions(collectionKeyring);
    }

    @Test
    public void unlock_migrateEnabled_unlockErrorShouldThrowException() throws Exception {
        // Given migrate is enabled
        // And legacy keyring unlock throws an exception
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        String password = "1234";

        doThrow(KeyringException.class)
                .when(legacyCollectionKeyring)
                .unlock(user, password);

        // When unlocked is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.unlock(user, password));

        // Then an exception is thrown
        assertWrappedException(actual, UNLOCK_KEYRING_ERR, enabled);

        verify(legacyCollectionKeyring, times(1)).unlock(user, password);
        verifyZeroInteractions(collectionKeyring);
    }
}
