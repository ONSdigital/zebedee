package com.github.onsdigital.zebedee.data.processing;

import com.github.davidcarboni.ResourceUtils;
import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.httpino.Serialiser;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.data.framework.DataBuilder;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.*;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.CollectionReaderFactory;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.api.endpoint.Data;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 1/18/16.
 */
public class DataPublicationDetailsTest {
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
        collection = Collection.create(collectionDescription, zebedee, publisher);

        publishedReader = new ContentReader(zebedee.published.path);
        collectionReader = new ZebedeeCollectionReader(zebedee, collection, publisher);
        collectionWriter = new ZebedeeCollectionWriter(zebedee, collection, publisher);
    }

    @After
    public void tearDown() throws IOException {
        bob.delete();
    }

    @Test
    public void initialiser_givenReviewedDataPageSet_identifiesLandingPage() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // a reviewed dataPageSet
        DataPagesSet example = generator.generateDataPagesSet("mynode", "mydata", 2015, 2, "mydata.csdb");
        dataBuilder.addReviewedDataPagesSet(example, collection, collectionWriter);

        // When
        // we initialise details
        DataPublicationDetails details = new DataPublicationDetails(publishedReader,
                collectionReader.getReviewed(),
                example.timeSeriesDataset.getUri().toString());


        // Then
        // details landing page should be identified
        assertEquals(PageType.dataset_landing_page, details.landingPage.getType());
        assertEquals(example.datasetLandingPage.getUri().toString(), details.landingPageUri);
    }

    @Test
    public void initialiser_givenReviewedDataPageSet_identifiesTimeseries() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // a reviewed dataPageSet
        DataPagesSet example = generator.generateDataPagesSet("mynode", "mydata", 2015, 2, "mydata.csdb");
        dataBuilder.addReviewedDataPagesSet(example, collection, collectionWriter);

        // When
        // we initialise details
        DataPublicationDetails details = new DataPublicationDetails(publishedReader,
                collectionReader.getReviewed(),
                example.timeSeriesDataset.getUri().toString());


        // Then
        // details landing page should be identified
        assertEquals(PageType.timeseries_dataset, details.datasetPage.getType());
        assertEquals(example.timeSeriesDataset.getUri().toString(), details.datasetUri);
    }

    @Test
    public void initialiser_givenReviewedDataPageSet_identifiesParentUri() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // a reviewed dataPageSet
        DataPagesSet example = generator.generateDataPagesSet("mynode", "mydata", 2015, 2, "mydata.csdb");
        dataBuilder.addReviewedDataPagesSet(example, collection, collectionWriter);

        // When
        // we initialise details
        DataPublicationDetails details = new DataPublicationDetails(publishedReader,
                collectionReader.getReviewed(),
                example.timeSeriesDataset.getUri().toString());


        // Then
        // details landing page should be identified
        assertEquals("/mynode/timeseries", details.getTimeseriesFolder());
    }

    @Test
    public void initialiser_ifLandingPageNotPresent_pullsItFromPublished() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // a reviewed dataPageSet
        DataPagesSet published = generator.generateDataPagesSet("mynode", "mydata", 2015, 2, "mydata.csdb");
        dataBuilder.publishDataPagesSet(published);

        DataPagesSet reviewed = generator.generateDataPagesSet("mynode", "mydata", 2016, 2, "mydata.csdb");
        reviewed.datasetLandingPage = null;
        dataBuilder.addReviewedDataPagesSet(reviewed, collection, collectionWriter);

        // When
        // we initialise details
        DataPublicationDetails details = new DataPublicationDetails(publishedReader,
                collectionReader.getReviewed(),
                reviewed.timeSeriesDataset.getUri().toString());


        // Then
        // details landing page should be identified
        assertEquals(PageType.timeseries_dataset, details.datasetPage.getType());
        assertEquals(reviewed.timeSeriesDataset.getUri().toString(), details.datasetUri);

    }
}


