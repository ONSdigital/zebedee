package com.github.onsdigital.zebedee;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.keyring.Keyring;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.model.encryption.ApplicationKeys;
import com.github.onsdigital.zebedee.model.encryption.EncryptionKeyFactory;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.session.service.SessionsAPIServiceImpl;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Common set up required by tests using {@link Builder} (Hide some of the nastiness).
 */
public abstract class ZebedeeTestBaseFixture {

    static final String TEST_EMAIL = "test@ons.gov.uk";

    @Mock
    protected UsersService usersService;

    @Mock
    private KeyManangerUtil keyManangerUtil;

    @Mock
    private CollectionHistoryDao collectionHistoryDao;

    @Mock
    private SessionsAPIServiceImpl sessionsService;

    @Mock
    protected ZebedeeConfiguration zebCfg;

    @Mock
    protected Sessions sessions;

    @Mock
    protected ApplicationKeys applicationKeys;

    @Mock
    protected KeyringCache legacyKeyringCache;

    @Mock
    protected Keyring collectionKeyring;

    @Mock
    protected Credentials credentials;

    @Mock
    protected User user;

    @Mock
    protected com.github.onsdigital.zebedee.json.Keyring usersKeyring;

    @Mock
    protected PermissionsService permissionsService;

    @Mock
    protected SecretKey collectionKey;

    @Mock
    protected Session userSession;

    @Mock
    protected EncryptionKeyFactory encryptionKeyFactory;

    protected Zebedee zebedee;
    protected Builder builder;
    protected Map<String, User> usersMap;

    @Before
    public void init() throws Exception {
        MockitoAnnotations.initMocks(this);
        TestUtils.initReaderConfig();

        builder = new Builder();
        zebedee = builder.getZebedee();

        UserList usersList = new UserList();
        usersList.add(builder.publisher1);

        ServiceSupplier<UsersService> usersServiceServiceSupplier = () -> usersService;

        ReflectionTestUtils.setField(zebedee, "usersService", usersService);

        // TODO I think this is a mistake.
        ReflectionTestUtils.setField(zebedee.getPermissionsService(), "usersServiceSupplier", usersServiceServiceSupplier);

        ReflectionTestUtils.setField(zebedee, "sessions", sessionsService);
        ReflectionTestUtils.setField(zebedee, "legacyKeyringCache", new KeyringCache(sessionsService));
        ReflectionTestUtils.setField(zebedee, "collectionKeyring", collectionKeyring);
        ReflectionTestUtils.setField(zebedee, "encryptionKeyFactory", encryptionKeyFactory);

        ServiceSupplier<CollectionHistoryDao> collectionHistoryDaoServiceSupplier = () -> collectionHistoryDao;

        Collection.setCollectionHistoryDaoServiceSupplier(collectionHistoryDaoServiceSupplier);
        Collection.setKeyManagerUtil(keyManangerUtil);

        usersMap = new HashMap<>();
        usersMap.put(builder.publisher1.getEmail(), builder.publisher1);

        when(usersService.getUserByEmail(builder.publisher1.getEmail()))
                .thenReturn(builder.publisher1);
        when(usersService.getUserByEmail(builder.reviewer1.getEmail()))
                .thenReturn(builder.reviewer1);
        when(usersService.getUserByEmail(builder.administrator.getEmail()))
                .thenReturn(builder.administrator);
        when(usersService.list())
                .thenReturn(usersList);

        Session session = new Session();
        session.setEmail(builder.publisher1.getEmail());
        session.setId("1234");
        session.setLastAccess(new Date());
        session.setStart(new Date());

        when(sessionsService.create(any(User.class))).thenReturn(session);
        when(sessionsService.get(anyString())).thenReturn(session);
        when(sessionsService.find(anyString())).thenReturn(session);

        Map<String, String> emailToCreds = new HashMap<>();
        emailToCreds.put(builder.publisher1.getEmail(), builder.publisher1Credentials.password);

        // UsersService is now mocked so needs to add this manually.
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                User u = invocationOnMock.getArgumentAt(1, User.class);
                String id = invocationOnMock.getArgumentAt(2, String.class);
                SecretKey key = invocationOnMock.getArgumentAt(3, SecretKey.class);

                if (emailToCreds.containsKey(u.getEmail())) {
                    u.keyring().unlock(emailToCreds.get(u.getEmail()));
                    u.keyring().put(id, key);
                }
                return null;
            }
        }).when(keyManangerUtil).assignKeyToUser(any(), any(), any(), any());

        setUp();
    }

    protected void setUpOpenSessionsTestMocks() {
        when(zebCfg.getSessions())
                .thenReturn(sessions);

        when(zebCfg.getUsersService())
                .thenReturn(usersService);

        when(zebCfg.getApplicationKeys())
                .thenReturn(applicationKeys);

        when(zebCfg.getKeyringCache())
                .thenReturn(legacyKeyringCache);

        when(zebCfg.getCollectionKeyring())
                .thenReturn(collectionKeyring);
    }

    protected void verifyKeyAddedToCollectionKeyring() throws Exception {
        verify(collectionKeyring, times(1)).add(any(), any(), any());
    }

    protected void verifyKeyAddedToCollectionKeyring(Collection collection) throws Exception {
        verify(collectionKeyring, times(1)).add(any(), eq(collection), any());
    }


    protected void setUpPermissionsServiceMockForLegacyTests(Zebedee instance, User someUser) throws Exception {
        when(permissionsService.canView(eq(someUser), any(CollectionDescription.class)))
                .thenReturn(true);

        when(permissionsService.canEdit(eq(someUser), any(CollectionDescription.class)))
                .thenReturn(true);

        ReflectionTestUtils.setField(instance, "permissionsService", permissionsService);
    }

    protected void setUpPermissionsServiceMockForLegacyTests(Zebedee instance, Session session) throws Exception {
        when(permissionsService.canView(eq(session), any(CollectionDescription.class)))
                .thenReturn(true);

        when(permissionsService.canEdit(eq(session), any(CollectionDescription.class)))
                .thenReturn(true);

        ReflectionTestUtils.setField(instance, "permissionsService", permissionsService);
    }

    protected void setUpKeyringMockForLegacyTests(Zebedee instance, User someUser, SecretKey key) throws Exception {
        when(collectionKeyring.get(eq(someUser), any(Collection.class)))
                .thenReturn(key);

        ReflectionTestUtils.setField(instance, "collectionKeyring", collectionKeyring);
    }



    public abstract void setUp() throws Exception;

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }

    @AfterClass
    public static void cleanUp() {
        TestUtils.clearReaderConfig();
    }
}
