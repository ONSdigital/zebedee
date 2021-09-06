package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.zebedee.keyring.KeyNotFoundException;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.junit.Test;

import javax.crypto.SecretKey;

import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.GET_KEY_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MigrationCollectionKeyringImpl_getTest extends MigrationCollectionKeyringImplTest {

    @Test
    public void get_migrateDisabled_success_shouldReadFromLegacyKeyring() throws Exception {
        // Given keyring migration is disabled
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        when(legacyCollectionKeyring.get(user, collection))
                .thenReturn(secretKey);

        // When Get is called
        SecretKey actual = keyring.get(user, collection);

        // Then the expected key is returned
        assertThat(actual, equalTo(secretKey));
        verify(legacyCollectionKeyring, times(1)).get(user, collection);
        verifyZeroInteractions(collectionKeyring);
    }

    @Test
    public void get_migrateDisabled_keyringError_shouldThrowException() throws Exception {
        // Given keyring migration is disabled
        // And legacy keyring returns an error
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        when(legacyCollectionKeyring.get(user, collection))
                .thenThrow(KeyringException.class);

        // When Get is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        // Then an exception is returned
        assertWrappedException(actual, GET_KEY_ERR, disabled);

        // And the legacy keyring is called 1 times
        verify(legacyCollectionKeyring, times(1)).get(user, collection);

        // And the central keyring is never called
        verifyZeroInteractions(collectionKeyring);
    }

    @Test
    public void get_migrateEnabled_success_shouldReadFromCentralKeyring() throws Exception {
        // Given keyring migration is enabled
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        when(collectionKeyring.get(user, collection))
                .thenReturn(secretKey);

        // When Get is called
        SecretKey actual = keyring.get(user, collection);

        // Then the expected key is returned
        assertThat(actual, equalTo(secretKey));
        verify(collectionKeyring, times(1)).get(user, collection);
        verifyZeroInteractions(legacyCollectionKeyring);
    }

    @Test
    public void get_migrateEnabled_keyringError_shouldThrowException() throws Exception {
        // Given keyring migration is enabled
        // And central keyring returns an error
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        when(collectionKeyring.get(user, collection))
                .thenThrow(KeyringException.class);

        // When Get is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        // Then an exception is returned
        assertWrappedException(actual, GET_KEY_ERR, enabled);

        // And the central keyring is called 1 times
        verify(collectionKeyring, times(1)).get(user, collection);

        // And the legacy keyring is never called
        verifyZeroInteractions(legacyCollectionKeyring);
    }

    @Test
    public void get_migrateEnabled_keyNotFound_shouldReturnKeyFromLegacyKeyring()  throws Exception {
        // Given keyring migration is enabled
        // And central keyring returns a key not found error
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        when(collectionKeyring.get(user, collection))
                .thenThrow(KeyNotFoundException.class);

        when(legacyCollectionKeyring.get(user, collection))
                .thenReturn(secretKey);

        // When Get is called
        SecretKey actual = keyring.get(user, collection);

        // Then the expected key is returned from the legacy keyring
        assertThat(actual, equalTo(secretKey));
        verify(collectionKeyring, times(1)).get(user, collection);
        verify(legacyCollectionKeyring, times(1)).get(user, collection);
    }

    @Test
    public void get_migrateEnabled_keyNotFoundAndLegacyKeyError_shouldThrowException()  throws Exception {
        // Given keyring migration is enabled
        // And central keyring returns a key not found error
        // And legacy keyring throws an exception
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        when(collectionKeyring.get(user, collection))
                .thenThrow(KeyNotFoundException.class);

        when(legacyCollectionKeyring.get(user, collection))
                .thenThrow(KeyNotFoundException.class);

        // When Get is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        // Then an exception is returned
        assertWrappedException(actual, GET_KEY_ERR, enabled);
        verify(collectionKeyring, times(1)).get(user, collection);
        verify(legacyCollectionKeyring, times(1)).get(user, collection);
    }
}
