package com.github.onsdigital.zebedee;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.KeyringCache;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.NewSessionsServiceImpl;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Common set up required by tests using {@link Builder} (Hide some of the nastiness).
 */
public abstract class ZebedeeTestBaseFixture {

    @Mock
    private UsersService usersService;

    @Mock
    private KeyManangerUtil keyManangerUtil;

    @Mock
    private CollectionHistoryDao collectionHistoryDao;

    @Mock
    private NewSessionsServiceImpl newSessionsService;

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
        ReflectionTestUtils.setField(zebedee.getPermissionsService(), "usersServiceSupplier", usersServiceServiceSupplier);
        ReflectionTestUtils.setField(zebedee, "sessions", newSessionsService);
        ReflectionTestUtils.setField(zebedee, "keyringCache", new KeyringCache(newSessionsService));

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

        when(newSessionsService.create(any(User.class))).thenReturn(session);
        when(newSessionsService.get(anyString())).thenReturn(session);
        when(newSessionsService.find(anyString())).thenReturn(session);

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
