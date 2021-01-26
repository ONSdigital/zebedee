package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.keyring.store.CollectionKeyStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;

import static com.github.onsdigital.zebedee.keyring.KeyringImpl.INVALID_COLLECTION_ID_ERR_MSG;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.INVALID_SECRET_KEY_ERR_MSG;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.KEY_MISMATCH_ERR_MSG;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class KeyringImplTest {

    private static final String TEST_COLLECTION_ID = "138"; // We are 138! We are 138 \m/

    private Keyring keyring;
    private Map<String, SecretKey> cache;

    @Mock
    private CollectionKeyStore keyStore;

    @Mock
    private SecretKey secretKey;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.cache = new HashMap<>();
        this.keyring = new KeyringImpl(keyStore, cache);
    }

    @Test(expected = KeyringException.class)
    public void testAdd_collectionIDNull_shouldThrowException() throws Exception {
        try {
            keyring.add(null, null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR_MSG));
            assertTrue(cache.isEmpty());
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testAdd_collectionIDEmpty_shouldThrowException() throws Exception {
        try {
            keyring.add("", null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR_MSG));
            assertTrue(cache.isEmpty());
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testAdd_secretKeyNull_shouldThrowException() throws Exception {
        try {
            keyring.add(TEST_COLLECTION_ID, null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_SECRET_KEY_ERR_MSG));
            assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
            assertTrue(cache.isEmpty());
            throw ex;
        }
    }

    @Test
    public void testAdd_keyExistsInCache_shouldDoNothing() throws Exception {
        cache.put(TEST_COLLECTION_ID, secretKey);
        assertThat(cache.size(), equalTo(1));

        keyring.add(TEST_COLLECTION_ID, secretKey);

        assertThat(cache.size(), equalTo(1));
        assertTrue(cache.containsKey(TEST_COLLECTION_ID));
        assertThat(cache.get(TEST_COLLECTION_ID), equalTo(secretKey));
        verifyZeroInteractions(keyStore);
    }

    @Test(expected = KeyringException.class)
    public void testAdd_keyWithDiffentValueExistsInCache_shouldThrowException() throws Exception {
        SecretKey key2 = mock(SecretKey.class);
        cache.put(TEST_COLLECTION_ID, key2);

        try {
            keyring.add(TEST_COLLECTION_ID, secretKey);
        } catch (KeyringException ex) {
            verifyZeroInteractions(keyStore);
            assertThat(cache.size(), equalTo(1));
            assertThat(ex.getMessage(), equalTo(KEY_MISMATCH_ERR_MSG));
            assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testAdd_keystoreThrowsException_shouldThrowException() throws Exception {
        doThrow(KeyringException.class)
                .when(keyStore)
                .write(TEST_COLLECTION_ID, secretKey);
        try {
            keyring.add(TEST_COLLECTION_ID, secretKey);
        } catch (KeyringException ex) {
            verify(keyStore, times(1)).write(TEST_COLLECTION_ID, secretKey);
            assertTrue(cache.isEmpty());
            throw ex;
        }
    }

    @Test
    public void testAdd_keyNotInCacheButExistsInStore_shouldAddKeyToCache() throws Exception {
        when(keyStore.exists(TEST_COLLECTION_ID))
                .thenReturn(true);

        when(keyStore.read(TEST_COLLECTION_ID))
                .thenReturn(secretKey);

        keyring.add(TEST_COLLECTION_ID, secretKey);

        assertTrue(cache.containsKey(TEST_COLLECTION_ID));
        assertThat(cache.get(TEST_COLLECTION_ID), equalTo(secretKey));
    }

    @Test(expected = KeyringException.class)
    public void testAdd_keyNotInCacheKeyStoreExistsError_shouldThrowException() throws Exception {
        when(keyStore.exists(TEST_COLLECTION_ID))
                .thenReturn(true);

        when(keyStore.read(TEST_COLLECTION_ID))
                .thenThrow(new KeyringException("unexpected error"));

        try {
            keyring.add(TEST_COLLECTION_ID, secretKey);
        } catch (KeyringException ex) {
            assertTrue(cache.isEmpty());
            assertThat(ex.getMessage(), equalTo("unexpected error"));

            verify(keyStore, times(1)).exists(TEST_COLLECTION_ID);
            verify(keyStore, times(1)).read(TEST_COLLECTION_ID);
            verifyNoMoreInteractions(keyStore);
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testAdd_keystoreKeyMismatch_shouldThrowException() throws Exception {
        when(keyStore.exists(TEST_COLLECTION_ID))
                .thenReturn(true);

        when(keyStore.read(TEST_COLLECTION_ID))
                .thenReturn(mock(SecretKey.class));

        try {
            keyring.add(TEST_COLLECTION_ID, secretKey);
        } catch (KeyringException ex) {
            assertTrue(cache.isEmpty());
            assertThat(ex.getMessage(), equalTo(KEY_MISMATCH_ERR_MSG));
            assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));

            verify(keyStore, times(1)).exists(TEST_COLLECTION_ID);
            verify(keyStore, times(1)).read(TEST_COLLECTION_ID);
            verifyNoMoreInteractions(keyStore);
            throw ex;
        }
    }

    @Test
    public void testAdd_success_shouldWriteKeyToStoreAndCache() throws Exception {
        keyring.add(TEST_COLLECTION_ID, secretKey);

        verify(keyStore, times(1)).write(TEST_COLLECTION_ID, secretKey);
        assertThat(cache.size(), equalTo(1));
        assertTrue(cache.containsKey(TEST_COLLECTION_ID));
        assertThat(cache.get(TEST_COLLECTION_ID), equalTo(secretKey));
    }
}
