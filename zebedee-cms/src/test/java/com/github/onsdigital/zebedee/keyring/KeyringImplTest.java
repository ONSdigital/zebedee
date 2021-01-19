package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.keyring.io.CollectionKeyReadWriter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.onsdigital.zebedee.keyring.KeyringImpl.INVALID_COLLECTION_ID_ERR_MSG;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.INVALID_SECRET_KEY_ERR_MSG;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.KEY_MISMATCH_ERR_MSG;
import static com.github.onsdigital.zebedee.keyring.KeyringImpl.KEY_NOT_FOUND_ERR_MSG;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KeyringImplTest {

    private static final String TEST_COLLECTION_ID = "138"; // We are 138! We are 138 \m/

    private Keyring keyring;
    private ConcurrentHashMap<String, CollectionKey> cache;

    @Mock
    private CollectionKeyReadWriter collectionKeyReadWriter;

    @Mock
    private SecretKey secretKey;

    private CollectionKey collectionKey;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        this.cache = new ConcurrentHashMap<>();
        this.collectionKey = new CollectionKey(TEST_COLLECTION_ID, secretKey);
        this.keyring = new KeyringImpl(collectionKeyReadWriter, cache);
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
            assertTrue(cache.isEmpty());
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testAdd_collectionKeyWriterError_shouldThrowException() throws Exception {
        doThrow(KeyringException.class)
                .when(collectionKeyReadWriter)
                .write(collectionKey);

        try {
            keyring.add(TEST_COLLECTION_ID, secretKey);
        } catch (KeyringException ex) {
            verify(collectionKeyReadWriter, times(1)).write(collectionKey);

            assertTrue(cache.isEmpty());
            throw ex;
        }
    }

    @Test
    public void testAdd_duplicateEntry_shouldDoNothing() throws Exception {
        cache.put(collectionKey.getCollectionID(), collectionKey);
        assertThat(cache.size(), equalTo(1));

        keyring.add(collectionKey.getCollectionID(), collectionKey.getSecretKey());

        assertThat(cache.size(), equalTo(1));
        verify(collectionKeyReadWriter, never()).write(collectionKey);
    }

    @Test(expected = KeyringException.class)
    public void testAdd_duplicateEntryDifferentKeyValue_shouldThrowException() throws Exception {
        cache.put(collectionKey.getCollectionID(), collectionKey);
        assertThat(cache.size(), equalTo(1));

        try {
            keyring.add(collectionKey.getCollectionID(), mock(SecretKey.class));
        } catch (KeyringException ex) {
            assertThat(cache.size(), equalTo(1));
            assertThat(cache.get(TEST_COLLECTION_ID).getSecretKey(), equalTo(secretKey));

            verify(collectionKeyReadWriter, never()).write(collectionKey);
            assertThat(ex.getMessage(), equalTo(KEY_MISMATCH_ERR_MSG));
            assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
            throw ex;
        }
    }

    @Test
    public void testAdd_success_shouldCallReadWriterAndPutKeyInCache() throws Exception {
        keyring.add(TEST_COLLECTION_ID, secretKey);

        assertFalse(cache.isEmpty());
        assertThat(cache.size(), equalTo(1));
        assertThat(cache.get(TEST_COLLECTION_ID), equalTo(collectionKey));

        verify(collectionKeyReadWriter, times(1)).write(collectionKey);
    }


    /**
     * Test Add with x concurrent requests. Verifies that the the cache contains the correct/expected values when
     * being access by several threads at the same time.
     *
     * @throws Exception test failed...obviously
     */
    @Test
    public void testAdd_concurrent_shouldAddAllKeys() throws Exception {
        KeyGenerator gen = KeyGenerator.getInstance("AES");

        int concurrentAddsCount = 50;

        Map<String, CollectionKey> keysAddedConcurrently = new HashMap<>();

        // create a list of concurrent add key tasks and keep and note of the collectionID -> Key mappings.
        List<Callable<String>> tasks = new ArrayList<>();
        for (int id = 0; id < concurrentAddsCount; id++) {
            SecretKey key = gen.generateKey();
            String collectionID = String.valueOf(id);

            keysAddedConcurrently.put(collectionID, new CollectionKey(collectionID, key));

            tasks.add(() -> {
                keyring.add(collectionID, key);
                return collectionID;
            });
        }

        // create a thread executor and submit all the add key tasks concurrently.
        ExecutorService executor = Executors.newFixedThreadPool(concurrentAddsCount);
        List<Future<String>> results = executor.invokeAll(tasks);

        assertFalse(cache.isEmpty());
        assertThat(cache.size(), equalTo(concurrentAddsCount));
        verify(collectionKeyReadWriter, times(concurrentAddsCount)).write(any(CollectionKey.class));

        for (Map.Entry<String, CollectionKey> entry : keysAddedConcurrently.entrySet()) {
            String id = entry.getKey();

            CollectionKey expected = keysAddedConcurrently.get(id);
            CollectionKey actual = cache.get(id);

            verify(collectionKeyReadWriter, times(1)).write(expected);
            assertThat(actual, is(notNullValue()));
            assertThat(actual, equalTo(expected));
        }
    }

    @Test(expected = KeyringException.class)
    public void testGet_collectionIDNull_shouldThrowException() throws Exception {
        try {
            keyring.get(null);
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR_MSG));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testGet_collectionIDEmpty_shouldThrowException() throws Exception {
        try {
            keyring.get("");
        } catch (KeyringException ex) {
            assertThat(ex.getMessage(), equalTo(INVALID_COLLECTION_ID_ERR_MSG));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testGet_keyNotExist_shouldThrowException() throws Exception {
        when(collectionKeyReadWriter.read(TEST_COLLECTION_ID))
                .thenReturn(null);

        try {
            keyring.get(TEST_COLLECTION_ID);
        } catch (KeyringException ex) {
            verify(collectionKeyReadWriter, times(1)).read(TEST_COLLECTION_ID);

            assertThat(ex.getMessage(), equalTo(KEY_NOT_FOUND_ERR_MSG));
            throw ex;
        }
    }

    @Test(expected = KeyringException.class)
    public void testGet_secretKeyValueNull_shouldThrowException() throws Exception {
        when(collectionKeyReadWriter.read(TEST_COLLECTION_ID))
                .thenReturn(new CollectionKey(TEST_COLLECTION_ID, null));

        try {
            keyring.get(TEST_COLLECTION_ID);
        } catch (KeyringException ex) {
            verify(collectionKeyReadWriter, times(1)).read(TEST_COLLECTION_ID);

            assertThat(ex.getMessage(), equalTo(INVALID_SECRET_KEY_ERR_MSG));
            throw ex;
        }
    }

    @Test
    public void testGet_keyNotExistInCache_shouldReadKeyFromDisk() throws Exception {
        when(collectionKeyReadWriter.read(TEST_COLLECTION_ID))
                .thenReturn(collectionKey);

        SecretKey actual = keyring.get(TEST_COLLECTION_ID);

        assertThat(actual, equalTo(secretKey));
        verify(collectionKeyReadWriter, times(1)).read(TEST_COLLECTION_ID);
    }

    @Test
    public void testGet_keyExistInCache_shouldReturnCachedValue() throws Exception {
        cache.put(TEST_COLLECTION_ID, collectionKey);

        SecretKey actual = keyring.get(TEST_COLLECTION_ID);

        assertThat(actual, equalTo(secretKey));
        verify(collectionKeyReadWriter, never()).read(TEST_COLLECTION_ID);
    }

}
