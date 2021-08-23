package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.zebedee.keyring.CollectionKeyCache;
import com.github.onsdigital.zebedee.keyring.KeyNotFoundException;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MigrationCollectionKeyCacheImplTest {

    private static final String COLLECTION_ID = "138";

    @Mock
    private CollectionKeyCache legacyCache, newCache;

    @Mock
    private SecretKey key;

    private CollectionKeyCache cache;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGet_migrateDisabled_shouldGetFromLegacy() throws Exception {
        when(legacyCache.get(COLLECTION_ID))
                .thenReturn(key);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, false);

        SecretKey actual = cache.get(COLLECTION_ID);

        assertThat(actual, is(notNullValue()));
        assertThat(actual, equalTo(key));
        verify(legacyCache, times(1)).get(COLLECTION_ID);
        verifyZeroInteractions(newCache);
    }

    @Test
    public void testGet_migrateDisabled_legacyCacheKeyringEx() throws Exception {
        when(legacyCache.get(COLLECTION_ID))
                .thenThrow(KeyringException.class);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, false);

        assertThrows(KeyringException.class, () -> cache.get(COLLECTION_ID));

        verify(legacyCache, times(1)).get(COLLECTION_ID);
        verifyZeroInteractions(newCache);
    }

    @Test
    public void testGet_migrateEnabled_existsInNewCache() throws Exception {
        when(newCache.get(COLLECTION_ID))
                .thenReturn(key);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, true);

        SecretKey actual = cache.get(COLLECTION_ID);

        assertThat(actual, is(notNullValue()));
        assertThat(actual, equalTo(key));
        verify(newCache, times(1)).get(COLLECTION_ID);
        verifyZeroInteractions(legacyCache);
    }

    @Test
    public void testGet_migrateEnabled_existsInLegacyCache() throws Exception {
        when(newCache.get(COLLECTION_ID))
                .thenThrow(KeyNotFoundException.class);
        when(legacyCache.get(COLLECTION_ID))
                .thenReturn(key);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, true);

        SecretKey actual = cache.get(COLLECTION_ID);

        assertThat(actual, is(notNullValue()));
        assertThat(actual, equalTo(key));
        verify(newCache, times(1)).get(COLLECTION_ID);
        verify(legacyCache, times(1)).get(COLLECTION_ID);
    }

    @Test
    public void testGet_migrateEnabled_keyNotFound() throws Exception {
        when(newCache.get(COLLECTION_ID))
                .thenThrow(KeyNotFoundException.class);
        when(legacyCache.get(COLLECTION_ID))
                .thenReturn(null);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, true);

        SecretKey actual = cache.get(COLLECTION_ID);

        assertThat(actual, is(nullValue()));
        verify(newCache, times(1)).get(COLLECTION_ID);
        verify(legacyCache, times(1)).get(COLLECTION_ID);
    }

    @Test
    public void testGet_migrateEnabled_newCacheException() throws Exception {
        when(newCache.get(COLLECTION_ID))
                .thenThrow(KeyringException.class);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, true);

        assertThrows(KeyringException.class, () -> cache.get(COLLECTION_ID));

        verify(newCache, times(1)).get(COLLECTION_ID);
        verifyZeroInteractions(legacyCache);
    }

    @Test
    public void testGet_migrateEnabled_legacyCacheException() throws Exception {
        when(newCache.get(COLLECTION_ID))
                .thenThrow(KeyNotFoundException.class);

        when(legacyCache.get(COLLECTION_ID))
                .thenThrow(KeyringException.class);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, true);

        assertThrows(KeyringException.class, () -> cache.get(COLLECTION_ID));

        verify(newCache, times(1)).get(COLLECTION_ID);
        verify(legacyCache, times(1)).get(COLLECTION_ID);
    }

    @Test
    public void testAdd_migrateDisabled_legacyAddEx() throws Exception {
        doThrow(KeyringException.class)
                .when(legacyCache)
                .add(any(), any());

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, false);
        assertThrows(KeyringException.class, () -> cache.add(COLLECTION_ID, key));

        verify(legacyCache, times(1)).add(COLLECTION_ID, key);
        verifyZeroInteractions(newCache);
    }

    @Test
    public void testAdd_migrateDisabled_success() throws Exception {
        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, false);

        cache.add(COLLECTION_ID, key);

        verify(legacyCache, times(1)).add(COLLECTION_ID, key);
        verifyZeroInteractions(newCache);
    }

    @Test
    public void testAdd_migrateEnabled_newCacheEx() throws Exception {
        doThrow(KeyringException.class)
                .when(newCache)
                .add(COLLECTION_ID, key);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, true);

        assertThrows(KeyringException.class, () -> cache.add(COLLECTION_ID, key));

        verify(legacyCache, times(1)).add(COLLECTION_ID, key);
        verify(newCache, times(1)).add(COLLECTION_ID, key);
    }

    @Test
    public void testAdd_migrateEnabled_success() throws Exception {
        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, true);

        cache.add(COLLECTION_ID, key);

        verify(legacyCache, times(1)).add(COLLECTION_ID, key);
        verify(newCache, times(1)).add(COLLECTION_ID, key);
    }

    @Test
    public void testLoad_migrateDisabled_shouldDoNothing() throws Exception {
        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, false);

        cache.load();

        verifyZeroInteractions(legacyCache, newCache);
    }

    @Test
    public void testLoad_migrateEnabled_loadException() throws Exception {
        doThrow(KeyringException.class)
                .when(newCache)
                .load();

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, true);

        assertThrows(KeyringException.class, () -> cache.load());

        verify(newCache, times(1)).load();
        verifyZeroInteractions(legacyCache);
    }

    @Test
    public void testLoad_migrateEnabled_success() throws Exception {
        doThrow(KeyringException.class)
                .when(newCache)
                .load();

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, true);

        assertThrows(KeyringException.class, () -> cache.load());

        verify(newCache, times(1)).load();
        verifyZeroInteractions(legacyCache);
    }

    @Test
    public void testList_migrateDisabled_listException() throws Exception {
        when(legacyCache.list())
                .thenThrow(KeyringException.class);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, false);

        assertThrows(KeyringException.class, () -> cache.list());

        verify(legacyCache, times(1)).list();
        verifyZeroInteractions(newCache);
    }

    @Test
    public void testList_migrateDisabled_success() throws Exception {
        Set<String> list = new HashSet<String>() {{
            add(COLLECTION_ID);
        }};

        when(legacyCache.list())
                .thenReturn(list);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, false);

        Set<String> actual = cache.list();

        assertThat(actual, is(notNullValue()));
        assertThat(actual.size(), equalTo(1));
        assertTrue(actual.contains(COLLECTION_ID));
        verify(legacyCache, times(1)).list();
        verifyZeroInteractions(newCache);
    }

    @Test
    public void testList_migrateEnabled_listException() throws Exception {
        when(newCache.list())
                .thenThrow(KeyringException.class);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, true);

        assertThrows(KeyringException.class, () -> cache.list());

        verify(newCache, times(1)).list();
        verifyZeroInteractions(legacyCache);
    }

    @Test
    public void testList_migrateEnabled_success() throws Exception {
        Set<String> list = new HashSet<String>() {{
            add(COLLECTION_ID);
        }};

        when(newCache.list())
                .thenReturn(list);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, true);

        Set<String> actual = cache.list();

        assertThat(actual, is(notNullValue()));
        assertThat(actual.size(), equalTo(1));
        assertTrue(actual.contains(COLLECTION_ID));
        verify(newCache, times(1)).list();
        verifyZeroInteractions(legacyCache);
    }

    @Test
    public void testRemove_migrateDisabled_legacyCacheError() throws Exception {
        doThrow(KeyringException.class)
                .when(legacyCache)
                .remove(COLLECTION_ID);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, false);

        assertThrows(KeyringException.class, () -> cache.remove(COLLECTION_ID));

        verify(legacyCache, times(1)).remove(COLLECTION_ID);
        verifyZeroInteractions(newCache);
    }

    @Test
    public void testRemove_migrateDisabled_success() throws Exception {
        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, false);

        cache.remove(COLLECTION_ID);

        verify(legacyCache, times(1)).remove(COLLECTION_ID);
        verifyZeroInteractions(newCache);
    }

    @Test
    public void testRemove_migrateEnabled_legacyCacheError() throws Exception {
        doThrow(KeyringException.class)
                .when(legacyCache)
                .remove(COLLECTION_ID);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, true);

        assertThrows(KeyringException.class, () -> cache.remove(COLLECTION_ID));

        verify(legacyCache, times(1)).remove(COLLECTION_ID);
        verifyZeroInteractions(newCache);
    }

    @Test
    public void testRemove_migrateEnabled_newCacheError() throws Exception {
        doThrow(KeyringException.class)
                .when(newCache)
                .remove(COLLECTION_ID);

        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, true);

        assertThrows(KeyringException.class, () -> cache.remove(COLLECTION_ID));

        verify(legacyCache, times(1)).remove(COLLECTION_ID);
        verify(newCache, times(1)).remove(COLLECTION_ID);
    }

    @Test
    public void testRemove_migrateEnabled_success() throws Exception {
        cache = new MigrationCollectionKeyCacheImpl(legacyCache, newCache, true);

        cache.remove(COLLECTION_ID);

        verify(legacyCache, times(1)).remove(COLLECTION_ID);
        verify(newCache, times(1)).remove(COLLECTION_ID);
    }
}