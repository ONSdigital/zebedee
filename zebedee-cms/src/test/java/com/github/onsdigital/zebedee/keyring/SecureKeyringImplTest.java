package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.user.model.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.util.HashSet;

import static com.github.onsdigital.zebedee.keyring.SecureKeyringImpl.USER_KEYRING_LOCKED_ERR;
import static com.github.onsdigital.zebedee.keyring.SecureKeyringImpl.USER_KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.SecureKeyringImpl.USER_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SecureKeyringImplTest {

    static final String TEST_COLLECTION_ID = "44";

    private SecureKeyring keyring;

    @Mock
    private Keyring centralKeyring;

    @Mock
    private User user;

    @Mock
    private com.github.onsdigital.zebedee.json.Keyring userKeyring;

    @Mock
    private SecretKey secretKey;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        keyring = new SecureKeyringImpl(centralKeyring);
    }

    @Test
    public void testPopulateFromUser_userNull() {
        // Given the user is null
        // when populateFromUser is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(null));

        // then a Keyring exeption is thrown.
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testPopulateFromUser_userKeyringNull() {
        // Given the user keyring is null
        when(user.keyring())
                .thenReturn(null);

        // When populateFromUser is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then a Keyring exeption is thrown.
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testPopulateFromUser_userKeyringIsLocked() {
        // Given the user keyring is locked
        when(user.keyring())
                .thenReturn(userKeyring);

        when(userKeyring.isUnlocked())
                .thenReturn(false);

        // When populateFromUser is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then a Keyring exeption is thrown.
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_LOCKED_ERR));
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testPopulateFromUser_userKeyringIsEmpty() throws Exception {
        // Given the user keyring is empty
        when(user.keyring())
                .thenReturn(userKeyring);

        when(userKeyring.isUnlocked())
                .thenReturn(true);

        when(userKeyring.list())
                .thenReturn(new HashSet<>());

        // When populateFromUser is called
        keyring.populateFromUser(user);

        // Then the central keyring is not updated.
        verifyZeroInteractions(centralKeyring);
    }

    @Test
    public void testPopulateFromUser_addThrowsException() throws Exception {
        // Given central keyring.add throws an exception
        when(user.keyring())
                .thenReturn(userKeyring);

        when(userKeyring.isUnlocked())
                .thenReturn(true);

        when(userKeyring.list())
                .thenReturn(new HashSet<String>() {{
                    add(TEST_COLLECTION_ID);
                }});

        when(userKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(secretKey);

        doThrow(KeyringException.class)
                .when(centralKeyring)
                .add(any(), any());

        // When populateFromUser is called
        assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then the central keyring is not updated.
        verify(centralKeyring, times(1)).add(any(), any());
    }

    @Test
    public void testPopulateFromUser_success() throws Exception {
        // Given a populated user keyring
        when(user.keyring())
                .thenReturn(userKeyring);

        when(userKeyring.isUnlocked())
                .thenReturn(true);

        when(userKeyring.list())
                .thenReturn(new HashSet<String>() {{
                    add(TEST_COLLECTION_ID);
                }});

        when(userKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(secretKey);

        // When populateFromUser is called
        keyring.populateFromUser(user);

        // Then the central keyring is updated with each entry in the user keyring.
        verify(centralKeyring, times(1)).add(TEST_COLLECTION_ID, secretKey);
    }
}
