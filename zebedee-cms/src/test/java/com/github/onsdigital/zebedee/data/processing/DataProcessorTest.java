package com.github.onsdigital.zebedee.data.processing;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.partial.Contact;
import com.github.onsdigital.zebedee.data.framework.DataBuilder;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
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
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 1/20/16.
 */
public class DataProcessorTest {
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
    DataPagesSet inReview;

    /**
     * Setup generates an instance of zebedee plus a collection
     *
     * It 
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
        inReview = generator.generateDataPagesSet("dataprocessor", "inreview", 2015, 2, "");
        dataBuilder.addReviewedDataPagesSet(inReview, collection, collectionWriter);

        // add a set of data to published
        published = generator.generateDataPagesSet("dataprocessor", "published", 2015, 2, "");
        dataBuilder.publishDataPagesSet(published);
    }

    @After
    public void tearDown() throws IOException {
        bob.delete();
    }

    @Test
    public void publishUriForTimeseries_givenDetails_returnsParentTimeseriesFolder() throws ParseException, URISyntaxException, IOException, ZebedeeException {
        // Given
        // A timeseries from our reviewed dataset
        DataPublicationDetails details = inReview.getDetails(publishedReader, collectionReader.getReviewed());
        TimeSeries series = inReview.timeSeriesList.get(0);

        // When
        // we get the publish uri for a timeseries
        String publishUri = new DataProcessor().publishUriForTimeseries(series, details);

        // Then
        // we expect it to be the cdid at the same root
        String cdid = series.getCdid();
        assertEquals(details.parentFolderUri + "/timeseries/" + cdid, publishUri);
    }

    @Test
    public void initialTimeseries_givenNewTimeseries_returnsEmptyTimeseries() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // We upload a data collection to a zebedee instance where we don't have current published content
        DataPublicationDetails details = inReview.getDetails(publishedReader, collectionReader.getReviewed());
        TimeSeries timeSeries = inReview.timeSeriesList.get(0);

        // When
        // we get the initialTimeseries
        TimeSeries initial = new DataProcessor().initialTimeseries(timeSeries, publishedReader, details);

        // Then
        // we expect it to be a skeleton timeseries
        assertEquals(PageType.timeseries, initial.getType());
        assertEquals(0, initial.years.size());
        assertEquals(0, initial.months.size());
        assertEquals(0, initial.quarters.size());
    }

    @Test
    public void initialTimeseries_givenExistingTimeseries_returnsCurrentTimeseries() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // We upload a data collection to a zebedee instance with current published content
        DataPagesSet republish = generator.generateDataPagesSet("dataprocessor", "published", 2016, 2, "");
        dataBuilder.addReviewedDataPagesSet(republish, collection, collectionWriter);

        DataPublicationDetails details = republish.getDetails(publishedReader, collectionReader.getReviewed());
        TimeSeries timeSeries = republish.timeSeriesList.get(0);

        // When
        // we get the initialTimeseries
        TimeSeries initial = new DataProcessor().initialTimeseries(timeSeries, publishedReader, details);

        // Then
        // we expect it to be the published timeseries complete with existing data
        assertEquals(PageType.timeseries, initial.getType());
        assertNotEquals(0, initial.years.size());
        assertNotEquals(0, initial.months.size());
        assertNotEquals(0, initial.quarters.size());
    }

    @Test
    public void syncMetadata_givenVariedDetailSet_takesContactsFromLandingPage() throws IOException, ZebedeeException, ParseException, URISyntaxException {

        // Given
        // A timeseries and the initial timeseries
        DataPublicationDetails details = inReview.getDetails(publishedReader, collectionReader.getReviewed());
        TimeSeries timeSeries = inReview.timeSeriesList.get(0);

        DataProcessor processor = new DataProcessor();
        TimeSeries initial = processor.initialTimeseries(timeSeries, publishedReader, details);

        // If
        // we randomise contacts
        setRandomContact(timeSeries);
        setRandomContact(initial);
        setRandomContact(details.landingPage);
        setRandomContact(details.datasetPage);


        // When
        // we sync details
        TimeSeries synced = processor.syncLandingPageMetadata(initial, details);
        synced = processor.syncNewTimeSeriesMetadata(synced, timeSeries);

        // Then
        // the timeseries should have contact details from the landing page
        assertEquals(details.landingPage.getDescription().getContact().getName(), synced.getDescription().getContact().getName());
        assertEquals(details.landingPage.getDescription().getContact().getEmail(), synced.getDescription().getContact().getEmail());
        assertEquals(details.landingPage.getDescription().getContact().getTelephone(), synced.getDescription().getContact().getTelephone());
        assertEquals(details.landingPage.getDescription().getContact().getOrganisation(), synced.getDescription().getContact().getOrganisation());
    }

    /**
     * Helper method
     */
    private void setRandomContact(Page page) {
        page.getDescription().getContact().setEmail(Random.id());
        page.getDescription().getContact().setName(Random.id());
        page.getDescription().getContact().setOrganisation(Random.id());
        page.getDescription().getContact().setTelephone(Random.id());
    }
}