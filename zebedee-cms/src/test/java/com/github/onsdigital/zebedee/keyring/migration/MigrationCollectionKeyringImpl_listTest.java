package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static com.github.onsdigital.zebedee.keyring.migration.MigrationCollectionKeyringImpl.LIST_KEYS_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MigrationCollectionKeyringImpl_listTest extends MigrationCollectionKeyringImplTest {

    @Test
    public void list_migrateDisabled_listError_shouldThrowException() throws Exception {
        // Given migrate is disabled
        // And legacy keyring.list throws an exception
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        when(legacyCollectionKeyring.list(user))
                .thenThrow(keyringException);

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.list(user));

        // Then an exception is thrown
        assertWrappedException(actual, LIST_KEYS_ERR, disabled);
        verify(legacyCollectionKeyring, times(1)).list(user);
        verifyZeroInteractions(collectionKeyring);
    }

    @Test
    public void list_migrateEnabled_listError_shouldThrowException() throws Exception {
        // Given migrate is enabled
        // And legacy keyring.list throws an exception
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        when(collectionKeyring.list(user))
                .thenThrow(keyringException);

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.list(user));

        // Then an exception is thrown
        assertWrappedException(actual, LIST_KEYS_ERR, enabled);
        verify(collectionKeyring, times(1)).list(user);
        verifyZeroInteractions(legacyCollectionKeyring);
    }

    @Test
    public void list_migrateDisabled_success_shouldListKeys() throws Exception {
        // Given migrate is disabled
        keyring = new MigrationCollectionKeyringImpl(disabled, legacyCollectionKeyring, collectionKeyring);

        Set<String> keys = new HashSet<String>() {{
            add("1234567890");
        }};

        when(legacyCollectionKeyring.list(user))
                .thenReturn(keys);

        // When list is called
        Set<String> actual = keyring.list(user);

        // Then the expected key list is returned
        assertThat(actual, equalTo(keys));
        verify(legacyCollectionKeyring, times(1)).list(user);
        verifyZeroInteractions(collectionKeyring);
    }

    @Test
    public void list_migrateEnabled_success_shouldListKeys() throws Exception {
        // Given migrate is enabled
        keyring = new MigrationCollectionKeyringImpl(enabled, legacyCollectionKeyring, collectionKeyring);

        Set<String> keys = new HashSet<String>() {{
            add("1234567890");
        }};

        when(collectionKeyring.list(user))
                .thenReturn(keys);

        // When list is called
        Set<String> actual = keyring.list(user);

        // Then the expected key list is returned
        assertThat(actual, equalTo(keys));
        verify(collectionKeyring, times(1)).list(user);
        verifyZeroInteractions(legacyCollectionKeyring);
    }
}
