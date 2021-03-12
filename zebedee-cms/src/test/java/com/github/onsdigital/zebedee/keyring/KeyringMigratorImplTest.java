package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class KeyringMigratorImplTest {

    @Mock
    private User user;

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
        assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown
        // And the legacy keyring is called 1 time
        // And the central keyring is never called
        verify(legacyKeyring, times(1)).populateFromUser(user);
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
        assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown
        // And the legacy keyring is called 1 time
        // And the central keyring is never called
        verify(legacyKeyring, times(1)).populateFromUser(user);
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
        assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown
        // And the legacy keyring is called 1 time
        // And the central keyring is called 1 time
        verify(legacyKeyring, times(1)).populateFromUser(user);
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
        assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then an exception is thrown
        // And the legacy keyring is called 1 time
        // And the central keyring is called 1 time
        verify(legacyKeyring, times(1)).populateFromUser(user);
        verify(centralKeyring, times(1)).populateFromUser(user);
    }
}
