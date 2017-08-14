package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;
import com.github.onsdigital.zebedee.permissions.service.PermissionsServiceImpl;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.user.service.UsersService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by dave on 01/06/2017.
 */
public class KeyManagerTest {

    private static final String COLLECTION_ID = "123";
    private static final String EMAIL = "test@ons.gov.uk";
    private static final String USER2_EMAIL = "user2@ons.gov.uk";

    @Mock
    private Zebedee zebedee;

    @Mock
    private User user, user2;

    @Mock
    private SecretKey secretKey;

    @Mock
    private UsersService usersService;

    @Mock
    private Keyring keyring, userKeyring, user2Keyring;

    @Mock
    private SessionsService sessionsService;

    @Mock
    private Session session, user2Session;

    @Mock
    private KeyringCache keyringCache;

    @Mock
    private PermissionsServiceImpl permissionsServiceImpl;

    @Mock
    private Collection collection;

    @Mock
    private Collections collections;

    @Mock
    private CollectionDescription collectionDescription;

    @Mock
    private Map<String, SecretKey> schedulerCache;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(user.getEmail())
                .thenReturn(EMAIL);
        when(user.keyring())
                .thenReturn(userKeyring);

        when(user2.getEmail())
                .thenReturn(USER2_EMAIL);
        when(user2.keyring())
                .thenReturn(user2Keyring);

        when(zebedee.getUsersService())
                .thenReturn(usersService);
        when(zebedee.getSessionsService())
                .thenReturn(sessionsService);
        when(zebedee.getKeyringCache())
                .thenReturn(keyringCache);
        when(zebedee.getPermissionsService())
                .thenReturn(permissionsServiceImpl);
        when(collection.getDescription())
                .thenReturn(collectionDescription);
        when(collectionDescription.getId())
                .thenReturn(COLLECTION_ID);

