package com.github.onsdigital.zebedee.keyring.migration;

import com.github.onsdigital.zebedee.keyring.KeyringCache;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MigrationKeyringCacheImplTest {

    private static final String COLLECTION_ID = "138";

    @Mock
    private KeyringCache legacyCache, centralCache;

    @Mock
    private SecretKey key;

    private KeyringCache cache;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Scenario: test get with migrationEnabled == false. Should only use the legacy cache to get values.
     */
    @Test
    public void testGet_migrateDisabled_shouldGetFromLegacy() throws Exception {
        when(legacyCache.get(COLLECTION_ID))
                .thenReturn(key);

        cache = new MigrationKeyringCacheImpl(legacyCache, centralCache, false);

        SecretKey actual = cache.get(COLLECTION_ID);

        assertThat(actual, is(notNullValue()));
        assertThat(actual, equalTo(key));
        verify(legacyCache, times(1)).get(COLLECTION_ID);
        verifyZeroInteractions(centralCache);
    }
}
