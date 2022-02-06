package com.github.onsdigital.zebedee.session.service;

import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.junit4.rules.RunInThread;
import com.github.onsdigital.zebedee.junit4.rules.RunInThreadRule;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.LegacySession;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.store.LegacySessionsStore;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThreadLocalSessionsServiceImplTest {

    private final static String SESSION_TOKEN = "7be8cdc8f0b63603eb34490c2fcb91a0a2d01a9c292dd8baf397779a22d917d9";
    private final static String EMAIL = "someone@example.com";
    private final static List<String> TEAM_IDS = Arrays.asList(new String[]{ThreadLocalSessionsServiceImpl.ADMIN_GROUP, ThreadLocalSessionsServiceImpl.PUBLISHER_GROUP, "10", "something"});

    private static ThreadLocal<Session> threadLocal = new ThreadLocal<>();

    private PermissionDefinition permissionDefinition;

    /**
     * Class under test
     */
    private ThreadLocalSessionsServiceImpl threadLocalSessionsService;

    @Mock
    private LegacySessionsStore legacySessionsStore;

    @Mock
    private PermissionsService permissionsService;

    @Mock
    private TeamsService teamsService;

    @Mock
    private Supplier<String> randomIdGenerator;

    @Mock
    private LegacySession legacySessionMock;

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
        MockitoAnnotations.openMocks(this);

        rootDir.create();

        this.permissionDefinition = new PermissionDefinition()
                .setEmail(EMAIL)
                .isEditor(true)
                .isAdmin(true);

        this.threadLocalSessionsService = new ThreadLocalSessionsServiceImpl(legacySessionsStore, permissionsService, teamsService);

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
        when(legacySessionsStore.find(EMAIL)).thenReturn(null);

        Session actual = threadLocalSessionsService.create(EMAIL);

        assertEquals(EMAIL, actual.getEmail());
        assertEquals(SESSION_TOKEN, actual.getId());
        verify(legacySessionsStore, times(1)).write(any(LegacySession.class));
        verify(randomIdGenerator, times(1)).get();
    }

    @Test
    public void create_ShouldCreateSession_WhenValidEmailAndSessionExpired() throws Exception {
        Calendar oldTime = Calendar.getInstance();
        oldTime.add(ThreadLocalSessionsServiceImpl.EXPIRY_UNIT, -(ThreadLocalSessionsServiceImpl.EXPIRY_AMOUNT+10));

        when(legacySessionMock.getLastAccess())
                .thenReturn(oldTime.getTime());

        when(legacySessionsStore.find(EMAIL)).thenReturn(legacySessionMock);

        Session actual = threadLocalSessionsService.create(EMAIL);

        assertEquals(EMAIL, actual.getEmail());
        assertEquals(SESSION_TOKEN, actual.getId());
        verify(legacySessionsStore, times(1)).write(any(LegacySession.class));
        verify(randomIdGenerator, times(1)).get();
    }

    @Test
    public void create_ShouldNotCreateDuplicateSession_WhenSessionAlreadyExists() throws Exception {
        Calendar oldTime = Calendar.getInstance();
        oldTime.add(ThreadLocalSessionsServiceImpl.EXPIRY_UNIT, -(ThreadLocalSessionsServiceImpl.EXPIRY_AMOUNT / 2));

        LegacySession session = new LegacySession(SESSION_TOKEN, EMAIL);
        session.setLastAccess(oldTime.getTime());

        when(legacySessionsStore.find(EMAIL)).thenReturn(session);

        Session actual = threadLocalSessionsService.create(EMAIL);

        assertEquals(EMAIL, actual.getEmail());
        assertEquals(SESSION_TOKEN, actual.getId());
        verify(legacySessionsStore, times(1)).write(any(LegacySession.class));
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
        when(legacySessionsStore.exists(SESSION_TOKEN))
                .thenReturn(false);

        Exception exception = assertThrows(SessionsException.class, () -> threadLocalSessionsService.set(SESSION_TOKEN));
        assertThat(exception.getMessage(), is(ThreadLocalSessionsServiceImpl.ACCESS_TOKEN_EXPIRED_ERROR));
        assertThat(threadLocal.get(), is(nullValue()));
    }

    @Test
    @RunInThread
    public void set_ShouldThrowException_WhenFailToReadSession() throws Exception {
        when(legacySessionsStore.exists(SESSION_TOKEN))
                .thenReturn(true);
        when(legacySessionsStore.read(SESSION_TOKEN))
                .thenThrow(new IOException("some error"));

        assertThrows(IOException.class, () -> threadLocalSessionsService.set(SESSION_TOKEN));
        assertThat(threadLocal.get(), is(nullValue()));
    }

    @Test
    @RunInThread
    public void set_ShouldThrowException_WhenFailToReadPermissions() throws Exception {
        LegacySession legacySession = new LegacySession(SESSION_TOKEN, EMAIL);
        Session session = new Session(SESSION_TOKEN, EMAIL);

        when(legacySessionsStore.exists(SESSION_TOKEN))
                .thenReturn(true);
        when(legacySessionsStore.read(SESSION_TOKEN))
                .thenReturn(legacySession);
        when(permissionsService.userPermissions(EMAIL, session))
                .thenThrow(new IOException("some error"));

        assertThrows(IOException.class, () -> threadLocalSessionsService.set(SESSION_TOKEN));
        assertThat(threadLocal.get(), is(nullValue()));
    }

    @Test
    @RunInThread
    public void set_ShouldThrowException_WhenFailToReadTeams() throws Exception {
        LegacySession legacySession = new LegacySession(SESSION_TOKEN, EMAIL);
        Session session = new Session(SESSION_TOKEN, EMAIL);

        when(legacySessionsStore.exists(SESSION_TOKEN))
                .thenReturn(true);
        when(legacySessionsStore.read(SESSION_TOKEN))
                .thenReturn(legacySession);
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
        LegacySession legacySession = new LegacySession(SESSION_TOKEN, EMAIL);
        Session session = new Session(SESSION_TOKEN, EMAIL);
        List<String> teams = new ArrayList<>();
        teams.add("10");
        teams.add("something");

        when(legacySessionsStore.exists(SESSION_TOKEN))
                .thenReturn(true);
        when(legacySessionsStore.read(SESSION_TOKEN))
                .thenReturn(legacySession);
        when(permissionsService.userPermissions(session))
                .thenReturn(permissionDefinition);
        when(teamsService.listTeamsForUser(session))
                .thenReturn(TEAM_IDS);

        threadLocalSessionsService.set(SESSION_TOKEN);

        Session actual = threadLocal.get();
        assertNotNull(actual);
        assertEquals(SESSION_TOKEN, actual.getId());
        assertEquals(EMAIL, actual.getEmail());
        assertEquals(ThreadLocalSessionsServiceImpl.ADMIN_GROUP, actual.getGroups().get(0));
        assertEquals(ThreadLocalSessionsServiceImpl.PUBLISHER_GROUP, actual.getGroups().get(1));
        assertEquals(teams.get(0), actual.getGroups().get(2));
        assertEquals(teams.get(1), actual.getGroups().get(3));
    }

    @Test
    @RunInThread
    public void set_ShouldClearThread_WhenSetAgain() throws Exception {
        threadLocal.set(new Session(SESSION_TOKEN, EMAIL));

        assertThrows(SessionsException.class, () -> threadLocalSessionsService.set(""));

        assertNull(threadLocal.get());
    }

    @Test
    @RunInThread
    public void get_ShouldReturnSession_WhenValidSession() throws Exception {
        Session session = new Session(SESSION_TOKEN, EMAIL, TEAM_IDS);
        threadLocal.set(session);

        Session actual = threadLocalSessionsService.get();

        assertEquals(session, actual);
    }

    @Test
    @RunInThread
    public void get_ShouldReturnNull_WhenNoSession() throws Exception {
        assertNull(threadLocalSessionsService.get());
    }

    @Test
    @RunInThread
    public void resetThread_ShouldClearSessions() throws Exception {
        threadLocal.set(new Session(SESSION_TOKEN, EMAIL));

        threadLocalSessionsService.resetThread();

        assertNull(threadLocal.get());
    }
}
