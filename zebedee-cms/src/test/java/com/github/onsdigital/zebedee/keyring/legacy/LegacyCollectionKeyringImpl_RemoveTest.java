package com.github.onsdigital.zebedee.keyring.legacy;

import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;

import static com.github.onsdigital.zebedee.keyring.legacy.LegacyCollectionKeyringImpl.COLLECTION_DESC_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyCollectionKeyringImpl.COLLECTION_ID_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyCollectionKeyringImpl.COLLECTION_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyCollectionKeyringImpl.LIST_USERS_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyCollectionKeyringImpl.REMOVE_KEY_SAVE_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LegacyCollectionKeyringImpl_RemoveTest extends BaseLegacyKeyringTest {

    @Override
    public void setUpTests() throws Exception {

    }

    @Test
    public void testRemove_collectionNull_shouldThrowException() {
        // Given collection is null

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.remove(null, null));

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
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.remove(null, collection));

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
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.remove(null, collection));

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
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.remove(null, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testRemove_listUsersError_shouldThrowException() throws Exception {
        // Given list users throws an exception
        when(users.list())
                .thenThrow(IOException.class);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> legacyCollectionKeyring.remove(null, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(LIST_USERS_ERR));
        assertTrue(ex.getCause() instanceof IOException);

        // And the key is not removed from any user
        verifyKeyNotRemovedFromUser(bert, bertKeyring);
        verifyKeyNotRemovedFromUser(ernie, ernieKeyring);
        verifyKeyNotRemovedFromUser(theCount, theCountKeyring);

        verify(users, times(1)).list();
        verify(keyringCache, never()).get(any(User.class));
    }

    @Test
    public void testRemove_listUsersReturnsNull_shouldNotRemoveKeyFromAnyUser() throws Exception {
        // Given list users rertuns null
        when(users.list())
                .thenReturn(null);

        // When remove is called
        legacyCollectionKeyring.remove(null, collection);

        // Then the key is not removed from any user
        verifyKeyNotRemovedFromUser(bert, bertKeyring);
        verifyKeyNotRemovedFromUser(ernie, ernieKeyring);
        verifyKeyNotRemovedFromUser(theCount, theCountKeyring);

        verify(users, times(1)).list();
        verify(keyringCache, never()).get(any(User.class));
    }

    @Test
    public void testRemove_listUsersReturnsEmpty_shouldNotRemoveKeyFromAnyUser() throws Exception {
        // Given list users rertuns an empty userlist
        when(users.list())
                .thenReturn(new UserList());

        // When remove is called
        legacyCollectionKeyring.remove(null, collection);

        // Then the key is not removed from any user
        verifyKeyNotRemovedFromUser(bert, bertKeyring);
        verifyKeyNotRemovedFromUser(ernie, ernieKeyring);
        verifyKeyNotRemovedFromUser(theCount, theCountKeyring);

        verify(users, times(1)).list();
        verify(keyringCache, never()).get(any(User.class));
    }

    @Test
    public void testRemove_removeKeyFromKeyringError_keyringInCache_shouldThrowException() throws Exception {
        // Given removeKeyFromKeyring throws an exception
        // And the user's keyring is in the cache
        doThrow(IOException.class)
                .when(users)
                .removeKeyFromKeyring(EMAIL_BERT, TEST_COLLECTION_ID);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyCollectionKeyring.remove(null, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(REMOVE_KEY_SAVE_ERR));
        assertTrue(ex.getCause() instanceof IOException);

        verifyUserKeyringRetrievedFromCache(bert);
        verifyKeyRemovedFromUser(bert, bertKeyring);

        verifyUserKeyringNotRetrievedFromCache(ernie);
        verifyKeyNotRemovedFromUser(ernie, ernieKeyring);

        verifyUserKeyringNotRetrievedFromCache(theCount);
        verifyKeyNotRemovedFromUser(theCount, theCountKeyring);

        verify(users, times(1)).list();
        verify(users, times(1)).removeKeyFromKeyring(EMAIL_BERT, TEST_COLLECTION_ID);
    }

    @Test
    public void testRemove_removeKeyFromKeyringError_keyringNotInCache_shouldThrowException() throws Exception {
        // Given removeKeyFromKeyring throws an exception
        // And the user's keyring is not in the cache
        doThrow(IOException.class)
                .when(users)
                .removeKeyFromKeyring(EMAIL_BERT, TEST_COLLECTION_ID);

        when(keyringCache.get(bert))
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyCollectionKeyring.remove(null, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(REMOVE_KEY_SAVE_ERR));
        assertTrue(ex.getCause() instanceof IOException);

        verifyUserKeyringRetrievedFromCache(bert);
        verify(bertKeyring, never()).remove(TEST_COLLECTION_ID);

        verifyUserKeyringNotRetrievedFromCache(ernie);
        verifyKeyNotRemovedFromUser(ernie, ernieKeyring);

        verifyUserKeyringNotRetrievedFromCache(theCount);
        verifyKeyNotRemovedFromUser(theCount, theCountKeyring);

        verify(users, times(1)).list();
        verify(users, times(1)).removeKeyFromKeyring(EMAIL_BERT, TEST_COLLECTION_ID);
    }

    @Test
    public void testRemove_success_userKeyringNotInCache_shouldThrowException() throws Exception {
        // Given remove is success
        // And the user's keyring is not in the cache
        when(keyringCache.get(bert))
                .thenReturn(null);

        // When remove is called
        legacyCollectionKeyring.remove(null, collection);

        // Then the key is successfully removed from the stored user
        verifyUserKeyringRetrievedFromCache(bert);

        // And the keyring cache is not updated.
        verify(bertKeyring, never()).remove(TEST_COLLECTION_ID);

        // And the others users are updated
        verifyUserKeyringRetrievedFromCache(ernie);
        verifyKeyRemovedFromUser(ernie, ernieKeyring);

        verifyUserKeyringRetrievedFromCache(theCount);
        verifyKeyRemovedFromUser(theCount, theCountKeyring);

        verify(users, times(1)).list();
        verify(users, times(3)).removeKeyFromKeyring(any(), any());
    }

    @Test
    public void testRemove_success_userKeyringInCache_shouldRemoveKeyFromUsers() throws Exception {
        // Given remove is success
        // And the user's keyring is in the cache

        // When remove is called
        legacyCollectionKeyring.remove(null, collection);

        // Then the key is successfully removed from all cached user keyrings
        // And the key is removed from all stored users.
        verifyUserKeyringRetrievedFromCache(bert);
        verifyKeyRemovedFromUser(bert, bertKeyring);

        verifyUserKeyringRetrievedFromCache(ernie);
        verifyKeyRemovedFromUser(ernie, ernieKeyring);

        verifyUserKeyringRetrievedFromCache(theCount);
        verifyKeyRemovedFromUser(theCount, theCountKeyring);

        verify(users, times(1)).list();
        verify(users, times(3)).removeKeyFromKeyring(any(), any());
    }

    @Test
    public void testRemove_success_shouldUpdateUsersWhoHaveKey() throws Exception {
        // Given remove is success
        // And the user's keyring is in the cache

        // Bert has the key
        when(bertKeyring.keySet())
                .thenReturn(keyringKeyset);

        // Ernie doesn't have the key
        when(ernieKeyring.keySet())
                .thenReturn(new HashSet<>());

        // The Count has the key
        when(theCountKeyring.keySet())
                .thenReturn(keyringKeyset);

        // When remove is called
        legacyCollectionKeyring.remove(null, collection);

        // Then the key is successfully removed from only users who have the key in their keyring - Bert and The Count.
        verifyUserKeyringRetrievedFromCache(bert);
        verifyKeyRemovedFromUser(bert, bertKeyring);

        verifyKeyNotRemovedFromUser(ernie, ernieKeyring);
        verifyUserKeyringNotRetrievedFromCache(ernie);

        verifyUserKeyringRetrievedFromCache(theCount);
        verifyKeyRemovedFromUser(theCount, theCountKeyring);

        verify(users, times(1)).list();
        verify(users, times(2)).removeKeyFromKeyring(any(), any());
    }
}
