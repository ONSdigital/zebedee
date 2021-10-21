package com.github.onsdigital.zebedee.keyring.legacy;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static com.github.onsdigital.zebedee.keyring.legacy.LegacyCollectionKeyringImpl.CACHE_GET_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyCollectionKeyringImpl.EMAIL_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyCollectionKeyringImpl.USER_KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyCollectionKeyringImpl.USER_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LegacyCollectionKeyringImpl_RevokeFromTest extends BaseLegacyKeyringTest {

    @Mock
    private Keyring bertCachedKeyring;

    private List<CollectionDescription> removals;

    @Override
    public void setUpTests() throws Exception {
        removals = new ArrayList<>();
        removals.add(collectionDescription);
    }

    @Test
    public void testRemoveFrom_removalsNull_shouldDoNothing() throws Exception {
        List<CollectionDescription> descriptions = null;
        legacyCollectionKeyring.revokeFrom(bert, descriptions);

        verifyZeroInteractions(bertKeyring, keyringCache);
    }

    @Test
    public void testRemoveFrom_removalsEmpty_shouldDoNothing() throws Exception {
        legacyCollectionKeyring.revokeFrom(bert, new ArrayList<>());

        verifyZeroInteractions(bertKeyring, keyringCache);
    }

    @Test
    public void testRemoveFrom_userNull_shouldThrowEx() throws Exception {
        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyCollectionKeyring.revokeFrom(null, removals));

        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(bertKeyring, keyringCache);
    }

    @Test
    public void testRemoveFrom_userEmailNull_shouldThrowEx() throws Exception {
        when(bert.getEmail())
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyCollectionKeyring.revokeFrom(bert, removals));

        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(bertKeyring, keyringCache);
    }

    @Test
    public void testRemoveFrom_userEmailEmpty_shouldThrowEx() throws Exception {
        when(bert.getEmail())
                .thenReturn("");

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyCollectionKeyring.revokeFrom(bert, removals));

        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(bertKeyring, keyringCache);
    }

    @Test
    public void testRemoveFrom_userKeyringNull_shouldThrowEx() throws Exception {
        when(bert.keyring())
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyCollectionKeyring.revokeFrom(bert, removals));

        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
        verifyZeroInteractions(bertKeyring, keyringCache);
    }

    @Test
    public void testRemoveFrom_getCachedKeyringError_shouldThrowEx() throws Exception {
        when(keyringCache.get(bert))
                .thenThrow(KeyringException.class);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyCollectionKeyring.revokeFrom(bert, removals));

        assertThat(ex.getMessage(), equalTo(CACHE_GET_ERR));
        verify(keyringCache, times(1)).get(bert);
        verifyZeroInteractions(bertKeyring, users);
    }

    @Test
    public void testRemoveFrom_success_shouldRemoveKeysFromCacheAndUser() throws Exception {
        when(keyringCache.get(bert))
                .thenReturn(bertCachedKeyring);

        legacyCollectionKeyring.revokeFrom(bert, removals);

        verify(keyringCache, times(1)).get(bert);
        verify(bertCachedKeyring, times(1)).remove(TEST_COLLECTION_ID);
        verify(bertKeyring, times(1)).remove(TEST_COLLECTION_ID);
        verify(users, times(1)).updateKeyring(bert);
    }
}