        ServiceSupplier<Collections> collectionsServiceSupplier = () -> collections;
        KeyManager.setCollectionsServiceSupplier(collectionsServiceSupplier);
    }

    @Test
    public void assignKeyToUser_ShouldDoNothingIfUserKeyringNull() throws Exception {
        when(user.keyring())
                .thenReturn(null);

        KeyManager.assignKeyToUser(zebedee, user, COLLECTION_ID, secretKey);

        verifyZeroInteractions(zebedee, usersService, secretKey, keyring);
        verify(user, times(1)).keyring();
        verify(user, times(1)).getEmail();
        verifyNoMoreInteractions(user);
    }

    @Test
    public void assignKeyToUser_Success() throws Exception {
        when(user.keyring())
                .thenReturn(keyring);
        when(sessionsService.find(EMAIL))
                .thenReturn(session);

        when(keyringCache.get(session))
                .thenReturn(keyring);

        KeyManager.assignKeyToUser(zebedee, user, COLLECTION_ID, secretKey);

        verify(user, times(1)).keyring();
        verify(user, times(2)).getEmail();
        verify(zebedee, times(1)).getUsersService();
        verify(usersService, times(1)).addKeyToKeyring(EMAIL, COLLECTION_ID, secretKey);
        verify(zebedee, times(1)).getSessionsService();
        verify(zebedee, times(1)).getKeyringCache();
        verify(keyringCache, times(1)).get(session);
        verify(keyring, times(1)).put(COLLECTION_ID, secretKey);
    }

    @Test
    public void distributeApplicationKeyToUser_ShouldDoNothingIfUserNull() throws Exception {
        UserList ul = new UserList();
        User nullUser = null;
        ul.add(nullUser);

        when(zebedee.getUsersService())
                .thenReturn(usersService);
        when(usersService.list())
                .thenReturn(ul);

        KeyManager.distributeApplicationKey(zebedee, COLLECTION_ID, secretKey);

        verify(zebedee, times(1)).getUsersService();
        verify(usersService, times(1)).list();
        verifyNoMoreInteractions(zebedee, usersService);
        verifyZeroInteractions(permissionsServiceImpl, keyringCache, keyring, sessionsService, secretKey);
    }

    @Test
    public void distributeApplicationKeyToUser_ShouldDoNothingIfUserEmailBlack() throws Exception {
        UserList ul = new UserList();
        User u = new User();
        ul.add(u);

        when(zebedee.getUsersService())
                .thenReturn(usersService);
        when(usersService.list())
                .thenReturn(ul);

        KeyManager.distributeApplicationKey(zebedee, COLLECTION_ID, secretKey);

        verify(zebedee, times(1)).getUsersService();
        verify(usersService, times(1)).list();
        verifyNoMoreInteractions(zebedee, usersService);
        verifyZeroInteractions(permissionsServiceImpl, keyringCache, keyring, sessionsService, secretKey);
    }

    @Test
    public void distributeApplicationKeyToUser_ShouldRemoveKeyFromUserIfDoesNotHavePermission() throws Exception {
        UserList ul = new UserList();
        ul.add(user);

        when(user.keyring())
                .thenReturn(keyring);
        when(usersService.list())
                .thenReturn(ul);
        when(permissionsServiceImpl.isAdministrator(EMAIL))
                .thenReturn(false);
        when(permissionsServiceImpl.canEdit(EMAIL))
                .thenReturn(false);
        when(sessionsService.find(EMAIL))
                .thenReturn(session);
        when(keyringCache.get(session))
                .thenReturn(keyring);

        KeyManager.distributeApplicationKey(zebedee, COLLECTION_ID, secretKey);

        verify(zebedee, times(2)).getUsersService();
        verify(zebedee, times(2)).getPermissionsService();
        verify(zebedee, times(1)).getSessionsService();
        verify(zebedee, times(1)).getKeyringCache();
        verify(permissionsServiceImpl, times(1)).isAdministrator(EMAIL);
        verify(permissionsServiceImpl, times(1)).canEdit(EMAIL);
        verify(keyringCache, times(1)).get(session);
        verify(keyring, times(1)).remove(COLLECTION_ID);
        verify(usersService, times(1)).list();
        verify(usersService, times(1)).removeKeyFromKeyring(EMAIL, COLLECTION_ID);
        verify(sessionsService, times(1)).find(EMAIL);
        verify(user, times(5)).getEmail();
        verify(user, times(1)).keyring();
        verifyNoMoreInteractions(zebedee, permissionsServiceImpl, usersService, sessionsService, keyringCache, user, keyring, session);
    }

    @Test
    public void distributeApplicationKeyToUser_ShouldAssignKeyToUserIfHasPermission() throws Exception {
        UserList ul = new UserList();
        ul.add(user);

        when(usersService.list())
                .thenReturn(ul);
        when(permissionsServiceImpl.isAdministrator(EMAIL))
                .thenReturn(true);
        when(permissionsServiceImpl.canEdit(EMAIL))
                .thenReturn(false);
        when(sessionsService.find(EMAIL))
                .thenReturn(session);
        when(keyringCache.get(session))
                .thenReturn(keyring);
        when(user.keyring())
                .thenReturn(keyring);

        KeyManager.distributeApplicationKey(zebedee, COLLECTION_ID, secretKey);

        verify(zebedee, times(2)).getUsersService();
        verify(zebedee, times(1)).getPermissionsService();
        verify(zebedee, times(1)).getSessionsService();
        verify(zebedee, times(1)).getKeyringCache();
        verify(permissionsServiceImpl, times(1)).isAdministrator(EMAIL);
        verify(permissionsServiceImpl, never()).canEdit(EMAIL);
        verify(keyringCache, times(1)).get(session);
        verify(keyring, times(1)).put(COLLECTION_ID, secretKey);
        verify(usersService, times(1)).list();
        verify(usersService, times(1)).addKeyToKeyring(EMAIL, COLLECTION_ID, secretKey);
        verify(sessionsService, times(1)).find(EMAIL);
        verify(user, times(4)).getEmail();
        verify(user, times(1)).keyring();
        verifyNoMoreInteractions(zebedee, permissionsServiceImpl, usersService, sessionsService, keyringCache, user, keyring, session);
    }

    @Test
    public void transferKeyring_Success() throws Exception {
        Set<String> collectionIds = new HashSet<>();
        collectionIds.add(COLLECTION_ID);

        Keyring source = mock(Keyring.class);
        Keyring target = mock(Keyring.class);

        when(source.get(COLLECTION_ID))
                .thenReturn(secretKey);

        KeyManager.transferKeyring(target, source, collectionIds);

        verify(source, times(1)).get(COLLECTION_ID);
        verify(target, times(1)).put(COLLECTION_ID, secretKey);
    }

    @Test
    public void transferKeyringByCollectionOwner_Success() throws Exception {
        Keyring source = mock(Keyring.class);
        Keyring target = mock(Keyring.class);

        Set<String> srcIDS = new HashSet<>();
        srcIDS.add(COLLECTION_ID);

        when(source.list())
                .thenReturn(srcIDS);
        when(collections.getCollection(COLLECTION_ID))
                .thenReturn(collection);
        when(source.get(COLLECTION_ID))
                .thenReturn(secretKey);

        KeyManager.transferKeyring(target, source);

        verify(source, times(1)).list();
        verify(source,times(1)).get(COLLECTION_ID);
        verify(target,times(1)).put(COLLECTION_ID, secretKey);
    }

    @Test
    public void distributeKeyToUser_ShouldRemoveKeyIfDoesNotHavePermissions() throws Exception {
        when(keyringCache.get(session))
                .thenReturn(keyring);
        when(keyring.get(COLLECTION_ID))
                .thenReturn(secretKey);
        when(permissionsServiceImpl.isAdministrator(EMAIL))
                .thenReturn(false);
        when(permissionsServiceImpl.canView(user, collectionDescription))
                .thenReturn(false);
        when(sessionsService.find(EMAIL))
                .thenReturn(session);
        when(user.keyring())
                .thenReturn(keyring);

        KeyManager.distributeKeyToUser(zebedee, collection, session, user);

        verify(zebedee, times(2)).getKeyringCache();
        verify(keyringCache, times(2)).get(session);
        verify(keyring, times(1)).get(COLLECTION_ID);
        verify(zebedee, times(2)).getPermissionsService();
        verify(permissionsServiceImpl, times(1)).isAdministrator(EMAIL);
        verify(permissionsServiceImpl, times(1)).canView(user, collectionDescription);
        verify(zebedee, times(1)).getUsersService();
        verify(usersService, times(1)).removeKeyFromKeyring(EMAIL, COLLECTION_ID);
        verify(zebedee, times(1)).getSessionsService();
        verify(sessionsService, times(1)).find(EMAIL);
        verify(keyring, times(1)).remove(COLLECTION_ID);
    }

    @Test
    public void distributeKeyToUser_ShouldAssignKeyIfHasPermissions() throws Exception {
        when(keyringCache.get(session))
                .thenReturn(keyring);
        when(keyring.get(COLLECTION_ID))
                .thenReturn(secretKey);
        when(permissionsServiceImpl.isAdministrator(EMAIL))
                .thenReturn(true);
        when(sessionsService.find(EMAIL))
                .thenReturn(session);
        when(user.keyring())
                .thenReturn(keyring);

        KeyManager.distributeKeyToUser(zebedee, collection, session, user);

        verify(zebedee, times(2)).getKeyringCache();
        verify(keyringCache, times(2)).get(session);
        verify(keyring, times(1)).get(COLLECTION_ID);
        verify(user, times(1)).keyring();
        verify(zebedee, times(1)).getSessionsService();
        verify(usersService, times(1)).addKeyToKeyring(EMAIL, COLLECTION_ID, secretKey);
        verify(sessionsService, times(1)).find(EMAIL);
        verify(keyring, times(1)).put(COLLECTION_ID, secretKey);
    }

    @Test
    public void distributeCollectionKey_NewCollectionShouldOnlyAssignToKeyRecipients() throws Exception {
        UserList users = new UserList();
        users.add(user);
        users.add(user2);

        List<User> keyRecipients = new ArrayList<>();
        keyRecipients.add(user);

        when(keyring.get(COLLECTION_ID))
                .thenReturn(secretKey);
        when(keyringCache.get(session))
                .thenReturn(keyring);
        when(zebedee.getPermissionsService())
                .thenReturn(permissionsServiceImpl);
        when(permissionsServiceImpl.getCollectionAccessMapping(collection))
                .thenReturn(keyRecipients);
        when(zebedee.getSessionsService())
                .thenReturn(sessionsService);
        when(sessionsService.find(EMAIL))
                .thenReturn(session);

        KeyManager.distributeCollectionKey(zebedee, session, collection, true);

        verify(zebedee, times(1)).getUsersService();
        verify(usersService, times(1)).addKeyToKeyring(EMAIL, COLLECTION_ID, secretKey);
        verify(usersService, never()).addKeyToKeyring(USER2_EMAIL, COLLECTION_ID, secretKey);
        verify(keyring, times(1)).put(COLLECTION_ID, secretKey);

        verify(sessionsService, never()).find(USER2_EMAIL);
        verify(user2Keyring, never()).put(COLLECTION_ID, secretKey);
    }

    @Test
    public void distributeCollectionKey_NewCollectionShouldNotAssignIfKeyRecipientsNull() throws Exception {
        UserList users = new UserList();
        users.add(user);

        when(zebedee.getKeyringCache())
                .thenReturn(keyringCache);
        when(keyringCache.get(session))
                .thenReturn(keyring);
        when(keyring.get(COLLECTION_ID))
                .thenReturn(secretKey);
        when(zebedee.getPermissionsService())
                .thenReturn(permissionsServiceImpl);
        when(permissionsServiceImpl.getCollectionAccessMapping(collection))
                .thenReturn(null);
        when(usersService.list())
                .thenReturn(users);
        when(sessionsService.find(EMAIL))
                .thenReturn(session);
        when(keyringCache.getSchedulerCache())
                .thenReturn(schedulerCache);

        KeyManager.distributeCollectionKey(zebedee, session, collection, true);

        verify(usersService, never()).addKeyToKeyring(any(), any(), any());
        verify(zebedee, times(2)).getKeyringCache();
        verify(zebedee, times(1)).getPermissionsService();
        verify(permissionsServiceImpl, times(1)).getCollectionAccessMapping(collection);
        verify(usersService, never()).removeKeyFromKeyring(EMAIL, COLLECTION_ID);
        verify(sessionsService, never()).find(EMAIL);
        verify(keyring, never()).put(COLLECTION_ID, secretKey);
        verify(schedulerCache, times(1)).put(COLLECTION_ID, secretKey);
    }

    @Test
    public void distributeCollectionKey_ExistingCollectionShouldRemoveKeyFromNonRecipients() throws
            Exception {
        UserList allUsers = new UserList();
        allUsers.add(user);
        allUsers.add(user2);

        List<User> permittedUsers = new ArrayList<>();
        permittedUsers.add(user2);

        when(zebedee.getKeyringCache())
                .thenReturn(keyringCache);
        when(keyringCache.get(session))
                .thenReturn(keyring);
        when(keyringCache.get(user2Session))
                .thenReturn(user2Keyring);
        when(keyring.get(COLLECTION_ID))
                .thenReturn(secretKey);
        when(zebedee.getPermissionsService())
                .thenReturn(permissionsServiceImpl);
        when(permissionsServiceImpl.getCollectionAccessMapping(collection))
                .thenReturn(permittedUsers);
        when(usersService.list())
                .thenReturn(allUsers);
        when(sessionsService.find(EMAIL))
                .thenReturn(session);
        when(sessionsService.find(USER2_EMAIL))
                .thenReturn(user2Session);
        when(keyringCache.getSchedulerCache())
                .thenReturn(schedulerCache);

        KeyManager.distributeCollectionKey(zebedee, session, collection, false);

        verify(zebedee, times(4)).getKeyringCache();
        verify(zebedee, times(1)).getPermissionsService();
        verify(permissionsServiceImpl, times(1)).getCollectionAccessMapping(collection);
        verify(sessionsService, times(1)).find(EMAIL);
        verify(sessionsService, times(1)).find(USER2_EMAIL);
        verify(usersService, times(1)).removeKeyFromKeyring(EMAIL, COLLECTION_ID);
        verify(usersService, times(1)).addKeyToKeyring(USER2_EMAIL, COLLECTION_ID, secretKey);
        verify(keyring, times(1)).remove(COLLECTION_ID);
        verify(user2Keyring, times(1)).put(COLLECTION_ID, secretKey);
        verify(schedulerCache, times(1)).put(COLLECTION_ID, secretKey);
    }

    @Test
    public void distributeCollectionKey_ShouldNeverRemoveKeysFromNewCollection() throws Exception {
        UserList users = new UserList();
        users.add(user);

        when(zebedee.getKeyringCache())
                .thenReturn(keyringCache);
        when(keyringCache.get(session))
                .thenReturn(keyring);
        when(keyring.get(COLLECTION_ID))
                .thenReturn(secretKey);
        when(zebedee.getPermissionsService())
                .thenReturn(permissionsServiceImpl);
        when(permissionsServiceImpl.getCollectionAccessMapping(collection))
                .thenReturn(users);
        when(usersService.list())
                .thenReturn(users);
        when(sessionsService.find(EMAIL))
                .thenReturn(session);
        when(keyringCache.getSchedulerCache())
                .thenReturn(schedulerCache);

        KeyManager.distributeCollectionKey(zebedee, session, collection, true);

        verify(usersService, times(1)).addKeyToKeyring(EMAIL, COLLECTION_ID, secretKey);
        verify(usersService, never()).removeKeyFromKeyring(any(), any());
        verify(zebedee, times(3)).getKeyringCache();
        verify(zebedee, times(1)).getPermissionsService();
        verify(permissionsServiceImpl, times(1)).getCollectionAccessMapping(collection);
        verify(usersService, never()).removeKeyFromKeyring(EMAIL, COLLECTION_ID);
        verify(sessionsService, times(1)).find(EMAIL);
        verify(keyring, times(1)).put(COLLECTION_ID, secretKey);
        verify(schedulerCache, times(1)).put(COLLECTION_ID, secretKey);
    }

}
