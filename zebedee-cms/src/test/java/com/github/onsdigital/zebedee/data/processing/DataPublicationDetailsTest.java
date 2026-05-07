package com.github.onsdigital.zebedee.data.processing;

import com.github.davidcarboni.cryptolite.Keys;
import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.data.framework.DataBuilder;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.session.model.Session;
import org.junit.After;
import org.junit.Test;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by thomasridd on 1/18/16.
 */
public class DataPublicationDetailsTest extends ZebedeeTestBaseFixture {

    private static final String SESSION_ID = "1234";
    private static final String PUBLISHER_EMAIL = "publisher@example.com";
    private static final String VIEWER_EMAIL = "viewer@example.com";

    Session publisherSession;
    Session reviewerSession;

    Collection collection;
    ContentReader publishedReader;
    CollectionReader collectionReader;
    CollectionWriter collectionWriter;
    DataBuilder dataBuilder;
    DataPagesGenerator generator;
    SecretKey secretKey;


    public void setUp() throws Exception {
        secretKey = Keys.newSecretKey();

        publisherSession = new Session(SESSION_ID, PUBLISHER_EMAIL);
        reviewerSession = new Session(SESSION_ID, VIEWER_EMAIL);

        setUpKeyringMockForLegacyTests(zebedee, publisherSession, secretKey);
        setUpPermissionsServiceMockForLegacyTests(zebedee, publisherSession);

        dataBuilder = new DataBuilder(zebedee, publisherSession, reviewerSession);
        generator = new DataPagesGenerator();

        CollectionDescription collectionDescription = new CollectionDescription();
        collectionDescription.setName("DataPublicationDetails");
        collectionDescription.setType(CollectionType.scheduled);
        collectionDescription.setPublishDate(new Date());
        collection = Collection.create(collectionDescription, zebedee, publisherSession);

        publishedReader = new FileSystemContentReader(zebedee.getPublished().getPath());
        collectionReader = new ZebedeeCollectionReader(zebedee, collection, publisherSession);
        collectionWriter = new ZebedeeCollectionWriter(zebedee, collection, publisherSession);
    }

    @After
    public void tearDown() throws IOException {
        builder.delete();
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
        assertEquals(PageType.DATASET_LANDING_PAGE, details.landingPage.getType());
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
        assertEquals(PageType.TIMESERIES_DATASET, details.datasetPage.getType());
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
        assertEquals(PageType.TIMESERIES_DATASET, details.datasetPage.getType());
        assertEquals(reviewed.timeSeriesDataset.getUri().toString(), details.datasetUri);

    }

    @Test
    public void initialiser_ifCSDBFilePresent_setsFileUri() throws IOException, ZebedeeException, ParseException, URISyntaxException {
        // Given
        // a set of data pages in review
        String filename = "mydata.csdb";
        DataPagesSet reviewed = generator.generateDataPagesSet("mynode", "mydata", 2016, 2, filename);
        dataBuilder.addReviewedDataPagesSet(reviewed, collection, collectionWriter);

        // When
        // we initialise details
        DataPublicationDetails details = new DataPublicationDetails(publishedReader,
                collectionReader.getReviewed(),
                reviewed.timeSeriesDataset.getUri().toString());

        // Then
        // we expect the uri for the csdb file to be set
        assertNotNull(details.fileUri);
        assertEquals(reviewed.timeSeriesDataset.getUri().toString() + "/" + filename, details.fileUri);
    }
}


