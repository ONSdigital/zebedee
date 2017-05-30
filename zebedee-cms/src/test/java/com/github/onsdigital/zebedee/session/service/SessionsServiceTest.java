package com.github.onsdigital.zebedee.session.service;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import com.github.onsdigital.zebedee.session.store.SessionsStoreImpl;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.github.onsdigital.zebedee.Zebedee.SESSIONS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test verify behaviour or {@link SessionsService}.
 */
public class SessionsServiceTest {

    private static ObjectMapper OBJ_MAPPER = new ObjectMapper();
    private static final String EMAIL = "TEST@ons.gov.uk";
    private static final String PWD = "1 2 3 4";
    private static final String SESSION_ID = "1234567890";
    private static final String JSON_EXT = ".json";

    @Rule
    public TemporaryFolder rootDir = new TemporaryFolder();

    @Mock
    private UsersService usersService;

    @Mock
    private Supplier<String> randomIdGenerator;

    @Mock
    private SessionsStoreImpl sessionsStore;

    @Mock
    private Session sessionMock;

    private Credentials credentials;
    private SessionsService sessionsService;
    private Path sessionsPath;
    private User user;

    private Path sessionPath() {
        return sessionsPath.resolve(SESSION_ID + JSON_EXT);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        rootDir.create();
        sessionsPath = rootDir.newFolder(SESSIONS).toPath();

        credentials = new Credentials();
        credentials.email = EMAIL;
        credentials.password = PWD;

        sessionsService = new SessionsService(sessionsPath);

        user = new User();
        user.setEmail(EMAIL);

        when(randomIdGenerator.get())
                .thenReturn(SESSION_ID);

        ReflectionTestUtils.setField(sessionsService, "randomIdGenerator", randomIdGenerator);
        ReflectionTestUtils.setField(sessionsService, "sessionsStore", sessionsStore);
    }

    @Test
    public void shouldCreateSession() throws IOException, NotFoundException, BadRequestException, ClassNotFoundException {
        Session expected = new Session();
        expected.setEmail(EMAIL);
        expected.setId(SESSION_ID);

        Session actual = sessionsService.create(user);

        assertThat(expected, equalTo(actual));
        verify(sessionsStore, times(1)).write(expected);
        verify(randomIdGenerator, times(1)).get();
    }

    @Test
    public void shouldNotCreateDuplicateSession() throws IOException, NotFoundException, BadRequestException {
        ReflectionTestUtils.setField(sessionsService, "sessionsStore", sessionsStore);
        Path p = sessionPath();

        when(sessionsStore.read(any()))
                .thenReturn(null);
        when(sessionMock.getEmail())
                .thenReturn(EMAIL);
        when(sessionMock.getLastAccess())
                .thenReturn(new Date());

        sessionsService.create(user);

        when(sessionsStore.find(EMAIL))
                .thenReturn(sessionMock);

        when(sessionsStore.read(any()))
                .thenReturn(sessionMock);

        sessionsService.create(user);

        verify(sessionsStore, times(2)).find(EMAIL);
        verify(sessionsStore, times(1)).write(sessionMock);
    }

    @Test
    public void shouldGetSession() throws IOException, NotFoundException, BadRequestException {
        // Create a session.
        randomIdGenerator = () -> Random.id();
        ReflectionTestUtils.setField(sessionsService, "randomIdGenerator", randomIdGenerator);


        // create a session.
        Session expected = new Session();
        expected.setEmail(EMAIL);
        expected.setId(SESSION_ID);

        when(sessionsStore.exists(SESSION_ID))
                .thenReturn(true);
        when(sessionsStore.read(sessionPath()))
                .thenReturn(expected);

        assertThat(sessionsService.get(SESSION_ID), equalTo(expected));
        verify(sessionsStore, times(1)).exists(SESSION_ID);
        verify(sessionsStore, times(1)).read(sessionPath());
    }

    @Test
    public void shouldNotGetNonexistentSession() throws IOException, NotFoundException, BadRequestException {
        when(sessionsStore.find(EMAIL))
                .thenReturn(null);

        assertThat(sessionsService.find(EMAIL), equalTo(null));
        verify(sessionsStore, times(1)).find(EMAIL);
        verify(sessionsStore, never()).write(any(Session.class));
    }

    @Test
    public void shouldReturnNullIfEmailIsEmptyOfNull() throws IOException, NotFoundException, BadRequestException {
        Session result = sessionsService.create(new User());

        assertNull(result);
        verify(sessionsStore, never()).find(anyString());
        verify(sessionsStore, never()).write(any(Session.class));
    }

    @Test
    public void shouldFindSession() throws IOException, NotFoundException, BadRequestException {
        when(sessionsStore.find(EMAIL))
                .thenReturn(sessionMock);
        when(sessionMock.getLastAccess())
                .thenReturn(new Date());

        assertThat(sessionsService.find(EMAIL), equalTo(sessionMock));
        verify(sessionsStore, times(1)).find(EMAIL);
        verify(sessionMock, times(1)).setLastAccess(any(Date.class));
        verify(sessionMock, times(1)).getLastAccess();
        verify(sessionsStore, times(1)).write(sessionMock);
    }

    @Test
    public void shouldNotFindNonexistentSession() throws IOException {
        when(sessionsStore.find(EMAIL))
                .thenReturn(null);

        assertNull(sessionsService.find(EMAIL));
        verify(sessionsStore, times(1)).find(EMAIL);
        verify(sessionsStore, never()).write(any(Session.class));
    }

    @Test
    public void shouldExpireSessions() throws IOException, InterruptedException, NotFoundException, BadRequestException {
        List<Session> expired = new ArrayList<>();
        expired.add(sessionMock);

        when(sessionsStore.filterSessions(any(Predicate.class)))
                .thenReturn(expired);
        when(sessionMock.getId())
                .thenReturn(SESSION_ID);

        sessionsService.deleteExpiredSessions();

        verify(sessionsStore, times(1)).filterSessions(any(Predicate.class));
        verify(sessionMock, times(2)).getId();
        verify(sessionsStore, times(1)).delete(sessionPath());
    }

    @Test
    public void shouldGetExpiryDate() throws Exception {
        DateTime currentTime = new DateTime();
        DateTime expected = new DateTime(currentTime).plusHours(1);

        when(sessionMock.getLastAccess())
                .thenReturn(currentTime.toDate());

        Date result = sessionsService.getExpiryDate(sessionMock);
        DateTime actual = new DateTime(result);

        assertThat(actual.getYear(), equalTo(expected.getYear()));
        assertThat(actual.getMonthOfYear(), equalTo(expected.getMonthOfYear()));
        assertThat(actual.getDayOfMonth(), equalTo(expected.getDayOfMonth()));
        assertThat(actual.getHourOfDay(), equalTo(expected.getHourOfDay()));
        assertThat(actual.getMinuteOfHour(), equalTo(expected.getMinuteOfHour()));
        assertThat(actual.getSecondOfMinute(), equalTo(expected.getSecondOfMinute()));
        verify(sessionMock, times(1)).getLastAccess();
    }

}
