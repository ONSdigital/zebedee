package com.github.onsdigital.zebedee.session.service;

import com.github.onsdigital.impl.UserDataPayload;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.junit4.rules.RunInThread;
import com.github.onsdigital.zebedee.junit4.rules.RunInThreadRule;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.store.SessionsStore;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThreadLocalSessionsServiceImplTest {

    private final static String SESSION_TOKEN = "7be8cdc8f0b63603eb34490c2fcb91a0a2d01a9c292dd8baf397779a22d917d9";
    private final static String EMAIL = "someone@example.com";
    private final static String[] TEAM_IDS = {"publishing-role", "1", "something"};

    private static ThreadLocal<UserDataPayload> threadLocal = new ThreadLocal<>();

    private PermissionDefinition permissionDefinition;
    private UserDataPayload userDataPayload;

    /**
     * Class under test
     */
    private ThreadLocalSessionsServiceImpl threadLocalSessionsService;

    @Mock
    private SessionsStore sessionsStore;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private TeamsService teamsService;

    @Mock
    private Supplier<String> randomIdGenerator;

    @Rule
    public TemporaryFolder rootDir = new TemporaryFolder();

    @Rule
    public RunInThreadRule runInThread = new RunInThreadRule();

    @BeforeClass
    public static void staticSetUp() {
        ThreadLocalSessionsServiceImpl.setStore(threadLocal);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        rootDir.create();

        this.permissionDefinition = new PermissionDefinition()
                .setEmail(EMAIL)
                .isEditor(true)
                .isAdmin(true);

        this.userDataPayload = new UserDataPayload(EMAIL, TEAM_IDS);

        this.threadLocalSessionsService = new ThreadLocalSessionsServiceImpl(sessionsStore, permissionsService, teamsService);

        ReflectionTestUtils.setField(threadLocalSessionsService, "randomIdGenerator", randomIdGenerator);

        when(randomIdGenerator.get())
                .thenReturn(SESSION_TOKEN);
    }

    @Test
    public void create_ShouldNotCreateSession_WhenEmailNull() throws Exception {
        assertNull(threadLocalSessionsService.create(null));
    }

    @Test
    public void create_ShouldNotCreateSession_WhenEmailEmpty() throws Exception {
        assertNull(threadLocalSessionsService.create(""));
    }

    @Test
    public void create_ShouldNotCreateSession_WhenEmailBlank() throws Exception {
        assertNull(threadLocalSessionsService.create("   "));
    }

    @Test
    public void create_ShouldCreateSession_WhenValidEmailAndNoSessionExists() throws Exception {
        when(sessionsStore.find(EMAIL)).thenReturn(null);

        Session actual = threadLocalSessionsService.create(EMAIL);

        assertEquals(EMAIL, actual.getEmail());
        assertEquals(SESSION_TOKEN, actual.getId());
        verify(sessionsStore, times(1)).write(actual);
        verify(randomIdGenerator, times(1)).get();
    }

    @Test
    public void create_ShouldCreateSession_WhenValidEmailAndSessionExpired() throws Exception {
        Calendar oldTime = Calendar.getInstance();
        oldTime.add(ThreadLocalSessionsServiceImpl.EXPIRY_UNIT, -(ThreadLocalSessionsServiceImpl.EXPIRY_AMOUNT+10));

        Session session = new Session();
        session.setEmail(EMAIL);
        session.setId("old-token");
        session.setLastAccess(oldTime.getTime());

        when(sessionsStore.find(EMAIL)).thenReturn(session);

        Session actual = threadLocalSessionsService.create(EMAIL);

        assertEquals(EMAIL, actual.getEmail());
        assertEquals(SESSION_TOKEN, actual.getId());
        assertTrue(oldTime.getTime().before(actual.getLastAccess()));
        verify(sessionsStore, times(1)).write(actual);
        verify(randomIdGenerator, times(1)).get();
    }

    @Test
    public void create_ShouldNotCreateDuplicateSession_WhenSessionAlreadyExists() throws Exception {
        Calendar oldTime = Calendar.getInstance();
        oldTime.add(ThreadLocalSessionsServiceImpl.EXPIRY_UNIT, -(ThreadLocalSessionsServiceImpl.EXPIRY_AMOUNT / 2));

        Session session = new Session();
        session.setEmail(EMAIL);
        session.setId(SESSION_TOKEN);
        session.setLastAccess(oldTime.getTime());

        when(sessionsStore.find(EMAIL)).thenReturn(session);

        Session actual = threadLocalSessionsService.create(EMAIL);

        assertEquals(EMAIL, actual.getEmail());
        assertEquals(SESSION_TOKEN, actual.getId());
        assertTrue(oldTime.getTime().before(actual.getLastAccess()));
        verify(sessionsStore, times(1)).write(actual);
        verify(randomIdGenerator, times(0)).get();
    }

    @Test
    @RunInThread
    public void set_ShouldThrowException_WhenTokenEmpty() throws Exception {
        Exception exception = assertThrows(SessionsException.class, () -> threadLocalSessionsService.set(""));
        assertThat(exception.getMessage(), is(ThreadLocalSessionsServiceImpl.ACCESS_TOKEN_REQUIRED_ERROR));
        assertThat(threadLocal.get(), is(nullValue()));
    }

    @Test
    @RunInThread
    public void set_ShouldThrowException_WhenTokenHasExpired() throws Exception {
        when(sessionsStore.exists(SESSION_TOKEN))
                .thenReturn(false);

        Exception exception = assertThrows(SessionsException.class, () -> threadLocalSessionsService.set(SESSION_TOKEN));
        assertThat(exception.getMessage(), is(ThreadLocalSessionsServiceImpl.ACCESS_TOKEN_EXPIRED_ERROR));
        assertThat(threadLocal.get(), is(nullValue()));
    }

    @Test
    @RunInThread
    public void set_ShouldThrowException_WhenFailToReadSession() throws Exception {
        when(sessionsStore.exists(SESSION_TOKEN))
                .thenReturn(true);
        when(sessionsStore.read(SESSION_TOKEN))
                .thenThrow(new IOException("some error"));

        assertThrows(IOException.class, () -> threadLocalSessionsService.set(SESSION_TOKEN));
        assertThat(threadLocal.get(), is(nullValue()));
    }

    @Test
    @RunInThread
    public void set_ShouldThrowException_WhenFailToReadPermissions() throws Exception {
        Session session = new Session();

        when(sessionsStore.exists(SESSION_TOKEN))
                .thenReturn(true);
        when(sessionsStore.read(SESSION_TOKEN))
                .thenReturn(session);
        when(permissionsService.userPermissions(session))
                .thenThrow(new IOException("some error"));

        assertThrows(IOException.class, () -> threadLocalSessionsService.set(SESSION_TOKEN));
        assertThat(threadLocal.get(), is(nullValue()));
    }

    @Test
    @RunInThread
    public void set_ShouldThrowException_WhenFailToReadTeams() throws Exception {
        Session session = new Session();
        session.setEmail(EMAIL);

        when(sessionsStore.exists(SESSION_TOKEN))
                .thenReturn(true);
        when(sessionsStore.read(SESSION_TOKEN))
                .thenReturn(session);
        when(permissionsService.userPermissions(session))
                .thenReturn(permissionDefinition);
        when(teamsService.listTeamsForUser(session))
                .thenThrow(new IOException("some error"));

        assertThrows(IOException.class, () -> threadLocalSessionsService.set(SESSION_TOKEN));
        assertThat(threadLocal.get(), is(nullValue()));
    }

    @Test
    @RunInThread
    public void set_ShouldSetSession_WhenTokenValid() throws Exception {
        Session session = new Session();
        session.setEmail(EMAIL);

        when(sessionsStore.exists(SESSION_TOKEN))
                .thenReturn(true);
        when(sessionsStore.read(SESSION_TOKEN))
                .thenReturn(session);
        when(permissionsService.userPermissions(session))
                .thenReturn(permissionDefinition);
        when(teamsService.listTeamsForUser(session))
                .thenReturn(Arrays.asList(TEAM_IDS));

        threadLocalSessionsService.set(SESSION_TOKEN);

        UserDataPayload actual = threadLocal.get();
        assertThat(actual, is(notNullValue()));
        assertThat(actual.getEmail(), is(EMAIL));
        assertThat(actual.getGroups()[0], is(ThreadLocalSessionsServiceImpl.ADMIN_GROUP));
        assertThat(actual.getGroups()[1], is(ThreadLocalSessionsServiceImpl.PUBLISHER_GROUP));
        assertThat(actual.getGroups()[2], is(TEAM_IDS[0]));
    }

    @Test
    @RunInThread
    public void set_ShouldClearThread_WhenSetAgain() throws Exception {
        threadLocal.set(userDataPayload);

        assertThrows(SessionsException.class, () -> threadLocalSessionsService.set(""));

        assertNull(threadLocal.get());
    }

    @Test
    @RunInThread
    public void get_ShouldReturnSession_WhenValidSession() throws Exception {
        threadLocal.set(userDataPayload);

        Session actual = threadLocalSessionsService.get();

        assertThat(actual.getEmail(), is(EMAIL));
        assertThat(actual.getGroups(), is(TEAM_IDS));
    }

    @Test
    @RunInThread
    public void get_ShouldReturnNull_WhenNoSession() throws Exception {
        assertThat(threadLocalSessionsService.get(), is(nullValue()));
    }

    @Test
    @RunInThread
    public void resetThread_ShouldClearSessions() throws Exception {
        threadLocal.set(userDataPayload);

        threadLocalSessionsService.resetThread();

        assertNull(threadLocal.get());
    }
}
