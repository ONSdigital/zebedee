package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.Session;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class SessionsTest {

    Zebedee zebedee;
    Builder builder;

    int expiryUnit;
    int expiryAmount;


    @Before
    public void setUp() throws Exception {
        builder = new Builder();
        zebedee = new Zebedee(builder.zebedee, false);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }


    @Test
    public void shouldCreateSession() throws IOException, NotFoundException, BadRequestException {

        // Given
        // No session have been created
        Credentials credentials = builder.administratorCredentials;

        // When
        // We create a session
        Session session = zebedee.openSession(credentials);

        // Then
        // The session should exist
        Assert.assertNotNull(session);
        Assert.assertEquals(credentials.email, session.email);
    }


    @Test
    public void shouldNotCreateDuplicateSession() throws IOException, NotFoundException, BadRequestException {

        // Given
        // A session has been created
        Credentials credentials = builder.administratorCredentials;
        Session session = zebedee.openSession(credentials);

        // When
        // We attempt to create a session for the same user
        Session newSession = zebedee.openSession(credentials);


        // Then
        // The existing session be returned
        Assert.assertNotNull(newSession);
        Assert.assertEquals(session.id, newSession.id);
    }

    @Test
    public void shouldGetSession() throws IOException, NotFoundException, BadRequestException {

        // Given
        // A session has been created
        Credentials credentials = builder.administratorCredentials;
        Session existingSession = zebedee.openSession(credentials);

        // When
        // We attempt to get the session
        Session session = zebedee.sessions.get(existingSession.id);

        // Then
        // The expected session should be returned
        Assert.assertNotNull(session);
        Assert.assertEquals(existingSession.id, session.id);
    }

    @Test
    public void shouldNotGetNonexistentSession() throws IOException, NotFoundException, BadRequestException {

        // Given
        // No session have been created
        Credentials credentials = builder.publisher2Credentials;

        // When
        // We try to get a session
        Session session = zebedee.sessions.find(credentials.email);

        // Then
        // No session should be returned
        Assert.assertNull(session);
    }

    @Test
    public void shouldNotThrowErrorForNullSessionToken() throws IOException, NotFoundException, BadRequestException {

        // Given
        // An empty session token
        Credentials token = null;

        // When
        // We try to get a session
        Session session = zebedee.openSession(token);

        // Then
        // No error should be thrown
        Assert.assertNull(session);
    }

    @Test
    public void shouldGetSessionConcurrently() throws IOException, InterruptedException, NotFoundException, BadRequestException {

        // Given
        // A session has been created
        Credentials credentials = builder.administratorCredentials;
        Session existingSession = zebedee.openSession(credentials);


        ExecutorService executor = Executors.newCachedThreadPool();

        List<GetSession> runnables = new ArrayList<>();

        for (int i = 0; i < 20; ++i) {
            runnables.add(new GetSession(existingSession));
        }

        for (GetSession runnable : runnables) {
            executor.execute(runnable);
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        for (GetSession runnable : runnables) {
            assertFalse(runnable.failed);
        }
    }

    @Test
    public void shouldFindSession() throws IOException, NotFoundException, BadRequestException {

        // Given
        // A session has been created
        Credentials credentials = builder.administratorCredentials;
        Session existingSession = zebedee.openSession(credentials);

        // When
        // We attempt to get the session
        Session session = zebedee.sessions.find(credentials.email);

        // Then
        // The expected session should be returned
        Assert.assertNotNull(session);
        Assert.assertEquals(existingSession.id, session.id);
    }

    @Test
    public void shouldNotFindNonexistentSession() throws IOException {

        // Given
        // No session has been created for a given email
        String email = Random.id() + "@nonexistent.com";

        // When
        // We try to get a session
        Session session = zebedee.sessions.find(email);

        // Then
        // No session should be returned
        Assert.assertNull(session);
    }

    @Test
    public void shouldNotThrowErrorForNullEmail() throws IOException {

        // Given
        // A null email
        String email = null;

        // When
        // We try to find a session
        Session session = zebedee.sessions.find(email);

        // Then
        // No error should be thrown
        Assert.assertNull(session);
    }

    @Test
    public void shouldExpireSessions() throws IOException, InterruptedException, NotFoundException, BadRequestException {

        // Given
        // A short expiry time and a session
        Credentials credentials = builder.administratorCredentials;
        Session session = zebedee.openSession(credentials);
        zebedee.sessions.setExpiry(1, Calendar.MILLISECOND);

        // When
        // We clear out expired sessions
        Thread.sleep(10);
        zebedee.sessions.deleteExpiredSessions();

        // Then
        // The session should be deleted
        Assert.assertNull(zebedee.sessions.get(session.id));
    }

    public class GetSession implements Runnable {
        public boolean failed = false;
        private Session session = null;

        public GetSession(Session session) {
            this.session = session;
        }

        @Override
        public void run() {
            try {
                // When
                // We attempt to get the session

                Session session = zebedee.sessions.get(this.session.id);

                System.out.println(session);

                // Then
                // The expected session should be returned
                if (session == null || !StringUtils.equals(session.id, this.session.id))
                    failed = true;

            } catch (IOException e) {
                fail();
            }
        }
    }
}