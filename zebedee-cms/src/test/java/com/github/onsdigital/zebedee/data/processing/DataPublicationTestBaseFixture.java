package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import com.github.onsdigital.zebedee.data.framework.DataBuilder;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateCommand;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.model.*;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by thomasridd on 1/24/16.
 *
 * Tests for the data publication object
 */
public class DataPublicationTestBaseFixture extends ZebedeeTestBaseFixture {

    final List<TimeseriesUpdateCommand> updateCommands = new ArrayList<>();
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
    public void setUp() throws Exception {
        publisher = zebedee.openSession(builder.publisher1Credentials);
        reviewer = zebedee.openSession(builder.reviewer1Credentials);

        dataBuilder = new DataBuilder(zebedee, publisher, reviewer);
        generator = new DataPagesGenerator();

        CollectionDescription collectionDescription = new CollectionDescription();
        collectionDescription.setName("DataPublicationDetails");
        collectionDescription.isEncrypted = true;
        collectionDescription.setType(CollectionType.scheduled);
        collectionDescription.setPublishDate(new Date());
        collection = Collection.create(collectionDescription, zebedee, publisher);

        publishedReader = new FileSystemContentReader(zebedee.getPublished().path);
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
        publication.process(publishedReader, collectionReader.getReviewed(), collectionWriter.getReviewed(), zebedee.getDataIndex(), updateCommands);

        // Then
        // we expect it to be not null
        assertNotNull(publication);

        // generate the files uncoded for checking
        Path idiotCheck = generateIdiotCheck(collectionReader.getReviewed());
    }

    @Test
    public void publication_givenLandingPageWithoutDatasetId_generatesFromCSDBFileName() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // we generate a publish with a fresh csdb upload
        DataPagesSet pagesSet = generator.generateDataPagesSet("datasetIds", "temp", 2015, 2, "abcd.csdb");
        pagesSet.datasetLandingPage.getDescription().setDatasetId("");
        dataBuilder.addReviewedDataPagesSet(pagesSet, collection, collectionWriter);

        DataPublicationDetails details = pagesSet.getDetails(publishedReader, collectionReader.getReviewed());
        DataPublication publication = new DataPublication(publishedReader, collectionReader.getReviewed(), details.datasetUri);
        publication.setDataLink(new DataLinkMock(pagesSet.getTimeSerieses()));

        // When
        // we process the publish
        publication.process(publishedReader, collectionReader.getReviewed(), collectionWriter.getReviewed(), zebedee.getDataIndex(), updateCommands);

        // Then
        // we expect datasetId to be extracted using the [datasetId].csdb pattern
        DataPublicationDetails reloaded = pagesSet.getDetails(publishedReader, collectionReader.getReviewed());
        assertEquals("abcd", reloaded.landingPage.getDescription().getDatasetId());
    }

    @Test
    public void publication_givenLandingPageWithoutDatasetId_generatesFromCSVFileName() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // we generate a publish with a fresh csv upload
        DataPagesSet pagesSet = generator.generateDataPagesSet("datasetIds", "temp", 2015, 2, "upload-abcd.csv");
        pagesSet.datasetLandingPage.getDescription().setDatasetId("");
        dataBuilder.addReviewedDataPagesSet(pagesSet, collection, collectionWriter);

        DataPublicationDetails details = pagesSet.getDetails(publishedReader, collectionReader.getReviewed());
        DataPublication publication = new DataPublication(publishedReader, collectionReader.getReviewed(), details.datasetUri);
        publication.setDataLink(new DataLinkMock(pagesSet.getTimeSerieses()));

        // When
        // we process the publish
        publication.process(publishedReader, collectionReader.getReviewed(), collectionWriter.getReviewed(), zebedee.getDataIndex(), updateCommands);

        // Then
        // we expect datasetId to be extracted using the upload.[datasetId].csv pattern
        DataPublicationDetails reloaded = pagesSet.getDetails(publishedReader, collectionReader.getReviewed());
        assertEquals("abcd", reloaded.landingPage.getDescription().getDatasetId());
    }

    @Test
    public void publication_givenCSDBFile_callsCSDBDataLink() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // we generate a publish with a csdb upload
        DataPagesSet pagesSet = generator.generateDataPagesSet("uploads", "temp", 2015, 2, "abcd.csdb");
        dataBuilder.addReviewedDataPagesSet(pagesSet, collection, collectionWriter);

        DataPublicationDetails details = pagesSet.getDetails(publishedReader, collectionReader.getReviewed());
        DataPublication publication = new DataPublication(publishedReader, collectionReader.getReviewed(), details.datasetUri);
        publication.setDataLink(new DataLinkMock(pagesSet.getTimeSerieses()));

        // When
        // we process the publish
        publication.process(publishedReader, collectionReader.getReviewed(), collectionWriter.getReviewed(), zebedee.getDataIndex(), updateCommands);

        // Then
        // we expect the csdb datalink to be called
        DataLinkMock mock = (DataLinkMock) publication.dataLink;
        assertEquals("csdb", mock.lastCall);
    }

    @Test
    public void publication_givenCSVFile_callsCSVDataLink() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // we generate a publish with a csv upload
        DataPagesSet pagesSet = generator.generateDataPagesSet("datasetIds", "temp", 2015, 2, "upload-abcd.csv");
        dataBuilder.addReviewedDataPagesSet(pagesSet, collection, collectionWriter);

        DataPublicationDetails details = pagesSet.getDetails(publishedReader, collectionReader.getReviewed());
        DataPublication publication = new DataPublication(publishedReader, collectionReader.getReviewed(), details.datasetUri);
        publication.setDataLink(new DataLinkMock(pagesSet.getTimeSerieses()));

        // When
        // we process the publish
        publication.process(publishedReader, collectionReader.getReviewed(), collectionWriter.getReviewed(), zebedee.getDataIndex(), updateCommands);

        // Then
        // we expect the csv datalink to be called
        DataLinkMock mock = (DataLinkMock) publication.dataLink;
        assertEquals("csv", mock.lastCall);
    }

    private Path generateIdiotCheck(ContentReader contentReader) throws IOException, ZebedeeException {
        Path temp = Files.createTempDirectory("temp");
        ContentIOUtils.copy(contentReader, new ContentWriter(temp));
        return temp;
    }
}