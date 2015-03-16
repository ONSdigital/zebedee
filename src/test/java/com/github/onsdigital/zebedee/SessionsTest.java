package com.github.onsdigital.zebedee;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.json.Session;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SessionsTest {

    Zebedee zebedee;
    Builder builder;

    @Before
    public void setUp() throws Exception {
        builder = new Builder(this.getClass());
        zebedee = new Zebedee(builder.zebedee);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }


    @Test
    public void shouldCreateSession() throws Exception {

        // Given
        // No session have been created
        String email = "blue@cat.com";

        // When
        // We create a session
        Session session = zebedee.sessions.create(email);

        // Then
        // The session should exist
        Assert.assertNotNull(session);
        Assert.assertEquals(email, session.email);
    }


    @Test
    public void shouldNotCreateDuplicateSession() throws Exception {

        // Given
        // A session has been created
        String email = "blue@cat.com";
        Session session = zebedee.sessions.create(email);

        // When
        // We attempt to create a session for the same user
        Session newSession = zebedee.sessions.create(email);


        // Then
        // The existing session be returned
        Assert.assertNotNull(newSession);
        Assert.assertEquals(session.id, newSession.id);
    }


    @Test
    public void shouldGetSession() throws Exception {

        // Given
        // A session has been created
        String email = "blue@cat.com";
        Session existingSession = zebedee.sessions.create(email);

        // When
        // We attempt to get the session
        Session session = zebedee.sessions.get(existingSession.id);

        // Then
        // The expected session should be returned
        Assert.assertNotNull(session);
        Assert.assertEquals(existingSession.id, session.id);
    }


    @Test
    public void shouldNotGetNonexistentSession() throws Exception {

        // Given
        // No session have been created
        String email = "blue@cat.com";
        Session existingSession = zebedee.sessions.create(email);

        // When
        // We try to get a session
        Session session = zebedee.sessions.get(Random.id());

        // Then
        // No session should be returned
        Assert.assertNull(session);
    }
}