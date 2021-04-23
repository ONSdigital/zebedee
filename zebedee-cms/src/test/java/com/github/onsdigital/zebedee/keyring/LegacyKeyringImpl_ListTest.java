package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.EMAIL_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.GET_USER_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.USER_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LegacyKeyringImpl_ListTest extends BaseLegacyKeyringTest {

    @Override
    public void setUpTests() throws Exception {

    }

    @Test
    public void testList_userNull_shouldThrowException() {
        // Given user is null

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.list(null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testList_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(bert.getEmail())
                .thenReturn(null);

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.list(bert));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testList_userEmailEmpty_shouldThrowException() {
        // Given user email is null
        when(bert.getEmail())
                .thenReturn("");

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.list(bert));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(keyringCache, users);
    }

    @Test
    public void testList_cacheKeyringExists_shouldListCachedKeyringEntries() throws Exception {
        // Given the users keyring exists in the cache
        Set<String> expected = new HashSet<String>() {{
            add(TEST_COLLECTION_ID);
        }};

        when(bertKeyring.list())
                .thenReturn(expected);

        // When list is called
        Set<String> actual = legacyKeyring.list(bert);

        // Then the expected set is returned
        assertThat(actual, equalTo(expected));
        verify(keyringCache, times(1)).get(bert);
        verify(bertKeyring, times(1)).list();
        verifyZeroInteractions(users);
    }

    @Test
    public void testList_cacheKeyringDoesNotExistUsersGetError_shouldThrowException() throws Exception {
        // Given the users keyring does not exists in the cache
        // And get user throws an exception
        when(keyringCache.get(bert))
                .thenReturn(null);

        when(users.getUserByEmail(EMAIL_BERT))
                .thenThrow(IOException.class);

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.list(bert));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(GET_USER_ERR));
        verify(keyringCache, times(1)).get(bert);
        verify(users, times(1)).getUserByEmail(EMAIL_BERT);
        verifyZeroInteractions(bertKeyring);
    }

    @Test
    public void testList_getUserReturnsNull_shouldThrowException() throws Exception {
        // Given the users get user returns null
        when(keyringCache.get(bert))
                .thenReturn(null);

        when(users.getUserByEmail(EMAIL_BERT))
                .thenReturn(null);

        // When list is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.list(bert));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(USER_NULL_ERR));
        verify(keyringCache, times(1)).get(bert);
        verify(users, times(1)).getUserByEmail(EMAIL_BERT);
        verifyZeroInteractions(bertKeyring);
    }

    @Test
    public void testList_storedUserKeyringNull_shouldReturnEmptyResult() throws Exception {
        // Given the stored users keyring is null
        when(keyringCache.get(bert))
                .thenReturn(null);

        User storedUser = mock(User.class);
        when(users.getUserByEmail(EMAIL_BERT))
                .thenReturn(storedUser);

        when(storedUser.keyring())
                .thenReturn(null);

        // When list is called
        Set<String> actual = legacyKeyring.list(bert);

        // Then an empty set is returned
        assertTrue(actual.isEmpty());
        verify(keyringCache, times(1)).get(bert);
        verify(users, times(1)).getUserByEmail(EMAIL_BERT);
        verifyZeroInteractions(bertKeyring);
    }

    @Test
    public void testList_storedUserSuccess_shouldReturnKeys() throws Exception {
        // Given the stored users keyring is not null
        when(keyringCache.get(bert))
                .thenReturn(null);

        User storedUser = mock(User.class);
        when(users.getUserByEmail(EMAIL_BERT))
                .thenReturn(storedUser);

        when(storedUser.keyring())
                .thenReturn(bertKeyring);

        Set<String> expected = new HashSet<String>() {{
            add(TEST_COLLECTION_ID);
        }};

        when(bertKeyring.list())
                .thenReturn(expected);

        // When list is called
        Set<String> actual = legacyKeyring.list(bert);

        // Then the expected result is returned
        assertThat(actual, equalTo(expected));
        verify(keyringCache, times(1)).get(bert);
        verify(users, times(1)).getUserByEmail(EMAIL_BERT);
        verify(bertKeyring,times(1)).list();
    }
}
