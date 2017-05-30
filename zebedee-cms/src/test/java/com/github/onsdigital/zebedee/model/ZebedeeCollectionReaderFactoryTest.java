package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.session.model.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class ZebedeeCollectionReaderFactoryTest {

    Zebedee zebedee;
    Builder builder;
    ZebedeeCollectionReaderFactory factory;

    @Before
    public void setUp() throws Exception {
        builder = new Builder();
        zebedee = new Zebedee(builder.zebedee, false);
        factory = new ZebedeeCollectionReaderFactory(zebedee);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundForNullCollection()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given a null collection
        Collection collection = null;
        Session session = zebedee.openSession(builder.administratorCredentials);

        // When we attempt to create a collection reader.
        factory.getCollectionReader(collection, session);

        // Then we should get the expected exception, not a null pointer.
    }

    @Test(expected = UnauthorizedException.class)
    public void shouldThrowUnauthorizedIfNotLoggedInOnReadContent()
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Given a null session
        Session session = null;
        Collection collection = new Collection(builder.collections.get(0), zebedee);

        // When we attempt to create a collection reader.
        factory.getCollectionReader(collection, session);

        // Then
        // We should get the expected exception, not a null pointer.
    }
}
