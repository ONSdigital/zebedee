package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.service.UsersService;
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
 * Test verify behaviour or {@link Sessions}.
 */
public class SessionsTest {

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
    private SessionsIOService sessionsIOService;

    @Mock
    private Session sessionMock;

    private Credentials credentials;
    private Sessions sessions;
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

        sessions = new Sessions(sessionsPath);

        user = new User();
        user.setEmail(EMAIL);

        when(randomIdGenerator.get())
                .thenReturn(SESSION_ID);

        ReflectionTestUtils.setField(sessions, "randomIdGenerator", randomIdGenerator);
        ReflectionTestUtils.setField(sessions, "sessionsIOService", sessionsIOService);
    }

    @Test
    public void shouldCreateSession() throws IOException, NotFoundException, BadRequestException, ClassNotFoundException {
        Session expected = new Session();
        expected.email = EMAIL;
        expected.id = SESSION_ID;

        Session actual = sessions.create(user);

        assertThat(expected, equalTo(actual));
        verify(sessionsIOService, times(1)).write(expected);
        verify(randomIdGenerator, times(1)).get();
    }

    @Test
    public void shouldNotCreateDuplicateSession() throws IOException, NotFoundException, BadRequestException {
        ReflectionTestUtils.setField(sessions, "sessionsIOService", sessionsIOService);
        Path p = sessionPath();

        when(sessionsIOService.read(any()))
                .thenReturn(null);
        when(sessionMock.getEmail())
                .thenReturn(EMAIL);
        when(sessionMock.getLastAccess())
                .thenReturn(new Date());

        sessions.create(user);

        when(sessionsIOService.find(EMAIL))
                .thenReturn(sessionMock);

        when(sessionsIOService.read(any()))
                .thenReturn(sessionMock);

        sessions.create(user);

        verify(sessionsIOService, times(2)).find(EMAIL);
        verify(sessionsIOService, times(1)).write(sessionMock);
    }

    @Test
    public void shouldGetSession() throws IOException, NotFoundException, BadRequestException {
        // Create a session.
        randomIdGenerator = () -> Random.id();
        ReflectionTestUtils.setField(sessions, "randomIdGenerator", randomIdGenerator);


        // create a session.
        Session expected = new Session();
        expected.setEmail(EMAIL);
        expected.setId(SESSION_ID);

        when(sessionsIOService.exists(SESSION_ID))
                .thenReturn(true);
        when(sessionsIOService.read(sessionPath()))
                .thenReturn(expected);

        assertThat(sessions.get(SESSION_ID), equalTo(expected));
        verify(sessionsIOService, times(1)).exists(SESSION_ID);
        verify(sessionsIOService, times(1)).read(sessionPath());
    }

    @Test
    public void shouldNotGetNonexistentSession() throws IOException, NotFoundException, BadRequestException {
        when(sessionsIOService.find(EMAIL))
                .thenReturn(null);

        assertThat(sessions.find(EMAIL), equalTo(null));
        verify(sessionsIOService, times(1)).find(EMAIL);
        verify(sessionsIOService, never()).write(any(Session.class));
    }

    @Test
    public void shouldReturnNullIfEmailIsEmptyOfNull() throws IOException, NotFoundException, BadRequestException {
        Session result = sessions.create(new User());

        assertNull(result);
        verify(sessionsIOService, never()).find(anyString());
        verify(sessionsIOService, never()).write(any(Session.class));
    }

    @Test
    public void shouldFindSession() throws IOException, NotFoundException, BadRequestException {
        when(sessionsIOService.find(EMAIL))
                .thenReturn(sessionMock);
        when(sessionMock.getLastAccess())
                .thenReturn(new Date());

        assertThat(sessions.find(EMAIL), equalTo(sessionMock));
        verify(sessionsIOService, times(1)).find(EMAIL);
        verify(sessionMock, times(1)).setLastAccess(any(Date.class));
        verify(sessionMock, times(1)).getLastAccess();
        verify(sessionsIOService, times(1)).write(sessionMock);
    }

    @Test
    public void shouldNotFindNonexistentSession() throws IOException {
        when(sessionsIOService.find(EMAIL))
                .thenReturn(null);

        assertNull(sessions.find(EMAIL));
        verify(sessionsIOService, times(1)).find(EMAIL);
        verify(sessionsIOService, never()).write(any(Session.class));
    }

    @Test
    public void shouldExpireSessions() throws IOException, InterruptedException, NotFoundException, BadRequestException {
        List<Session> expired = new ArrayList<>();
        expired.add(sessionMock);

        when(sessionsIOService.filterSessions(any(Predicate.class)))
                .thenReturn(expired);
        when(sessionMock.getId())
                .thenReturn(SESSION_ID);

        sessions.deleteExpiredSessions();

        verify(sessionsIOService, times(1)).filterSessions(any(Predicate.class));
        verify(sessionMock, times(2)).getId();
        verify(sessionsIOService, times(1)).delete(sessionPath());
    }

    @Test
    public void shouldGetExpiryDate() throws Exception {
        DateTime currentTime = new DateTime();
        DateTime expected = new DateTime(currentTime).plusHours(1);

        when(sessionMock.getLastAccess())
                .thenReturn(currentTime.toDate());

        Date result = sessions.getExpiryDate(sessionMock);
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
