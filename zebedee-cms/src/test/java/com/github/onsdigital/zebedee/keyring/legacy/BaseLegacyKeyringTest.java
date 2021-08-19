package com.github.onsdigital.zebedee.keyring.legacy;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.keyring.SchedulerKeyCache;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Base test set up for LegacyKeyringImpl - set up the mocks for the extending test classes and set default happy
 * path behaviour.
 */
public abstract class BaseLegacyKeyringTest {
    static final String EMAIL_BERT = "bert@sesamestreet.com";
    static final String EMAIL_ERNIE = "ernie@sesamestreet.com";
    static final String EMAIL_THE_COUNT = "thecount@sesamestreet.com";
    static final String TEST_COLLECTION_ID = "666";
    static final String TEST_PASSWORD = "1234567890";

    @Mock
    protected User bert, ernie, theCount;

    @Mock
    protected Session session;

    @Mock
    protected UsersService users;

    @Mock
    protected PermissionsService permissions;

    @Mock
    protected com.github.onsdigital.zebedee.json.Keyring bertKeyring;

    @Mock
    protected com.github.onsdigital.zebedee.json.Keyring ernieKeyring;

    @Mock
    protected com.github.onsdigital.zebedee.json.Keyring theCountKeyring;

    @Mock
    protected Sessions sessionsService;

    @Mock
    protected KeyringCache keyringCache;

    @Mock
    protected Collection collection;

    @Mock
    protected CollectionDescription collectionDescription;

    @Mock
    protected SecretKey secretKey;

    @Mock
    protected SchedulerKeyCache schedulerCache;

    protected CollectionKeyring legacyCollectionKeyring;
    protected KeyringException expectedEx;
    protected List<User> usersWithCollectionAccess;
    protected UserList allUsers;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        expectedEx = new KeyringException("bork");

        usersWithCollectionAccess = new ArrayList<User>() {{
            add(bert);
            add(ernie);
        }};

        allUsers = new UserList() {{
            add(bert);
            add(ernie);
            add(theCount);
        }};

        setUpMockUserBert();
        setUpMockUserErnie();
        setUpMockUserTheCount();
        setUpServiceMocksWithHappyPathBehaviour();

        setUpTests();

        legacyCollectionKeyring = new LegacyKeyringImpl(sessionsService, users, permissions, keyringCache, schedulerCache);

    }

    /**
     * Set up the service mocks with the happy path behaviour as a default - indvidual test cases can overwrite this
     * as and when they require different behaviour.
     */
    public void setUpServiceMocksWithHappyPathBehaviour() throws Exception {
        when(sessionsService.find(EMAIL_BERT))
                .thenReturn(session);

        when(collection.getDescription())
                .thenReturn(collectionDescription);

        when(collectionDescription.getId())
                .thenReturn(TEST_COLLECTION_ID);

        when(permissions.getCollectionAccessMapping(collection))
                .thenReturn(usersWithCollectionAccess);

        when(users.list())
                .thenReturn(allUsers);
    }

    private void setUpMockUserBert() throws Exception {
        when(bert.getEmail())
                .thenReturn(EMAIL_BERT);

        when(bert.keyring())
                .thenReturn(bertKeyring);

        when(bertKeyring.isUnlocked())
                .thenReturn(true);

        when(bertKeyring.unlock(TEST_PASSWORD))
                .thenReturn(true);

        when(keyringCache.get(bert))
                .thenReturn(bertKeyring);
    }

    private void setUpMockUserErnie() throws Exception {
        when(ernie.getEmail())
                .thenReturn(EMAIL_ERNIE);

        when(ernie.keyring())
                .thenReturn(ernieKeyring);

        when(ernieKeyring.isUnlocked())
                .thenReturn(true);

        when(ernieKeyring.unlock(TEST_PASSWORD))
                .thenReturn(true);

        when(keyringCache.get(ernie))
                .thenReturn(ernieKeyring);
    }

    private void setUpMockUserTheCount() throws Exception {
        when(theCount.getEmail())
                .thenReturn(EMAIL_THE_COUNT);

        when(theCount.keyring())
                .thenReturn(theCountKeyring);

        when(theCountKeyring.isUnlocked())
                .thenReturn(true);

        when(theCountKeyring.unlock(TEST_PASSWORD))
                .thenReturn(true);

        when(keyringCache.get(theCount))
                .thenReturn(theCountKeyring);
    }

    protected void verifyUsersNotUpdated(User... users) {
        verifyZeroInteractions(users);
    }

    /**
     * Verify a key was added to the specified user.
     */
    protected void verifyKeyAddedToUser(User user, com.github.onsdigital.zebedee.json.Keyring userkeyring)
            throws Exception {
        verify(users, times(1)).addKeyToKeyring(user.getEmail(), TEST_COLLECTION_ID, secretKey);
        verify(keyringCache, times(1)).get(user);
        verify(userkeyring, times(1)).put(TEST_COLLECTION_ID, secretKey);
    }

    /**
     * Verify a key was not added to the specified user.
     */
    protected void verifyKeyNotAddedToUser(User user, com.github.onsdigital.zebedee.json.Keyring userKeyring) throws Exception {
        verify(userKeyring, never()).put(eq(TEST_COLLECTION_ID), any());
        verify(users, never()).addKeyToKeyring(eq(user.getEmail()), eq(TEST_COLLECTION_ID), any());
    }

    /**
     * Verify a key was removed from the specified user.
     */
    protected void verifyKeyRemovedFromUser(User user, com.github.onsdigital.zebedee.json.Keyring userKeyring) throws Exception {
        verify(keyringCache, times(1)).get(user);
        verify(userKeyring, times(1)).remove(TEST_COLLECTION_ID);
        verify(users, times(1)).removeKeyFromKeyring(user.getEmail(), TEST_COLLECTION_ID);
    }

    /**
     * Verify a key was not removed from the specified user.
     */
    protected void verifyKeyNotRemovedFromUser(User user, com.github.onsdigital.zebedee.json.Keyring userKeyring) throws Exception {
        verify(userKeyring, never()).remove(TEST_COLLECTION_ID);
        verify(users, never()).removeKeyFromKeyring(user.getEmail(), TEST_COLLECTION_ID);
    }

    protected void verifyUserKeyringNotRetrievedFromCache(User user) throws Exception {
        verify(keyringCache, never()).get(user);
    }

    protected void verifyUserKeyringRetrievedFromCache(User user) throws Exception {
        verify(keyringCache, times(1)).get(user);
    }

    protected void verifyKeyAddedToSchedulerCache() throws Exception {
        verify(schedulerCache, times(1)).add(TEST_COLLECTION_ID, secretKey);
        verifyNoMoreInteractions(schedulerCache);
    }

    public abstract void setUpTests() throws Exception;

}

