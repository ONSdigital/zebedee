package com.github.onsdigital.zebedee.keyring.legacy;

import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.onsdigital.zebedee.keyring.legacy.LegacyKeyringCacheImpl.COLLECTION_ID_EMPTY;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacyKeyringCacheImpl.SECRET_KEY_EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LegacyKeyringCacheImplTest {

    static final String COLLECTION_ID = "666";

    @Mock
    private SecretKey secretKey;

    private LegacyKeyringCacheImpl keyringCache;
    private ConcurrentHashMap<String, SecretKey> cache;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        cache = new ConcurrentHashMap<String, SecretKey>() {{
            put(COLLECTION_ID, secretKey);
        }};

        keyringCache = new LegacyKeyringCacheImpl(cache);
    }

    @Test
    public void testGet_cacheNull_shouldReturnNull() throws Exception {
        keyringCache = new LegacyKeyringCacheImpl(null);
        SecretKey actual = keyringCache.get(COLLECTION_ID);

        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testGet_cacheEmpty_shouldReturnNull() throws Exception {
        keyringCache = new LegacyKeyringCacheImpl(new ConcurrentHashMap<>());

        SecretKey actual = keyringCache.get(COLLECTION_ID);

        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testGet_entryNotInCache_shouldReturnNull() throws Exception {
        keyringCache = new LegacyKeyringCacheImpl(new ConcurrentHashMap<String, SecretKey>() {{
            put("1234", secretKey);
        }});

        SecretKey actual = keyringCache.get(COLLECTION_ID);

        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testGet_entryExists_shouldReturnKey() throws Exception {
        SecretKey actual = keyringCache.get(COLLECTION_ID);

        assertThat(actual, equalTo(secretKey));
    }

    @Test
    public void testPut_collectionIDNull_shouldThrowException() {
        KeyringException ex = assertThrows(KeyringException.class, () -> keyringCache.add(null, null));

        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY));
    }

    @Test
    public void testAdd_collectionIDEmpty_shouldThrowException() {
        ConcurrentHashMap<String, SecretKey> cache = new ConcurrentHashMap<>();
        keyringCache = new LegacyKeyringCacheImpl(cache);

        KeyringException ex = assertThrows(KeyringException.class, () -> keyringCache.add("", null));

        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testAdd_secretKeyNull_shouldThrowException() {
        ConcurrentHashMap<String, SecretKey> cache = new ConcurrentHashMap<>();
        keyringCache = new LegacyKeyringCacheImpl(cache);

        KeyringException ex = assertThrows(KeyringException.class, () -> keyringCache.add(COLLECTION_ID, null));

        assertThat(ex.getMessage(), equalTo(SECRET_KEY_EMPTY));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testAdd_entryAlreadyExists_shouldReplace() throws Exception {
        assertThat(secretKey, equalTo(cache.get(COLLECTION_ID)));

        SecretKey newkey = mock(SecretKey.class);
        keyringCache.add(COLLECTION_ID, newkey);

        assertThat(newkey, equalTo(cache.get(COLLECTION_ID)));
        assertThat(cache.size(), equalTo(1));
    }

    @Test
    public void testAdd_entryDoesNotAlreadyExists_shouldAddKey() throws Exception {
        cache = new ConcurrentHashMap<>();
        keyringCache = new LegacyKeyringCacheImpl(cache);

        keyringCache.add(COLLECTION_ID, secretKey);

        assertThat(secretKey, equalTo(cache.get(COLLECTION_ID)));
        assertThat(cache.size(), equalTo(1));
    }

    @Test
    public void testLoad_shouldThrowUnsupportedOperationException() throws Exception {
        cache = new ConcurrentHashMap<>();
        keyringCache = new LegacyKeyringCacheImpl(cache);

        assertThrows(UnsupportedOperationException.class, () -> keyringCache.load());
    }

    @Test
    public void testRemove_shouldRemoveItemFromCache() throws Exception {
        cache = mock(ConcurrentHashMap.class);
        keyringCache = new LegacyKeyringCacheImpl(cache);

        keyringCache.remove(COLLECTION_ID);

        verify(cache, times(1)).remove(COLLECTION_ID);
    }

    @Test
    public void testList_shouldListItemsInCache() throws Exception {
        SecretKey key1 = mock(SecretKey.class);
        SecretKey key2 = mock(SecretKey.class);

        cache = new ConcurrentHashMap<String, SecretKey>() {{
            put("1",key1);
            put("2",key2);
        }};

        keyringCache = new LegacyKeyringCacheImpl(cache);

        Set<String> actual = keyringCache.list();

        assertThat(actual, is(notNullValue()));
        assertThat(actual.size(), equalTo(2));
        assertTrue(actual.contains("1"));
        assertTrue(actual.contains("2"));
    }

    @Test
    public void testList_cacheNull_shouldReturnEmpty() throws Exception {
        keyringCache = new LegacyKeyringCacheImpl(null);

        Set<String> actual = keyringCache.list();

        assertThat(actual, is(notNullValue()));
        assertTrue(actual.isEmpty());
    }
}
