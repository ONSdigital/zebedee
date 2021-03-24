package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;

import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.ADD_KEY_SAVE_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_DESC_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_ID_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.COLLECTION_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.GET_KEY_REIPIENTS_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.LIST_USERS_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.REMOVE_KEY_SAVE_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.SECRET_KEY_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests to verify LegacyKeyringImpl#Add().
 */
public class LegacyKeyringImpl_AddTest extends BaseLegacyKeyringTest {

    @Mock
    private User user;

    @Override
    public void setUpTests() throws Exception {
        // set up goes here.
    }

    @Test
    public void testAdd_collectionNull_shouldThrowException() {
        // Given collection is null

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(null, null, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(COLLECTION_NULL_ERR));

        // And no users are updated
        verifyUsersNotUpdated(bert, ernie, theCount);
        verifyZeroInteractions(bertKeyring, ernieKeyring, theCountKeyring);
        verifyZeroInteractions(users, keyringCache, permissions);
    }

    @Test
    public void testAdd_collectionDescriptionNull_shouldThrowException() {
        // Given collection description is null
        when(collection.getDescription())
                .thenReturn(null);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class,
                () -> legacyKeyring.add(null, collection, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(COLLECTION_DESC_NULL_ERR));

        // And no users are updated
        verifyUsersNotUpdated(bert, ernie, theCount);
        verifyZeroInteractions(bertKeyring, ernieKeyring, theCountKeyring);
        verifyZeroInteractions(user, permissions, keyringCache);
    }

    @Test
    public void testAdd_collectionDescriptionIDNull_shouldThrowException() {
        // Given collection ID is null
        when(collectionDescription.getId())
                .thenReturn(null);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(null, collection, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));

