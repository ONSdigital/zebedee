package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.CACHE_KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.EMAIL_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.KEYRING_LOCKED_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.SAVE_USER_KEYRING_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.USER_KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.USER_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LegacyKeyringImpl_PopulateTest extends BaseLegacyKeyringTest {

    @Mock
    private User srcUser, targeUser;

    @Mock
    private Keyring srcKeyring, targetKeyring, srcCachedKeyring;

    private Set<String> collectionIDs;

    @Override
    public void setUpTests() throws Exception {
        when(srcUser.getEmail())
                .thenReturn("src@test.com");

        when(srcUser.keyring())
                .thenReturn(srcKeyring);

        when(keyringCache.get(srcUser))
                .thenReturn(srcCachedKeyring);

        when(srcCachedKeyring.isUnlocked())
                .thenReturn(true);

        when(srcCachedKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(secretKey);

        when(targeUser.getEmail())
                .thenReturn("target@test.com");

        when(targeUser.keyring())
                .thenReturn(targetKeyring);

        collectionIDs = new HashSet<String>() {{
            add(TEST_COLLECTION_ID);
        }};
    }

    @Test
    public void testPopulate_srcUserNull_ShouldThrowException() {
        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.populate(null, null, null));

        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testPopulate_srcUserEmailNull_ShouldThrowException() {
        when(srcUser.getEmail())
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.populate(srcUser, null, null));

        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testPopulate_srcUserEmailEmpty_ShouldThrowException() {
        when(srcUser.getEmail())
                .thenReturn("");

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.populate(srcUser, null, null));

        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testPopulate_srcUserKeyringNull_ShouldThrowException() {
        when(srcUser.keyring())
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.populate(srcUser, null, null));

        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
    }

    @Test
    public void testPopulate_srcUserKeyringNotInCache_ShouldThrowException() throws Exception {
        when(keyringCache.get(srcUser))
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.populate(srcUser, null, null));

        assertThat(ex.getMessage(), equalTo(CACHE_KEYRING_NULL_ERR));
        verify(keyringCache, times(1)).get(srcUser);
    }

    @Test
    public void testPopulate_srcKeyringLocked_ShouldThrowException() throws Exception {
        when(srcCachedKeyring.isUnlocked())
                .thenReturn(false);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.populate(srcUser, null, null));

        assertThat(ex.getMessage(), equalTo(KEYRING_LOCKED_ERR));
        verify(keyringCache, times(1)).get(srcUser);
    }

    @Test
    public void testPopulate_targetUserNull_ShouldThrowException() {
        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.populate(srcUser, null, null));

        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testPopulate_targetUserEmailNull_ShouldThrowException() {
        when(targeUser.getEmail())
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.populate(srcUser, targeUser, null));

        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testPopulate_targetUserEmailEmpty_ShouldThrowException() {
        when(targeUser.getEmail())
                .thenReturn("");

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.populate(srcUser, targeUser, null));

        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testPopulate_targetUserKeyringNull_ShouldThrowException() {
        when(targeUser.keyring())
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.populate(srcUser, targeUser, null));

        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
    }

    @Test
    public void testPopulate_collectionIdsNull_shouldDoNothing() throws Exception {
        legacyKeyring.populate(srcUser, targeUser, null);

        verifyZeroInteractions(srcKeyring, targetKeyring);
    }

    @Test
    public void testPopulate_collectionIdsEmpty_shouldDoNothing() throws Exception {
        legacyKeyring.populate(srcUser, targeUser, new HashSet<>());

        verifyZeroInteractions(srcKeyring, targetKeyring);
    }

    @Test
    public void testPopulate_updateUserKeyringError_shouldThrowException() throws Exception {
        when(users.updateKeyring(targeUser))
                .thenThrow(IOException.class);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.populate(srcUser, targeUser, collectionIDs));

        assertThat(ex.getMessage(), equalTo(SAVE_USER_KEYRING_ERR));
        verify(keyringCache, times(1)).get(srcUser);
        verify(srcCachedKeyring, times(1)).get(TEST_COLLECTION_ID);
        verify(targetKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);
        verify(users, times(1)).updateKeyring(targeUser);
    }

    @Test
    public void testPopulate_success_shouldAssignExpectedKeys() throws Exception {
        legacyKeyring.populate(srcUser, targeUser, collectionIDs);

        verify(keyringCache, times(1)).get(srcUser);
        verify(srcCachedKeyring, times(1)).get(TEST_COLLECTION_ID);
        verify(targetKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);
        verify(users, times(1)).updateKeyring(targeUser);
    }

}
