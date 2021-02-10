package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.keyring.cache.KeyringCache;
import com.github.onsdigital.zebedee.user.model.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.util.HashSet;

import static com.github.onsdigital.zebedee.keyring.CollectionKeyringImpl.NOT_INITALISED_ERR;
import static com.github.onsdigital.zebedee.keyring.CollectionKeyringImpl.USER_KEYRING_LOCKED_ERR;
import static com.github.onsdigital.zebedee.keyring.CollectionKeyringImpl.USER_KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.CollectionKeyringImpl.USER_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CollectionKeyringImplTest {

    static final String TEST_COLLECTION_ID = "44";

    private CollectionKeyring keyring;

    @Mock
    private KeyringCache keyringCache;

    @Mock
    private User user;

    @Mock
    private com.github.onsdigital.zebedee.json.Keyring userKeyring;

    @Mock
    private SecretKey secretKey;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        CollectionKeyringImpl.init(keyringCache);
        keyring = CollectionKeyringImpl.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        resetInstanceToNull();
    }

    @Test
    public void testPopulateFromUser_userNull() throws KeyringException {
        // Given the user is null
        // when populateFromUser is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(null));

        // then a Keyring exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(keyringCache);
    }

    @Test
    public void testPopulateFromUser_userKeyringNull() throws Exception {
        // Given the user keyring is null
        when(user.keyring())
                .thenReturn(null);

        // When populateFromUser is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then a Keyring exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
        verifyZeroInteractions(keyringCache);
    }

    @Test
    public void testPopulateFromUser_userKeyringIsLocked() throws Exception {
        // Given the user keyring is locked
        when(user.keyring())
                .thenReturn(userKeyring);

        when(userKeyring.isUnlocked())
                .thenReturn(false);

        // When populateFromUser is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then a Keyring exception is thrown.
        assertThat(ex.getMessage(), equalTo(USER_KEYRING_LOCKED_ERR));
        verifyZeroInteractions(keyringCache);
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
        verifyZeroInteractions(keyringCache);
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
                .when(keyringCache)
                .add(any(), any());

        // When populateFromUser is called
        assertThrows(KeyringException.class, () -> keyring.populateFromUser(user));

        // Then the central keyring is not updated.
        verify(keyringCache, times(1)).add(any(), any());
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
        verify(keyringCache, times(1)).add(TEST_COLLECTION_ID, secretKey);
    }

    @Test
    public void testGetInstance_notInitialised() throws Exception {
        // Given CollectionKeyring has not been initalised
        resetInstanceToNull();

        // When GetInstance is called
        // Then an exception is thrown
        KeyringException ex = assertThrows(KeyringException.class, () -> CollectionKeyringImpl.getInstance());
        assertThat(ex.getMessage(), equalTo(NOT_INITALISED_ERR));
    }

    @Test
    public void testGetInstance_success() throws KeyringException {
        // Given CollectionKeyring has been initalised

        // When GetInstance is called
        CollectionKeyring collectionKeyring = CollectionKeyringImpl.getInstance();

        // Then a non null instance is returned
        assertThat(collectionKeyring, is(notNullValue()));
    }

    @Test
    public void testInit_keyringCacheNull() throws Exception {
        // Given keyringCache is null
        KeyringCache keyringCache = null;

        resetInstanceToNull();

        // When init is called
        KeyringException ex = assertThrows(KeyringException.class, () -> CollectionKeyringImpl.init(keyringCache));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo("keyringCache required but was null"));
    }

    private void resetInstanceToNull() throws Exception {
        // Use some evil reflection magic to set the instance back to null for this test case.
        Field field = CollectionKeyringImpl.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        field.set(null, null);
    }
}