        // And no users are updated
        verifyUsersNotUpdated(bert, ernie, theCount);
        verifyZeroInteractions(bertKeyring, ernieKeyring, theCountKeyring);
        verifyZeroInteractions(user, permissions, keyringCache);
    }

    @Test
    public void testAdd_collectionDescriptionIDEmpty_shouldThrowException() {
        // Given collection ID is empty
        when(collectionDescription.getId())
                .thenReturn("");

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(null, collection, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(COLLECTION_ID_EMPTY_ERR));

        // And no users are updated
        verifyUsersNotUpdated(bert, ernie, theCount);
        verifyZeroInteractions(bertKeyring, ernieKeyring, theCountKeyring);
        verifyZeroInteractions(user, permissions, keyringCache);
    }

    @Test
    public void testAdd_keyNull_shouldThrowException() {
        // Given key is null

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.add(null, collection, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(SECRET_KEY_NULL_ERR));

        // And no users are updated
        verifyUsersNotUpdated(bert, ernie, theCount);
        verifyZeroInteractions(bertKeyring, ernieKeyring, theCountKeyring);
        verifyZeroInteractions(user, permissions, keyringCache);
    }

    @Test
    public void testAdd_GetUsersWithAccessError_shouldThrowException() throws Exception {
        // Given get collection access mapping throws an exception
        when(permissions.getCollectionAccessMapping(collection))
                .thenThrow(IOException.class);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.add(null, collection, secretKey));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(GET_KEY_REIPIENTS_ERR));
        assertTrue(ex.getCause() instanceof IOException);

        verify(permissions, times(1)).getCollectionAccessMapping(collection);

        // And no users are updated
        verifyUsersNotUpdated(bert, ernie, theCount);
        verifyZeroInteractions(bertKeyring, ernieKeyring, theCountKeyring);
        verifyZeroInteractions(user, permissions, keyringCache);
    }

    @Test
    public void testAdd_GetUsersWithAccessRetunsNull_shouldNotAssignKeyToAnyUsers() throws Exception {
        // Given get collection access mapping returns null
        when(permissions.getCollectionAccessMapping(collection))
                .thenReturn(null);

        // When add is called
        legacyKeyring.add(null, collection, secretKey);

        // Then the key is not assigned to any users
        verify(permissions, times(1)).getCollectionAccessMapping(collection);
        verify(users, never()).addKeyToKeyring(any(), any(), any());

        // And the key is removed from the expected users
        verify(users, times(1)).removeKeyFromKeyring(EMAIL_THE_COUNT, TEST_COLLECTION_ID);
        verify(keyringCache, times(1)).get(theCount);
        verify(theCountKeyring, times(1)).remove(TEST_COLLECTION_ID);
    }

    @Test
    public void testAdd_GetUsersWithAccessRetunsEmpty_shouldNotAssignKeyToAnyUsers() throws Exception {
        // Given get collection access mapping returns empty
        when(permissions.getCollectionAccessMapping(collection))
                .thenReturn(new ArrayList<>());

        // When add is called
        legacyKeyring.add(null, collection, secretKey);

        // Then the key is not assigned to any users
        verify(permissions, times(1)).getCollectionAccessMapping(collection);
        verify(users, never()).addKeyToKeyring(any(), any(), any());

        // And the key is removed from the expected users
        verify(users, times(1)).removeKeyFromKeyring(EMAIL_THE_COUNT, TEST_COLLECTION_ID);
        verify(keyringCache, times(1)).get(theCount);
        verify(theCountKeyring, times(1)).remove(TEST_COLLECTION_ID);
    }

    @Test
    public void testAdd_GetUsersError_shouldThrowException() throws Exception {
        // Given get users throws an exception
        when(users.list())
                .thenThrow(IOException.class);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.add(null, collection, secretKey));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(LIST_USERS_ERR));
        assertTrue(ex.getCause() instanceof IOException);

        // And the key is not assigned to anyone
        verify(permissions, times(1)).getCollectionAccessMapping(collection);
        verify(users, times(1)).list();

        // And no users are updated
        verifyUsersNotUpdated(bert, ernie, theCount);
        verifyZeroInteractions(bertKeyring, ernieKeyring, theCountKeyring);
        verifyZeroInteractions(user, permissions, keyringCache);
    }

    @Test
    public void testAdd_GetUsersReturnsNull_shouldNotRemoveKeyFromAnyone() throws Exception {
        // Given get users returns null
        when(users.list())
                .thenReturn(null);

        // When add is called
        legacyKeyring.add(null, collection, secretKey);

        // Then the key is assigned to the expected users
        verifyKeyAddedToUser(bert, bertKeyring);
        verifyKeyAddedToUser(ernie, ernieKeyring);

        verifyUsersNotUpdated(theCount);
        verifyKeyNotAddedToUser(theCount, theCountKeyring);

        // And the key is not removed from any users
        verify(users, never()).removeKeyFromKeyring(any(), any());

        verify(permissions, times(1)).getCollectionAccessMapping(collection);
        verify(users, times(1)).list();
    }

    @Test
    public void testAdd_GetUsersReturnsEmpty_shouldNotRemoveKeyFromAnyone() throws Exception {
        // Given get users returns null
        when(users.list())
                .thenReturn(new UserList());

        // When add is called
        legacyKeyring.add(null, collection, secretKey);

        // Then the key is assigned to the expected users
        verifyKeyAddedToUser(bert, bertKeyring);
        verifyKeyAddedToUser(ernie, ernieKeyring);

        verifyUsersNotUpdated(theCount);
        verifyKeyNotAddedToUser(theCount, theCountKeyring);

        // And the key is not removed from any users
        verify(users, never()).removeKeyFromKeyring(any(), any());

        verify(permissions, times(1)).getCollectionAccessMapping(collection);
        verify(users, times(1)).list();
    }

    @Test
    public void testAdd_AddKeyToStoredUserError_shouldThrowException() throws Exception {
        // Given add key to stored user throws an exception
        when(users.addKeyToKeyring(EMAIL_BERT, TEST_COLLECTION_ID, secretKey))
                .thenThrow(IOException.class);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class,
                () -> legacyKeyring.add(null, collection, secretKey));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(ADD_KEY_SAVE_ERR));
        assertTrue(actual.getCause() instanceof IOException);

        verify(permissions, times(1)).getCollectionAccessMapping(collection);
        verify(users, times(1)).list();

        verifyKeyAddedToUser(bert, bertKeyring);

        verifyUsersNotUpdated(ernie, theCount);
    }

    @Test
    public void testAdd_RemoveKeyFromStoredUserError_shouldThrowException() throws Exception {
        // Given remove key from stored user throws an exception
        when(users.removeKeyFromKeyring(EMAIL_THE_COUNT, TEST_COLLECTION_ID))
                .thenThrow(IOException.class);

        // When add is called
        KeyringException actual = assertThrows(KeyringException.class,
                () -> legacyKeyring.add(null, collection, secretKey));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(REMOVE_KEY_SAVE_ERR));
        assertTrue(actual.getCause() instanceof IOException);

        verify(permissions, times(1)).getCollectionAccessMapping(collection);
        verify(users, times(1)).list();

        verifyKeyAddedToUser(bert, bertKeyring);
        verifyKeyAddedToUser(ernie, ernieKeyring);

        verifyKeyRemovedFromUser(theCount, theCountKeyring);
        verifyKeyNotRemovedFromUser(bert, bertKeyring);
        verifyKeyNotRemovedFromUser(ernie, ernieKeyring);
    }

    @Test
    public void testAdd_success_shouldAssignAndRemoveKeyFromTheExpectedUsers() throws Exception {
        // Given add is successfull

        // When add key is called
        legacyKeyring.add(null, collection, secretKey);

        // Then the key is assigned to the expected users only
        verifyKeyAddedToUser(bert, bertKeyring);
        verifyKeyAddedToUser(ernie, ernieKeyring);
        verifyKeyNotAddedToUser(theCount, theCountKeyring);
        verify(users, times(1)).addKeyToKeyring(EMAIL_BERT, TEST_COLLECTION_ID, secretKey);
        verify(users, times(1)).addKeyToKeyring(EMAIL_ERNIE, TEST_COLLECTION_ID, secretKey);

        // And the key is removed from the expected users only
        verifyKeyRemovedFromUser(theCount, theCountKeyring);
        verifyKeyNotRemovedFromUser(bert, bertKeyring);
        verifyKeyNotRemovedFromUser(ernie, ernieKeyring);
        verify(users, times(1)).removeKeyFromKeyring(EMAIL_THE_COUNT, TEST_COLLECTION_ID);

        verify(permissions, times(1)).getCollectionAccessMapping(collection);
        verify(users, times(1)).list();
    }
}
