package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;

import static com.github.onsdigital.zebedee.keyring.KeyringMigratorImpl.ERR_FMT;
import static com.github.onsdigital.zebedee.keyring.KeyringMigratorImpl.GET_KEY_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigratorImpl.POPULATE_FROM_USER_ERR;
import static com.github.onsdigital.zebedee.keyring.KeyringMigratorImpl.REMOVE_KEY_ERR;
import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class KeyringMigratorImplTest {

    @Mock
    private User user;

    @Mock
    private Collection collection;

    @Mock
    private SecretKey secretKey;

    @Mock
    private Keyring legacyKeyring;

    @Mock
    private Keyring centralKeyring;

    private Keyring keyring;

    private boolean enabled = true;
    private boolean disabled = false;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    private void assertException(KeyringException actual, String expectedMsg, boolean migrationEnabled) {
        String msg = format(ERR_FMT, expectedMsg, migrationEnabled);
        assertThat(actual.getMessage(), equalTo(msg));
        assertThat(actual.getCause(), is(notNullValue()));
    }

    @Test
    public void populateFromUser_migrateDisabled_success_shouldUpdateBothKeyrings() throws Exception {
        // Given keyring migration is disabled
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        // When populate from user is called
        keyring.populateFromUser(user);

        // Then both the legacy and central keyrings are updated
        verify(legacyKeyring, times(1)).populateFromUser(user);
        verify(centralKeyring, times(1)).populateFromUser(user);
    }

    @Test
    public void populateFromUser_migrateEnabled_success_shouldUpdateBothKeyrings() throws Exception {
        // Given keyring migration is enabled
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        // When populate from user is called
        keyring.populateFromUser(user);

        // Then both the legacy and central keyrings are updated
        verify(legacyKeyring, times(1)).populateFromUser(user);
        verify(centralKeyring, times(1)).populateFromUser(user);
    }

    @Test
    public void populateFromUser_migrateDisabled_legacyKeyringException_shouldThrowException() throws Exception {
        // Given keyring migration is disabled
        // And legacy keyring errors when called.
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        doThrow(KeyringException.class)
                .when(legacyKeyring)
                .populateFromUser(user);

        // When populate from user is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown
        assertException(actual, POPULATE_FROM_USER_ERR, disabled);

        // And the legacy keyring is called 1 time
        verify(legacyKeyring, times(1)).populateFromUser(user);

        // And the central keyring is never called
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void populateFromUser_migrateEnabled_legacyKeyringException_shouldThrowException() throws Exception {
        // Given keyring migration is enabled
        // And legacy keyring errors when called.
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        doThrow(KeyringException.class)
                .when(legacyKeyring)
                .populateFromUser(user);

        // When populate from user is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown
        assertException(actual, POPULATE_FROM_USER_ERR, enabled);

        // And the legacy keyring is called 1 time
        verify(legacyKeyring, times(1)).populateFromUser(user);

        // And the central keyring is never called
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void populateFromUser_migrateDisabled_centralKeyringException_shouldThrowException() throws Exception {
        // Given keyring migration is disabled
        // And central keyring errors when called.
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        doThrow(KeyringException.class)
                .when(centralKeyring)
                .populateFromUser(user);

        // When populate from user is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown
        assertException(actual, POPULATE_FROM_USER_ERR, disabled);

        // And the legacy keyring is called 1 time
        verify(legacyKeyring, times(1)).populateFromUser(user);

        // And the central keyring is called 1 time
        verify(centralKeyring, times(1)).populateFromUser(user);
    }

    @Test
    public void populateFromUser_migrateEnabled_centralKeyringException_shouldThrowException() throws Exception {
        // Given keyring migration is enabled
        // And central keyring errors when called.
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        doThrow(KeyringException.class)
                .when(centralKeyring)
                .populateFromUser(user);

        // When populate from user is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown
        assertException(actual, POPULATE_FROM_USER_ERR, enabled);

        // And the legacy keyring is called 1 time
        verify(legacyKeyring, times(1)).populateFromUser(user);

        // And the central keyring is called 1 time
        verify(centralKeyring, times(1)).populateFromUser(user);
    }

    @Test
    public void get_migrateDisabled_success_shouldReadFromLegacyKeyring() throws Exception {
        // Given keyring migration is disabled
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        when(legacyKeyring.get(user, collection))
                .thenReturn(secretKey);

        // When Get is called
        SecretKey actual = keyring.get(user, collection);

        // Then the expected key is returned
        assertThat(actual, equalTo(secretKey));
        verify(legacyKeyring, times(1)).get(user, collection);
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void get_migrateDisabled_keyringError_shouldThrowException() throws Exception {
        // Given keyring migration is disabled
        // And legacy keyring returns an error
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        when(legacyKeyring.get(user, collection))
                .thenThrow(KeyringException.class);

        // When Get is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        // Then an exception is returned
        assertException(actual, GET_KEY_ERR, disabled);

        // And the legacy keyring is called 1 times
        verify(legacyKeyring, times(1)).get(user, collection);

        // And the central keyring is never called
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void get_migrateEnabled_success_shouldReadFromCentralKeyring() throws Exception {
        // Given keyring migration is enabled
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        when(centralKeyring.get(user, collection))
                .thenReturn(secretKey);

        // When Get is called
        SecretKey actual = keyring.get(user, collection);

        // Then the expected key is returned
        assertThat(actual, equalTo(secretKey));
        verify(centralKeyring, times(1)).get(user, collection);
        verifyZeroInteractions(legacyKeyring);
    }

    @Test
    public void get_migrateEnabled_keyringError_shouldThrowException() throws Exception {
        // Given keyring migration is enabled
        // And central keyring returns an error
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        when(centralKeyring.get(user, collection))
                .thenThrow(KeyringException.class);

        // When Get is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.get(user, collection));

        // Then an exception is returned
        assertException(actual, GET_KEY_ERR, enabled);

        // And the central keyring is called 1 times
        verify(centralKeyring, times(1)).get(user, collection);

        // And the legacy keyring is never called
        verifyZeroInteractions(legacyKeyring);
    }

    @Test
    public void remove_migrationDisabled_success_shouldRemoveFromBothKeyrings() throws Exception {
        // Given keyring migration is disbaled
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        // When remove is called
        keyring.remove(user, collection);

        // The central keyring remove is called 1 times
        verify(centralKeyring, times(1)).remove(user, collection);

        // And legracy keyring remove is called 1 time
        verify(legacyKeyring, times(1)).remove(user, collection);
    }

    @Test
    public void remove_migrationEnabled_success_shouldRemoveFromBothKeyrings() throws Exception {
        // Given keyring migration is enabled
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        // When remove is called
        keyring.remove(user, collection);

        // The central keyring remove is called 1 times
        verify(centralKeyring, times(1)).remove(user, collection);

        // And legracy keyring remove is called 1 time
        verify(legacyKeyring, times(1)).remove(user, collection);
    }

    @Test
    public void remove_migrationDisabled_legacyError_shouldThrowException() throws Exception {
        // Given keyring migration is disbaled
        // And legacy keyring returns an error
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        doThrow(KeyringException.class)
                .when(legacyKeyring)
                .remove(user, collection);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertException(actual, REMOVE_KEY_ERR, disabled);

        // And the central keyring is called 1 time.
        verify(centralKeyring, times(1)).remove(user, collection);

        // And legracy keyring is called 1 time
        verify(legacyKeyring, times(1)).remove(user, collection);
    }

    @Test
    public void remove_migrationEnabled_legacyError_shouldThrowException() throws Exception {
        // Given keyring migration is enabled
        // And legacy keyring returns an error
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        doThrow(KeyringException.class)
                .when(legacyKeyring)
                .remove(user, collection);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertException(actual, REMOVE_KEY_ERR, enabled);

        // And the central keyring is called 1 time.
        verify(centralKeyring, times(1)).remove(user, collection);

        // And legracy keyring is called 1 time
        verify(legacyKeyring, times(1)).remove(user, collection);
    }

    @Test
    public void remove_migrationDisabled_centralError_shouldThrowException() throws Exception {
        // Given keyring migration is disbaled
        // And central keyring returns an error
        keyring = new KeyringMigratorImpl(disabled, legacyKeyring, centralKeyring);

        doThrow(KeyringException.class)
                .when(centralKeyring)
                .remove(user, collection);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertException(actual, REMOVE_KEY_ERR, disabled);

        // And the central keyring is called 1 time.
        verify(centralKeyring, times(1)).remove(user, collection);

        // And legracy keyring is never called.
        verifyZeroInteractions(legacyKeyring);
    }

    @Test
    public void remove_migrationEnabled_centralError_shouldThrowException() throws Exception {
        // Given keyring migration is enabled
        // And central keyring returns an error
        keyring = new KeyringMigratorImpl(enabled, legacyKeyring, centralKeyring);

        doThrow(KeyringException.class)
                .when(centralKeyring)
                .remove(user, collection);

        // When remove is called
        KeyringException actual = assertThrows(KeyringException.class, () -> keyring.remove(user, collection));

        // Then an exception is thrown
        assertException(actual, REMOVE_KEY_ERR, enabled);

        // And the central keyring is called 1 time.
        verify(centralKeyring, times(1)).remove(user, collection);

        // And legracy keyring is never called.
        verifyZeroInteractions(legacyKeyring);
    }
}
