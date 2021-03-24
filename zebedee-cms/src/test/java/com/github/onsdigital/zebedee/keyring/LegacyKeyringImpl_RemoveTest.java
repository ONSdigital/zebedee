package com.github.onsdigital.zebedee.keyring;

import org.junit.Test;

import java.io.IOException;

import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_DESC_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_ID_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.EMAIL_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.REMOVE_KEY_SAVE_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.USER_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LegacyKeyringImpl_RemoveTest extends BaseLegacyKeyringTest {

    @Override
    public void setUpTests() throws Exception {

    }

    @Test
    public void testRemove_userNull_shouldThrowException() {
        // Given user is null

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(bert.getEmail())
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(bert, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(bert.getEmail())
                .thenReturn("");

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(bert, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_collectionNull_shouldThrowException() {
        // Given collection is null

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(bert, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_collectionDescriptionNull_shouldThrowException() {
        // Given collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(bert, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_collectionIDNull_shouldThrowException() {
        // Given collection ID is null
        when(collectionDescription.getId())
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(bert, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_collectionIDEmpty_shouldThrowException() {
        // Given collection ID is empty
        when(collectionDescription.getId())
                .thenReturn("");

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyKeyring.remove(bert, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_userKeyringNotInCache_shouldStillUpdateAndSaveUser() throws Exception {
        // Given the keyring cache does not contain the users keyring
        when(keyringCache.get(bert))
                .thenReturn(null);

        // When remove is called
        legacyKeyring.remove(bert, collection);

        // Then the user record is updated and saved
        verify(users, times(1)).removeKeyFromKeyring(EMAIL_BERT, TEST_COLLECTION_ID);

        // And no update is made the cached user keyring
        verify(keyringCache, times(1)).get(bert);
        verifyZeroInteractions(bertKeyring);
    }

    @Test
    public void testRemove_userKeyringNotInCacheSaveUserError_shouldThrowException() throws Exception {
        // Given the keyring cache does not contain the users keyring
        when(keyringCache.get(bert))
                .thenReturn(null);

        IOException cause = new IOException("save user error");
        when(users.removeKeyFromKeyring(EMAIL_BERT, TEST_COLLECTION_ID))
                .thenThrow(cause);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.remove(bert, collection));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(REMOVE_KEY_SAVE_ERR));
        assertThat(actual.getCause(), equalTo(cause));

        verify(keyringCache, times(1)).get(bert);
        verify(users, times(1)).removeKeyFromKeyring(EMAIL_BERT, TEST_COLLECTION_ID);
    }

    @Test
    public void testRemove_success_shouldRemoveKeyFromCacheKeyringAndSaveUser() throws Exception {
        // Given the users cached keyring is unlocked

        // When remove is called
        legacyKeyring.remove(bert, collection);

        // Then the key is removed from cached keyring
        verify(keyringCache, times(1)).get(bert);
        verify(bertKeyring, times(1)).remove(TEST_COLLECTION_ID);

        // And the user is updated and saved
        verify(users, times(1)).removeKeyFromKeyring(EMAIL_BERT, TEST_COLLECTION_ID);
    }

    @Test
    public void testRemove_saveChangesToUserError_shouldThrowException() throws Exception {
        // Given removing the key from the user and saving the change throws an exception
        IOException cause = new IOException("save user error");

        when(users.removeKeyFromKeyring(EMAIL_BERT, TEST_COLLECTION_ID))
                .thenThrow(cause);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.remove(bert, collection));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(REMOVE_KEY_SAVE_ERR));
        assertThat(actual.getCause(), equalTo(cause));

        verify(keyringCache, times(1)).get(bert);
        verify(bertKeyring, times(1)).remove(TEST_COLLECTION_ID);
        verify(users, times(1)).removeKeyFromKeyring(EMAIL_BERT, TEST_COLLECTION_ID);
    }
}
