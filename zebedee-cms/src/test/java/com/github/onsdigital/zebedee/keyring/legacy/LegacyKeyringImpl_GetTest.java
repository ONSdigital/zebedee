package com.github.onsdigital.zebedee.keyring.legacy;

import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.io.IOException;

import static com.github.onsdigital.zebedee.keyring.legacy.LegacyKeyringImpl.CACHE_GET_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyKeyringImpl.COLLECTION_DESC_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyKeyringImpl.COLLECTION_ID_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyKeyringImpl.COLLECTION_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyKeyringImpl.EMAIL_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyKeyringImpl.USER_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LegacyKeyringImpl_GetTest extends BaseLegacyKeyringTest {

    @Override
    public void setUpTests() throws Exception {
    }

    @Test
    public void testGet_userNull_shouldThrowException() {
        // Given user is null

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.get(null, null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testGet_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(bert.getEmail())
                .thenReturn(null);

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.get(bert, null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testGet_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(bert.getEmail())
                .thenReturn("");

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.get(bert, null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testGet_collectionNull_shouldThrowException() {
        // Given collection is null

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.get(bert, null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void testGet_collectionDescriptionNull_shouldThrowException() {
        // Given collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.get(bert, collection));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));
    }

    @Test
    public void testGet_collectionIDNull_shouldThrowException() {
        // Given collection ID is null
        when(collectionDescription.getId())
                .thenReturn(null);

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.get(bert, collection));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testGet_collectionIDEmpty_shouldThrowException() {
        // Given collection ID is empty
        when(collectionDescription.getId())
                .thenReturn("");

        // When get is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.get(bert, collection));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
    }

    @Test
    public void testGet_keyringCacheError_shouldThrowException() throws Exception {
        // Given keyring cache throws an exception
        when(keyringCache.get(bert))
                .thenThrow(IOException.class);

        // When get is called
        KeyringException actual = assertThrows(KeyringException.class,
                () -> legacyCollectionKeyring.get(bert, collection));

        // Then null is returned
        assertThat(actual.getMessage(), equalTo(CACHE_GET_ERR));
        verify(keyringCache, times(1)).get(bert);
    }

    @Test
    public void testGet_keyringCacheReturnsNull_shouldReturnNull() throws Exception {
        // Given keyring cache throws an exception
        when(keyringCache.get(bert))
                .thenReturn(null);

        // When get is called
        SecretKey actual = legacyCollectionKeyring.get(bert, collection);

        // Then null is returned
        assertThat(actual, is(nullValue()));
        verify(keyringCache, times(1)).get(bert);
    }

    @Test
    public void testGet_userKeyringIsLocked_shouldReturnNull() throws Exception {
        // Given the user keyring is locked
        when(bertKeyring.isUnlocked())
                .thenReturn(false);

        // When get is called
        SecretKey actual = legacyCollectionKeyring.get(bert, collection);

        // Then null is returned
        assertThat(actual, is(nullValue()));
        verify(keyringCache, times(1)).get(bert);
        verify(bertKeyring, times(1)).isUnlocked();
    }

    @Test
    public void testGet_keyNotInUserKeyring_shouldReturnNull() throws Exception {
        // Given the user keyring does not contain the requested collection key
        when(ernieKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(null);

        // When get is called
        SecretKey actual = legacyCollectionKeyring.get(ernie, collection);

        // Then null is returned
        assertThat(actual, is(nullValue()));
        verify(keyringCache, times(1)).get(ernie);
        verify(ernieKeyring, times(1)).isUnlocked();
        verify(ernieKeyring, times(1)).get(TEST_COLLECTION_ID);
    }

    @Test
    public void testGet_success_shouldReturnKey() throws Exception {
        // Given an unlocked user keyring exists in the cache
        // and contains the requested key
        when(bertKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(secretKey);

        // When get is called
        SecretKey actual = legacyCollectionKeyring.get(bert, collection);

        // Then the expected key is returned
        assertThat(actual, equalTo(secretKey));
        verify(keyringCache, times(1)).get(bert);
        verify(bertKeyring, times(1)).isUnlocked();
        verify(bertKeyring, times(1)).get(TEST_COLLECTION_ID);
    }
}
