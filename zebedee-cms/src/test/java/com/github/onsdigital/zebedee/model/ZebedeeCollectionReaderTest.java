package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.reader.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ZebedeeCollectionReaderTest extends ZebedeeTestBaseFixture {

    ZebedeeCollectionReader reader;

    public void setUp() throws Exception {
        Session session = zebedee.openSession(builder.publisher1Credentials);
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        reader = new ZebedeeCollectionReader(zebedee, collection, session);
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowNullPointerIfNoUriOnReadContent()
            throws IOException, ZebedeeException {

        // Given
        // A null session
        String uri = null;

        // When
        // We attempt to read content
        reader.getResource(uri);

        // Then
        // We should get the expected exception, not a null pointer.
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundForReadingNonexistentFile()
            throws IOException, ZebedeeException {

        // Given
        // A nonexistent file
        String uri = "/this/file/doesnt/exist.json";

        // When
        // We attempt to read the file
        reader.getResource(uri);

        // Then
        // We should get the expected exception
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestForReadingADirectoryAsAFile()
            throws IOException, ZebedeeException {

        // Given
        // A uri that defines a directory
        String uri = "/this/is/a/directory/";
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.getEmail(), uri + "file.json"));

        // When
        // We attempt to read the file
        reader.getResource(uri);

        // Then
        // We should get the expected exception
    }

    @Test
    public void shouldReadFile()
            throws IOException, ZebedeeException {

        // Given
        // A nonexistent file
        String uri = "/file.json";
        Collection collection = new Collection(builder.collections.get(0), zebedee);
        assertTrue(collection.create(builder.publisher1.getEmail(), uri));

        // When
        // We attempt to read the file
        Resource resource = reader.getResource(uri);

        // Then
        // Check the expected interactions
        assertNotNull(resource);
        assertNotNull(resource.getData());
    }
}
