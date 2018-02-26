package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.KeyManangerUtil;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import com.github.onsdigital.zebedee.data.framework.DataBuilder;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Created by thomasridd on 1/24/16.
 */
public class ContentIOUtilsTest extends ZebedeeTestBaseFixture {

    @Mock
    private UsersService usersService;

    @Mock
    private KeyManangerUtil keyManangerUtil;

    @Mock
    private CollectionHistoryDao collectionHistoryDao;

    Session publisher;
    Session reviewer;

    Path copy;

    Collection collection;
    ContentReader publishedReader;
    CollectionReader collectionReader;
    CollectionWriter collectionWriter;
    DataBuilder dataBuilder;
    DataPagesGenerator generator;

    DataPagesSet published;
    DataPagesSet encrypted;

    /**
     * Setup generates an instance of zebedee, a collection, and various DataPagesSet objects (that are test framework generators)
     *
     * @throws Exception
     */
    @Override
    public void setUp() throws Exception {
        publisher = zebedee.openSession(builder.publisher1Credentials);
        reviewer = zebedee.openSession(builder.reviewer1Credentials);

        // create a copy destination
        copy = Files.createTempDirectory("ContentIOUtils");

        // I'm using dataBuilder to speed up generation but this could use any files
        dataBuilder = new DataBuilder(zebedee, publisher, reviewer);
        generator = new DataPagesGenerator();

        CollectionDescription collectionDescription = new CollectionDescription();
        collectionDescription.setName("ContentIOUtils");
        collectionDescription.isEncrypted = true;
        collectionDescription.setType(CollectionType.scheduled);
        collectionDescription.setPublishDate(new Date());
        collection = Collection.create(collectionDescription, zebedee, publisher);

        publishedReader = new FileSystemContentReader(zebedee.getPublished().path);
        collectionReader = new ZebedeeCollectionReader(zebedee, collection, publisher);
        collectionWriter = new ZebedeeCollectionWriter(zebedee, collection, publisher);

        // add a set of data in a collection
        encrypted = generator.generateDataPagesSet("dataprocessor", "encrypted", 2015, 2, "inreview.csdb");
        dataBuilder.addReviewedDataPagesSet(encrypted, collection, collectionWriter);

        // add a set of data to published
        published = generator.generateDataPagesSet("dataprocessor", "published", 2015, 2, "");
        dataBuilder.publishDataPagesSet(published);

    }

    @Override
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(copy.toFile());

    }

    @Test
    public void copyContent_givenPlainContent_doesRunCopy() throws IOException, ZebedeeException {
        // Given
        // a reader for
        ContentWriter writer = new ContentWriter(copy);
        ContentReader reader = publishedReader;
        String example = published.datasetLandingPage.getUri().toString() + "/data.json";

        // When
        // we run the copy
        ContentIOUtils.copy(reader, writer);

        // Then
        // we expect the uri's from our published set to have been copied
        assertTrue(Files.exists(uriResolve(copy, example)));
    }

    @Test
    public void copyContent_givenCollectionContent_doesRunCopy() throws IOException, ZebedeeException {
        // Given
        // a reader for
        ContentWriter writer = new ContentWriter(copy);
        ContentReader reader = collectionReader.getReviewed();
        String example = encrypted.datasetLandingPage.getUri().toString() + "/data.json";

        // When
        // we run the copy
        ContentIOUtils.copy(reader, writer);

        // Then
        // we expect the uri's from our published set to have been copied
        assertTrue(Files.exists(uriResolve(copy, example)));
    }

    private Path uriResolve(Path root, String uri) {
        if (uri.startsWith("/")) {
            return root.resolve(uri.substring(1, uri.length()));
        } else {
            return root.resolve(uri);
        }
    }
}