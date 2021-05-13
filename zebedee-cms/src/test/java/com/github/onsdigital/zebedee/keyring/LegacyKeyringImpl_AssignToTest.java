package com.github.onsdigital.zebedee.keyring;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Keyring;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.CACHED_KEY_MISSING_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.CACHE_KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.EMAIL_EMPTY_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.KEYRING_LOCKED_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.USER_KEYRING_NULL_ERR;
import static com.github.onsdigital.zebedee.keyring.LegacyKeyringImpl.USER_NULL_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LegacyKeyringImpl_AssignToTest extends BaseLegacyKeyringTest {

    @Mock
    private Keyring bertCachedKeyring, ernieCachedKeyring;

    private List<CollectionDescription> assignments;

    private Set<String> keySet;

    @Override
    public void setUpTests() throws Exception {
        assignments = new ArrayList<>();
        assignments.add(collectionDescription);

        keySet = new HashSet<>();
        keySet.add(TEST_COLLECTION_ID);

        when(keyringCache.get(bert))
                .thenReturn(bertCachedKeyring);

        when(keyringCache.get(ernie))
                .thenReturn(ernieCachedKeyring);

        when(bertCachedKeyring.isUnlocked())
                .thenReturn(true);

        when(bertCachedKeyring.keySet())
                .thenReturn(keySet);

        when(bertCachedKeyring.get(TEST_COLLECTION_ID))
                .thenReturn(secretKey);
    }

    @Test
    public void testAssignTo_assignmentsNull_shouldDoNothing() throws Exception {
        legacyKeyring.assignTo(bert, ernie, null);

        verifyZeroInteractions(bert, ernie, bertKeyring, ernieKeyring, keyringCache, users);
    }

    @Test
    public void testAssignTo_assignmentsEmpty_shouldDoNothing() throws Exception {
        legacyKeyring.assignTo(bert, ernie, new ArrayList<>());

        verifyZeroInteractions(bert, ernie, bertKeyring, ernieKeyring, keyringCache, users);
    }

    @Test
    public void testAssignTo_srcUserNull_shouldThrowEx() throws Exception {
        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.assignTo(null, ernie, assignments));

        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(ernie, bertKeyring, ernieKeyring, keyringCache, users);
    }

    @Test
    public void testAssignTo_srcUserEmailNull_shouldThrowEx() throws Exception {
        when(bert.getEmail())
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.assignTo(bert, ernie, assignments));

        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(ernie, bertKeyring, ernieKeyring, keyringCache, users);
    }

    @Test
    public void testAssignTo_srcUserEmailEmpty_shouldThrowEx() throws Exception {
        when(bert.getEmail())
                .thenReturn("");

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.assignTo(bert, ernie, assignments));

        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(ernie, bertKeyring, ernieKeyring, keyringCache, users);
    }

    @Test
    public void testAssignTo_srcUserKeyringNull_shouldThrowEx() throws Exception {
        when(bert.keyring())
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.assignTo(bert, ernie, assignments));

        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
        verifyZeroInteractions(ernie, bertKeyring, ernieKeyring, keyringCache, users);
    }

    @Test
    public void testAssignTo_targetUserNull_shouldThrowEx() throws Exception {
        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.assignTo(bert, null, assignments));

        assertThat(ex.getMessage(), equalTo(USER_NULL_ERR));
        verifyZeroInteractions(ernie, bertKeyring, ernieKeyring, keyringCache, users);
    }

    @Test
    public void testAssignTo_targetUserEmailNull_shouldThrowEx() throws Exception {
        when(ernie.getEmail())
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.assignTo(bert, ernie, assignments));

        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(bertKeyring, ernieKeyring, keyringCache, users);
    }

    @Test
    public void testAssignTo_targetUserEmailEmpty_shouldThrowEx() throws Exception {
        when(ernie.getEmail())
                .thenReturn("");

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.assignTo(bert, ernie, assignments));

        assertThat(ex.getMessage(), equalTo(EMAIL_EMPTY_ERR));
        verifyZeroInteractions(bertKeyring, ernieKeyring, keyringCache, users);
    }

    @Test
    public void testAssignTo_targetUserKeyringNull_shouldThrowEx() throws Exception {
        when(ernie.keyring())
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.assignTo(bert, ernie, assignments));

        assertThat(ex.getMessage(), equalTo(USER_KEYRING_NULL_ERR));
        verifyZeroInteractions(bertKeyring, ernieKeyring, keyringCache, users);
    }

    @Test
    public void testAssignTo_getSrcCachedKeyringError_shouldThrowEx() throws Exception {
        when(keyringCache.get(bert))
                .thenThrow(KeyringException.class);

        assertThrows(KeyringException.class,
                () -> legacyKeyring.assignTo(bert, ernie, assignments));

        verify(keyringCache, times(1)).get(bert);
    }

    @Test
    public void testAssignTo_SrcCachedKeyringNull_shouldThrowEx() throws Exception {
        when(keyringCache.get(bert))
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.assignTo(bert, ernie, assignments));

        assertThat(ex.getMessage(), equalTo(CACHE_KEYRING_NULL_ERR));
        verify(keyringCache, times(1)).get(bert);
    }

    @Test
    public void testAssignTo_SrcCachedKeyringLocked_shouldThrowEx() throws Exception {
        when(bertCachedKeyring.isUnlocked())
                .thenReturn(false);

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.assignTo(bert, ernie, assignments));

        assertThat(ex.getMessage(), equalTo(KEYRING_LOCKED_ERR));
        verify(keyringCache, times(1)).get(bert);
    }

    @Test
    public void testAssignTo_SrcCachedKeyringDoesNotContainKey_shouldThrowEx() throws Exception {
        when(bertCachedKeyring.keySet())
                .thenReturn(new HashSet<>());

        KeyringException ex = assertThrows(KeyringException.class,
                () -> legacyKeyring.assignTo(bert, ernie, assignments));

        assertThat(ex.getMessage(), equalTo(CACHED_KEY_MISSING_ERR));
        assertThat(ex.getCollectionID(), equalTo(TEST_COLLECTION_ID));
    }

    @Test
    public void testAssignTo_success_shouldAddKeysToCachedKeyAndUserKeyring() throws Exception {
        legacyKeyring.assignTo(bert, ernie, assignments);

        verify(ernieCachedKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);
        verify(ernieKeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);
        verify(users, times(1)).updateKeyring(ernie);
    }
}
