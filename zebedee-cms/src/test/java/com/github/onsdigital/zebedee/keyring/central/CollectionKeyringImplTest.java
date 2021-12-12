package com.github.onsdigital.zebedee.keyring.central;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.keyring.CollectionKeyCache;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.onsdigital.zebedee.keyring.central.CollectionKeyringImpl.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class CollectionKeyringImplTest {

    static final String TEST_COLLECTION_ID = "44";
    static final String TEST_EMAIL_ID = "testid@ons.gov.uk";
    private CollectionKeyring keyring;

    @Mock
    private CollectionKeyCache keyCache;

    @Mock
    private Session session;

    @Mock
    private SecretKey secretKey;

    @Mock
    private Collection collection;

    @Mock
    private Collections collections;

    @Mock
    private CollectionDescription collDesc;

    @Mock
    private PermissionsService permissionsService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        CollectionKeyringImpl.init(keyCache, permissionsService, collections);
        keyring = CollectionKeyringImpl.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        resetInstanceToNull();
    }

    @Test
    public void testGetInstance_notInitialised() throws Exception {
        // Given CollectionKeyring has not been initialised
        resetInstanceToNull();

        // When GetInstance is called
        // Then an exception is thrown
        KeyringException ex = assertThrows(KeyringException.class, () -> CollectionKeyringImpl.getInstance());
        assertThat(ex.getMessage(), equalTo(NOT_INITIALISED_ERR));
    }

    @Test
    public void testGetInstance_success() throws KeyringException {
        // Given CollectionKeyring has been initialised

        // When GetInstance is called
        CollectionKeyring keyring = CollectionKeyringImpl.getInstance();

        // Then a non null instance is returned
        assertThat(keyring, is(notNullValue()));
    }

    @Test
    public void testInit_keyringCacheNull() throws Exception {
        resetInstanceToNull();

        // When init is called
        KeyringException ex = assertThrows(KeyringException.class, () -> CollectionKeyringImpl.init(null, null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(KEYRING_CACHE_NULL_ERR));
    }

    @Test
    public void testInit_permissionServiceNull() throws Exception {
        resetInstanceToNull();

        // When init is called
        KeyringException ex = assertThrows(KeyringException.class,
                () -> CollectionKeyringImpl.init(keyCache, null, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(PERMISSION_SERVICE_NULL_ERR));
    }

    @Test
    public void testInit_collectionsNull() throws Exception {
        resetInstanceToNull();

        // When init is called
        KeyringException ex = assertThrows(KeyringException.class,
                () -> CollectionKeyringImpl.init(keyCache, permissionsService, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTIONS_NULL_ERR));
    }

    @Test
    public void testGet_sessionIsNull_shouldThrowException() {
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(null, null));

        assertThat(ex.getMessage(), equalTo(SESSION_NULL_ERR));
    }

    @Test
    public void testGet_sessionEmailNull_shouldThrowException() {
        // Given user email is null
        when(session.getEmail())
                .thenReturn(null);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(session, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SESSION_EMAIL_ERR));

    }

    @Test
    public void testGet_sessionEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(session.getEmail())
                .thenReturn("");

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(session, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SESSION_EMAIL_ERR));

    }

    @Test
    public void testGet_collectionIsNull_shouldThrowException() {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(session, null));

        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void testGet_collectionDescriptionIsNull_shouldThrowException() {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(session, collection));

        assertThat(ex.getMessage(), equalTo(COLLECTION_DESCRIPTION_NULL_ERR));
    }

    @Test
    public void testGet_collectionIDNull_shouldThrowException() {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(session, collection));

        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_NULL_OR_EMPTY_ERR));
    }

    @Test
    public void testGet_collectionIDEmpty_shouldThrowException() {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn("");

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(session, collection));

        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_NULL_OR_EMPTY_ERR));
    }

    @Test
    public void testGet_permissionsServiceThrowsException() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.canView(TEST_EMAIL_ID, collDesc))
                .thenThrow(new IOException("Bork"));

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(session, collection));

        assertThat(ex.getCause().getMessage(), equalTo("Bork"));
        verify(permissionsService, times(1)).canView(TEST_EMAIL_ID, collDesc);
    }

    @Test
    public void testGet_permissionsServiceReturnsFalse() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.canView(TEST_EMAIL_ID, collDesc))
                .thenReturn(false);

        SecretKey secretKey = keyring.get(session, collection);

        assertThat(secretKey, is(nullValue()));
        verify(permissionsService, times(1)).canView(TEST_EMAIL_ID, collDesc);
    }

    @Test
    public void testGet_keyringCacheThrowsException() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.canView(TEST_EMAIL_ID, collDesc))
                .thenReturn(true);

        when(keyCache.get(TEST_COLLECTION_ID))
                .thenThrow(new KeyringException("Beep"));

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.get(session, collection));
        assertThat(ex.getMessage(), equalTo("Beep"));

        verify(permissionsService, times(1)).canView(TEST_EMAIL_ID, collDesc);
        verify(keyCache, times(1)).get(TEST_COLLECTION_ID);
    }

    @Test
    public void testGet_keyringCacheReturnsNull() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.canView(TEST_EMAIL_ID, collDesc))
                .thenReturn(true);

        when(keyCache.get(TEST_COLLECTION_ID))
                .thenReturn(null);

        SecretKey key = keyring.get(session, collection);

        assertThat(key, is(nullValue()));
        verify(permissionsService, times(1)).canView(TEST_EMAIL_ID, collDesc);
        verify(keyCache, times(1)).get(TEST_COLLECTION_ID);
    }

    @Test
    public void testGet_keyringCacheReturnsCollectionKey() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.canView(TEST_EMAIL_ID, collDesc))
                .thenReturn(true);

        when(keyCache.get(TEST_COLLECTION_ID))
                .thenReturn(secretKey);

        SecretKey key = keyring.get(session, collection);

        assertThat(key, equalTo(secretKey));
        verify(permissionsService, times(1)).canView(TEST_EMAIL_ID, collDesc);
        verify(keyCache, times(1)).get(TEST_COLLECTION_ID);
    }

    @Test
    public void testRemove_sessionIsNull_shouldThrowException() {
        // Given user is null

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(null, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SESSION_NULL_ERR));

    }

    @Test
    public void testRemove_sessionEmailNull_shouldThrowException() {
        // Given user email is null
        when(session.getEmail())
                .thenReturn(null);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(session, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SESSION_EMAIL_ERR));

    }

    @Test
    public void testRemove_sessionEmailEmpty_shouldThrowException() {
        // Given user email is null
        when(session.getEmail())
                .thenReturn("");

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(session, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SESSION_EMAIL_ERR));

    }

    @Test
    public void testRemove_collectionIsNull_shouldThrowException() {
        // Given collection is null
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(session, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void testRemove_collectionDescriptionIsNull_shouldThrowException() {
        // Given session with valid email id and collection description is null
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription()).
                thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(session, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_DESCRIPTION_NULL_ERR));
    }

    @Test
    public void testRemove_collectionIdIsNull_shouldThrowException() {
        // Given session with valid email id and collection Id  null
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collection.getDescription().getId())
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(session, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_NULL_OR_EMPTY_ERR));
    }

    @Test
    public void testRemove_collectionIdIsEmpty_shouldThrowException() {
        // Given collection Id is null
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collection.getDescription().getId())
                .thenReturn("");

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(session, collection));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_NULL_OR_EMPTY_ERR));
    }

    @Test
    public void testRemove_permissionsServiceThrowsException() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.canEdit(session))
                .thenThrow(new IOException("Bork"));

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.remove(session, collection));

        assertThat(ex.getCause().getMessage(), equalTo("Bork"));
        verify(permissionsService, times(1)).canEdit(session);
    }

    @Test
    public void testRemove_permissionsServiceReturnsFalse() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.canEdit(session))
                .thenReturn(false);

        keyring.remove(session, collection);

        verify(permissionsService, times(1)).canEdit(session);
    }

    @Test
    public void testRemove_removesCollectionKeyFromKeyringCache() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.canEdit(session))
                .thenReturn(true);

        keyring.remove(session, collection);

        verify(permissionsService, times(1)).canEdit(session);
        verify(keyCache, times(1)).remove(TEST_COLLECTION_ID);
    }

    @Test
    public void testAdd_userIsNull_shouldThrowException() {
        // Given user is null

        // When Add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(null, collection, secretKey));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SESSION_NULL_ERR));
    }

    @Test
    public void testAdd_sessionEmailNull_shouldThrowException() {
        // Given session email is null
        when(session.getEmail())
                .thenReturn(null);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(session, collection, secretKey));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SESSION_EMAIL_ERR));

    }

    @Test
    public void testAdd_sessionEmailEmpty_shouldThrowException() {
        // Given session email is empty
        when(session.getEmail())
                .thenReturn("");

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(session, collection, secretKey));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SESSION_EMAIL_ERR));

    }

    @Test
    public void testAdd_collectionIsNull_shouldThrowException() {
        // Given session with valid email and collection to be null
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);
        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(session, null, secretKey));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_NULL_ERR));
    }

    @Test
    public void testAdd_collectionDescriptionIsNull_shouldThrowException() {
        // Given collection description is null
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription()).
                thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(session, collection, secretKey));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_DESCRIPTION_NULL_ERR));
    }

    @Test
    public void testAdd_collectionIdIsNull_shouldThrowException() {
        // Given collection Id is null
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collection.getDescription().getId())
                .thenReturn(null);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(session, collection, secretKey));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_NULL_OR_EMPTY_ERR));
    }

    @Test
    public void testAdd_collectionIdIsEmpty_shouldThrowException() {
        // Given collection Id is empty
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collection.getDescription().getId())
                .thenReturn("");

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(session, collection, secretKey));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(COLLECTION_ID_NULL_OR_EMPTY_ERR));
    }

    @Test
    public void testAdd_secretKeyIsNull_shouldThrowException() {
        // Given secret key is null
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collection.getDescription().getId())
                .thenReturn(TEST_COLLECTION_ID);

        // When remove is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(session, collection, null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SECRET_KEY_NULL_ERR));
    }

    @Test
    public void testAdd_permissionsServiceThrowsException() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.canEdit(session))
                .thenThrow(new IOException("Bork"));

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.add(session, collection, secretKey));

        assertThat(ex.getCause().getMessage(), equalTo("Bork"));
        verify(permissionsService, times(1)).canEdit(session);
    }

    @Test
    public void testAdd_permissionsServiceReturnsFalse() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.canEdit(session))
                .thenReturn(false);

        keyring.add(session, collection, secretKey);

        verify(permissionsService, times(1)).canEdit(session);
        verifyZeroInteractions(keyCache);
    }

    @Test
    public void testAdd_keyringCacheAddsCollectionKey() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(collection.getDescription())
                .thenReturn(collDesc);

        when(collDesc.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissionsService.canEdit(session))
                .thenReturn(true);

        keyring.add(session, collection, secretKey);

        verify(permissionsService, times(1)).canEdit(session);
        verify(keyCache, times(1)).add(TEST_COLLECTION_ID, secretKey);
    }

    @Test
    public void testList_userIsNull_shouldThrowException() {
        // Given user is null

        // When list is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.list(null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SESSION_NULL_ERR));
    }

    @Test
    public void testList_emailIsNull_shouldThrowException() {
        // Given user is null

        // When list is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.list(null));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SESSION_NULL_ERR));
    }


    @Test
    public void testList_userEmailNull_shouldThrowException() {
        // Given session email is null
        when(session.getEmail())
                .thenReturn(null);

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.list(session));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SESSION_EMAIL_ERR));

    }

    @Test
    public void testList_userEmailEmpty_shouldThrowException() {
        // Given user email is empty
        when(session.getEmail())
                .thenReturn("");

        // When add is called
        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.list(session));

        // Then an exception is thrown
        assertThat(ex.getMessage(), equalTo(SESSION_EMAIL_ERR));

    }

    @Test
    public void testList_permissionsServiceThrowsException() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(permissionsService.canEdit(session))
                .thenThrow(new IOException("Bork"));

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.list(session));

        assertThat(ex.getCause().getMessage(), equalTo("Bork"));
    }

    @Test
    public void testList_userHasAdminPermission() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(permissionsService.canEdit(session))
                .thenReturn(true);

        Set<String> expected = new HashSet<String>() {{
            add("111");
            add("222");
            add("333");
        }};

        when(keyCache.list())
                .thenReturn(expected);

        Set<String> actual = keyring.list(session);

        assertThat(actual, equalTo(expected));
        verifyZeroInteractions(collections);
        verify(permissionsService, times(1)).canEdit(session);
    }

    @Test
    public void testList_filterCollectionsException() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(permissionsService.canEdit(session))
                .thenReturn(false);

        when(collections.filterBy(any()))
                .thenThrow(IOException.class);

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.list(session));

        assertThat(ex.getMessage(), equalTo(FILTER_COLLECTIONS_ERR));
        verify(permissionsService, times(1)).canEdit(session);
        verify(collections, times(1)).filterBy(any());
    }

    @Test
    public void testList_filterCollectionsReturnsEmptyList() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(permissionsService.canEdit(session))
                .thenReturn(false);

        when(collections.filterBy(any()))
                .thenReturn(new ArrayList<>());

        Set<String> actual = keyring.list(session);
        assertTrue(actual.isEmpty());
    }

    @Test
    public void testList_filterCollectionsReturnsNull() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(permissionsService.canEdit(session))
                .thenReturn(false);

        when(collections.filterBy(any()))
                .thenReturn(null);

        KeyringException ex = assertThrows(KeyringException.class, () -> keyring.list(session));

        assertThat(ex.getMessage(), equalTo(FILTER_COLLECTIONS_ERR));
        verify(permissionsService, times(1)).canEdit(session);
        verify(collections, times(1)).filterBy(any());
    }

    @Test
    public void testList_shouldReturnExpectedValuesForViewerUser() throws Exception {
        when(session.getEmail())
                .thenReturn(TEST_EMAIL_ID);

        when(permissionsService.canEdit(session))
                .thenReturn(false);

        // All collections held in the cache.
        Set<String> cacheValues = new HashSet<String>() {{
            add("111");
            add("222");
            add(TEST_COLLECTION_ID);
            add("333");
        }};
        when(keyCache.list())
                .thenReturn(cacheValues);

        when(collection.getId())
                .thenReturn(TEST_COLLECTION_ID);

        // Collections the user has view permission for.
        List<Collection> accessibleToUser = new ArrayList<Collection>() {{
            add(collection);
        }};
        when(collections.filterBy(any()))
                .thenReturn(accessibleToUser);

        Set<String> actual = keyring.list(session);

        assertThat(actual.size(), equalTo(1));
        assertThat(actual.iterator().next(), equalTo(TEST_COLLECTION_ID));

        verify(collections, times(1)).filterBy(any());
        verify(permissionsService, times(1)).canEdit(session);
    }

    private void resetInstanceToNull() throws Exception {
        // Use some evil reflection magic to set the instance back to null for this test case.
        Field field = CollectionKeyringImpl.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        field.set(null, null);
    }
}
