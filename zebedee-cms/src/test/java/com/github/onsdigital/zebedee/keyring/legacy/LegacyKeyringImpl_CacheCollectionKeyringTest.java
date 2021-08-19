package com.github.onsdigital.zebedee.keyring.legacy;

import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.junit.Test;

import java.io.IOException;

import static com.github.onsdigital.zebedee.keyring.legacy.LegacyCollectionKeyringImpl.CACHE_PUT_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyCollectionKeyringImpl.EMAIL_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyCollectionKeyringImpl.GET_SESSION_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyCollectionKeyringImpl.USER_KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyCollectionKeyringImpl.USER_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LegacyKeyringImpl_CacheCollectionKeyringTest extends BaseLegacyKeyringTest {

    @Override
    public void setUpTests() throws Exception {
    }

    @Test
    public void testCacheKeyring_userNull_shouldThrowException() {
        // Given user is null

        // When cacheKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.cacheKeyring(null));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testCacheKeyring_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(bert.getEmail())
                .thenReturn(null);

        // When cacheKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.cacheKeyring(bert));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testCacheKeyring_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(bert.getEmail())
                .thenReturn("");

        // When cacheKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.cacheKeyring(bert));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testCacheKeyring_userKeyringNull_shouldThrowException() {
        // Given user keyring is null
        when(bert.keyring())
                .thenReturn(null);

        // When cacheKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.cacheKeyring(bert));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
    }

    @Test
    public void testCacheKeyring_getSessionError_shouldThrowException() throws IOException {
        // Given get session throws an exception
        when(sessionsService.find(EMAIL_BERT))
                .thenThrow(expectedEx);

        // When cacheKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.cacheKeyring(bert));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(GET_SESSION_ERR));
        assertThat(ex.getCause(), equalTo(expectedEx));
    }

    @Test
    public void testCacheKeyring_getSessionReturnsNull_doNothing() throws IOException {
        // Given get session returns null
        when(sessionsService.find(EMAIL_BERT))
                .thenReturn(null);

        // When cacheKeyring is called
        legacyCollectionKeyring.cacheKeyring(bert);

        // Then no keying is added to the cache
        verify(sessionsService, times(1)).find(EMAIL_BERT);
        verifyZeroInteractions(keyringCache);
    }

    @Test
    public void testCacheKeyring_cachePutError_shouldThrowException() throws IOException {
        // Given cache put throws an exception
        doThrow(expectedEx)
                .when(keyringCache)
                .put(bert, session);

        // When cacheKeyring is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.cacheKeyring(bert));

        // Then an exception is thrown.
        assertThat(ex.getMessage(), equalTo(CACHE_PUT_ERR));
        assertThat(ex.getCause(), equalTo(expectedEx));
    }

    @Test
    public void testCacheKeyring_success_shouldPopulateCacheAndAppKeys() throws IOException {
        // Given a valid user

        // When cacheKeyring is called
        legacyCollectionKeyring.cacheKeyring(bert);

        // Then the keyring cache is updated with the users keys
        verify(sessionsService, times(1)).find(EMAIL_BERT);
        verify(keyringCache, times(1)).put(bert, session);
    }
}
