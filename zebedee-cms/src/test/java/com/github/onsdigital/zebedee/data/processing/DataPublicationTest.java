package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.framework.DataBuilder;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 1/24/16.
 *
 * Tests for the data publication object
 */
public class DataPublicationTest {

    Zebedee zebedee;
    Builder bob;
    Session publisher;
    Session reviewer;

    Collection collection;
    ContentReader publishedReader;
    CollectionReader collectionReader;
    CollectionWriter collectionWriter;
    DataBuilder dataBuilder;
    DataPagesGenerator generator;

    DataPagesSet published;
    DataPagesSet unpublished;
    DataPagesSet republish;

    /**
     * Setup generates an instance of zebedee, a collection, and various DataPagesSet objects (that are test framework generators)
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {

        bob = new Builder(DataPublicationDetailsTest.class);
        zebedee = new Zebedee(bob.zebedee, false);

        publisher = zebedee.openSession(bob.publisher1Credentials);
        reviewer = zebedee.openSession(bob.reviewer1Credentials);

        dataBuilder = new DataBuilder(zebedee, publisher, reviewer);
        generator = new DataPagesGenerator();

        CollectionDescription collectionDescription = new CollectionDescription();
        collectionDescription.name = "DataPublicationDetails";
        collectionDescription.isEncrypted = true;
        collection = Collection.create(collectionDescription, zebedee, publisher);

        publishedReader = new ContentReader(zebedee.published.path);
        collectionReader = new ZebedeeCollectionReader(zebedee, collection, publisher);
        collectionWriter = new ZebedeeCollectionWriter(zebedee, collection, publisher);

        // add a set of data in a collection
        unpublished = generator.generateDataPagesSet("dataprocessor", "inreview", 2015, 2, "inreview.csdb");
        dataBuilder.addReviewedDataPagesSet(unpublished, collection, collectionWriter);

        // add a set of data to published
        published = generator.generateDataPagesSet("dataprocessor", "published", 2015, 2, "");
        dataBuilder.publishDataPagesSet(published);

        // generate a set of data that will replace the data in published
        republish = generator.generateDataPagesSet("dataprocessor", "published", 2016, 2, "");
    }

    @After
    public void tearDown() throws IOException {
        bob.delete();
    }

    @Test
    public void publication_givenPublicationInReview_doesInitialise() throws ZebedeeException, IOException {
        // Given
        // our data in review
        DataPublicationDetails details = unpublished.getDetails(publishedReader, collectionReader.getReviewed());

        // When
        // we initialise publication
        DataPublication publication = new DataPublication(publishedReader, collectionReader.getReviewed(), details.datasetUri);


        // Then
        // we expect it to be not null
        assertNotNull(publication);
    }

    @Test
    public void publication_givenPublicationInReview_doesProcess() throws ZebedeeException, IOException, URISyntaxException {
        // Given
        // our data in review
        DataPublicationDetails details = unpublished.getDetails(publishedReader, collectionReader.getReviewed());
        DataPublication publication = new DataPublication(publishedReader, collectionReader.getReviewed(), details.datasetUri);
        publication.setDataLink(new DataLinkMock(unpublished.getTimeSerieses()));

        // When
        // we initialise publication
        publication.process(publishedReader, collectionReader.getReviewed(), collectionWriter.getReviewed());

        // Then
        // we expect it to be not null
        assertNotNull(publication);
    }
}