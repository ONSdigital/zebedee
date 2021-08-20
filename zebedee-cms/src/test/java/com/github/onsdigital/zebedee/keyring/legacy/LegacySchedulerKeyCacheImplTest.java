package com.github.onsdigital.zebedee.keyring.legacy;

import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.onsdigital.zebedee.keyring.legacy.LegacySchedulerKeyCacheImpl.COLLECTION_ID_EMPTY;
import static com.github.onsdigital.zebedee.keyring.legacy.LegacySchedulerKeyCacheImpl.SECRET_KEY_EMPTY;
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

public class LegacySchedulerKeyCacheImplTest {

    static final String COLLECTION_ID = "666";

    @Mock
    private SecretKey secretKey;

    private LegacySchedulerKeyCacheImpl schedulerCache;
    private ConcurrentHashMap<String, SecretKey> cache;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        cache = new ConcurrentHashMap<String, SecretKey>() {{
            put(COLLECTION_ID, secretKey);
        }};

        schedulerCache = new LegacySchedulerKeyCacheImpl(cache);
    }

    @Test
    public void testGet_cacheNull_shouldReturnNull() throws Exception {
        schedulerCache = new LegacySchedulerKeyCacheImpl(null);
        SecretKey actual = schedulerCache.get(COLLECTION_ID);

        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testGet_cacheEmpty_shouldReturnNull() throws Exception {
        schedulerCache = new LegacySchedulerKeyCacheImpl(new ConcurrentHashMap<>());

        SecretKey actual = schedulerCache.get(COLLECTION_ID);

        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testGet_entryNotInCache_shouldReturnNull() throws Exception {
        schedulerCache = new LegacySchedulerKeyCacheImpl(new ConcurrentHashMap<String, SecretKey>() {{
            put("1234", secretKey);
        }});

        SecretKey actual = schedulerCache.get(COLLECTION_ID);

        assertThat(actual, is(nullValue()));
    }

    @Test
    public void testGet_entryExists_shouldReturnKey() throws Exception {
        SecretKey actual = schedulerCache.get(COLLECTION_ID);

        assertThat(actual, equalTo(secretKey));
    }

    @Test
    public void testPut_collectionIDNull_shouldThrowException() {
        KeyringException ex = assertThrows(KeyringException.class, () -> schedulerCache.add(null, null));

        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY));
    }

    @Test
    public void testAdd_collectionIDEmpty_shouldThrowException() {
        ConcurrentHashMap<String, SecretKey> cache = new ConcurrentHashMap<>();
        schedulerCache = new LegacySchedulerKeyCacheImpl(cache);

        KeyringException ex = assertThrows(KeyringException.class, () -> schedulerCache.add("", null));

        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_EMPTY));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testAdd_secretKeyNull_shouldThrowException() {
        ConcurrentHashMap<String, SecretKey> cache = new ConcurrentHashMap<>();
        schedulerCache = new LegacySchedulerKeyCacheImpl(cache);

        KeyringException ex = assertThrows(KeyringException.class, () -> schedulerCache.add(COLLECTION_ID, null));

        assertThat(ex.getMessage(), equalTo(SECRET_KEY_EMPTY));
        assertTrue(cache.isEmpty());
    }

    @Test
    public void testAdd_entryAlreadyExists_shouldReplace() throws Exception {
        assertThat(secretKey, equalTo(cache.get(COLLECTION_ID)));

        SecretKey newkey = mock(SecretKey.class);
        schedulerCache.add(COLLECTION_ID, newkey);

        assertThat(newkey, equalTo(cache.get(COLLECTION_ID)));
        assertThat(cache.size(), equalTo(1));
    }

    @Test
    public void testAdd_entryDoesNotAlreadyExists_shouldAddKey() throws Exception {
        cache = new ConcurrentHashMap<>();
        schedulerCache = new LegacySchedulerKeyCacheImpl(cache);

        schedulerCache.add(COLLECTION_ID, secretKey);

        assertThat(secretKey, equalTo(cache.get(COLLECTION_ID)));
        assertThat(cache.size(), equalTo(1));
    }

    @Test
    public void testLoad_shouldThrowUnsupportedOperationException() throws Exception {
        schedulerCache = new LegacySchedulerKeyCacheImpl();

        assertThrows(UnsupportedOperationException.class, () -> schedulerCache.load());
    }

    @Test
    public void testRemove_shouldRemoveItemFromCache() throws Exception {
        ConcurrentHashMap cache = mock(ConcurrentHashMap.class);
        schedulerCache = new LegacySchedulerKeyCacheImpl(cache);

        schedulerCache.remove(COLLECTION_ID);

        verify(cache, times(1)).remove(COLLECTION_ID);
    }

    @Test
    public void testRemove_cacheNull_shouldDoNothing() throws Exception {
        schedulerCache = new LegacySchedulerKeyCacheImpl(null);
        schedulerCache.remove(COLLECTION_ID);
    }

    @Test
    public void testList_cacheNullShouldReturnEmpty() throws Exception {
        schedulerCache = new LegacySchedulerKeyCacheImpl(null);

        Set<String> actual = schedulerCache.list();

        assertThat(actual, is(notNullValue()));
        assertTrue(actual.isEmpty());
    }

    @Test
    public void testList_successShouldReturnExpectedValues() throws Exception {
        SecretKey k1 = mock(SecretKey.class);
        SecretKey k2 = mock(SecretKey.class);

        cache = new ConcurrentHashMap<String, SecretKey>() {{
            put("1", k1);
            put("2", k2);
        }};

        schedulerCache = new LegacySchedulerKeyCacheImpl(cache);

        Set<String> actual = schedulerCache.list();

        assertThat(actual, is(notNullValue()));
        assertThat(actual.size(), equalTo(2));
        assertTrue(actual.contains("1"));
        assertTrue(actual.contains("2"));
    }
}
