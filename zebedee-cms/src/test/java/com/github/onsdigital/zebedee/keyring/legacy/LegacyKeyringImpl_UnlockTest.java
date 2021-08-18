package com.github.onsdigital.zebedee.keyring.legacy;

import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.keyring.legacy.BaseLegacyKeyringTest;
import org.junit.Test;

import static com.github.onsdigital.zebedee.keyring.legacy.LegacyKeyringImpl.EMAIL_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyKeyringImpl.PASSWORD_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyKeyringImpl.UNLOCK_KEYRING_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyKeyringImpl.USER_KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyKeyringImpl.USER_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LegacyKeyringImpl_UnlockTest extends BaseLegacyKeyringTest {

    @Override
    public void setUpTests() throws Exception {
    }

    @Test
    public void testUnlock_userNull_shouldThrowException() {
        // Given user is null

        // When unlock is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.unlock(null, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(USER_NULL_ERR));
    }

    @Test
    public void testUnlock_userEmailNull_shouldThrowException() {
        // Given user email is null
        when(bert.getEmail())
                .thenReturn(null);

        // When unlock is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.unlock(bert, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testUnlock_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(bert.getEmail())
                .thenReturn("");

        // When unlock is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.unlock(bert, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(EMAIL_EMPTY_ERR));
    }

    @Test
    public void testUnlock_userKeyringNull_shouldThrowException() {
        // Given user keyring is null
        when(bert.keyring())
                .thenReturn(null);

        // When unlock is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.unlock(bert, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
    }

    @Test
    public void testUnlock_passwordNull_shouldThrowException() {
        // Given password is null

        // When unlock is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.unlock(bert, null));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(PASSWORD_EMPTY_ERR));
        verifyZeroInteractions(bertKeyring);
    }

    @Test
    public void testUnlock_unlockFailed_shouldThrowException() {
        // Given unlocking the user keying is unsuccessful
        when(bertKeyring.unlock(TEST_PASSWORD))
                .thenReturn(false);

        // When unlock is called
        KeyringException actual = assertThrows(KeyringException.class, () -> legacyKeyring.unlock(bert, TEST_PASSWORD));

        // Then an exception is thrown
        assertThat(actual.getMessage(), equalTo(UNLOCK_KEYRING_ERR));
        verify(bertKeyring, times(1)).unlock(TEST_PASSWORD);
    }

    @Test
    public void testUnlock_success_shouldUnlockTheUserKeyring() throws Exception {
        // Given unlocking the keyring is successful

        // When unlock is called
        legacyKeyring.unlock(bert, TEST_PASSWORD);

        // Then no error is returned
        // And the user keyring is unlocked
        verify(bertKeyring, times(1)).unlock(TEST_PASSWORD);
    }
}
