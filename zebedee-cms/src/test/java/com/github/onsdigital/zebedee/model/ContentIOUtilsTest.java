package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.data.framework.DataBuilder;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.Assert.assertTrue;

/**
 * Created by thomasridd on 1/24/16.
 */
@Ignore("IGNORE: user keys concurrency defect")
public class ContentIOUtilsTest {
    Zebedee zebedee;
    Builder bob;
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
    @Before
    public void setUp() throws Exception {

        bob = new Builder();
        zebedee = new Zebedee(bob.zebedee, false);

        publisher = zebedee.openSession(bob.publisher1Credentials);
        reviewer = zebedee.openSession(bob.reviewer1Credentials);

        // create a copy destination
        copy = Files.createTempDirectory("ContentIOUtils");

        // I'm using dataBuilder to speed up generation but this could use any files
        dataBuilder = new DataBuilder(zebedee, publisher, reviewer);
        generator = new DataPagesGenerator();

        CollectionDescription collectionDescription = new CollectionDescription();
        collectionDescription.name = "ContentIOUtils";
        collectionDescription.isEncrypted = true;
        collectionDescription.type = CollectionType.scheduled;
        collectionDescription.publishDate = new Date();
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

    @After
    public void tearDown() throws IOException {
        bob.delete();
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