package com.github.onsdigital.zebedee.data.processing;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;
import com.github.onsdigital.zebedee.data.framework.DataBuilder;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.*;

public class DataWriterTest extends ZebedeeTestBaseFixture {

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
     * Setup generates an instance of zebedee plus a collection
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
        unpublished = generator.generateDataPagesSet("dataprocessor", "inreview", 2015, 2, "");
        dataBuilder.addReviewedDataPagesSet(unpublished, collection, collectionWriter);

        // add a set of data to published
        published = generator.generateDataPagesSet("dataprocessor", "published", 2015, 2, "");
        dataBuilder.publishDataPagesSet(published);

        // add a set of data to published over the existing set
        republish = generator.generateDataPagesSet("dataprocessor", "published", 2016, 2, "");
        dataBuilder.addReviewedDataPagesSet(republish, collection, collectionWriter);
    }

    @Test
    public void versionAndSave_forBrandNewData_doesCreateFileInReviewed() throws ZebedeeException, IOException, URISyntaxException {
        // Given
        // a processor for a brand new timeseries
        DataPublicationDetails details = unpublished.getDetails(publishedReader, collectionReader.getReviewed());
        TimeSeries timeSeries = unpublished.timeSeriesList.get(0);

        DataProcessor processor = new DataProcessor();
        processor.processTimeseries(publishedReader, details, timeSeries, zebedee.getDataIndex());

        // When
        // we version and save
        DataWriter dataWriter = new DataWriter(collectionWriter.getReviewed(), collectionReader.getReviewed(), publishedReader);
        dataWriter.versionAndSave(processor, details);

        // Then
        // the new file should save
        assertTrue(fileExists(collectionReader.getReviewed(), timeSeries.getUri().toString()));
    }

    boolean fileExists(ContentReader reader, String path) throws ZebedeeException, IOException {
        try {
            reader.getContent(path);
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Test
    public void versionAndSave_forAlreadyPublishedData_shouldCreateFileInReviewed() throws ZebedeeException,
            IOException, URISyntaxException {
        // Given
        // we republish a published timeseries with identical values
        TimeSeries timeSeries = published.timeSeriesList.get(0);
        DataPublicationDetails details = published.getDetails(publishedReader, collectionReader.getReviewed());

        DataProcessor processor = new DataProcessor();
        processor.processTimeseries(publishedReader, details, timeSeries, zebedee.getDataIndex());

        // When
        // we version and save
        DataWriter dataWriter = new DataWriter(collectionWriter.getReviewed(), collectionReader.getReviewed(), publishedReader);
        dataWriter.versionAndSave(processor, details);

        // Then
        // we expect an addition to the collection -  as the date will have been modified.
        assertTrue(timeSeries.getUri().toString(), fileExists(collectionReader.getReviewed(), timeSeries.getUri()
                .toString()));
    }

    @Test
    public void versionAndSave_forUpdatedData_doesCreateFileInReviewed() throws ZebedeeException, IOException, URISyntaxException {
        // Given
        // a processor for updates to an old timeseries
        DataPublicationDetails details = republish.getDetails(publishedReader, collectionReader.getReviewed());
        TimeSeries timeSeries = republish.timeSeriesList.get(0);

        DataProcessor processor = new DataProcessor();
        processor.processTimeseries(publishedReader, details, timeSeries, zebedee.getDataIndex());

        // When
        // we version and save
        DataWriter dataWriter = new DataWriter(collectionWriter.getReviewed(), collectionReader.getReviewed(), publishedReader);
        dataWriter.versionAndSave(processor, details);

        // Then
        // the new file should save
        assertTrue(fileExists(collectionReader.getReviewed(), timeSeries.getUri().toString()));
    }

    @Test
    public void versionTimeseries_forNewTimeSeries_doesntCreateVersion() throws ZebedeeException, IOException, URISyntaxException {
        // Given
        // a previously unpublished timeseries
        TimeSeries timeSeries = unpublished.timeSeriesList.get(0);
        DataPublicationDetails details = unpublished.getDetails(publishedReader, collectionReader.getReviewed());

        DataProcessor processor = new DataProcessor();
        processor.processTimeseries(publishedReader, details, timeSeries, zebedee.getDataIndex());

        // When
        // we version and save
        DataWriter dataWriter = new DataWriter(collectionWriter.getReviewed(), collectionReader.getReviewed(), publishedReader);
        dataWriter.versionAndSave(processor, details);

        // Then
        // we expect no version path to have been created
        String versionPath = VersionedContentItem.getVersionUri(timeSeries.getUri().toString(), 1);
        assertFalse(fileExists(collectionReader.getReviewed(), versionPath));
    }

    @Test
    public void versionTimeseries_forPublishedTimeSeries_doesntCreateVersionFiles() throws ZebedeeException, IOException, URISyntaxException {
        // Given
        // a previously published timeseries
        TimeSeries timeSeries = published.timeSeriesList.get(0);
        DataPublicationDetails details = published.getDetails(publishedReader, collectionReader.getReviewed());

        DataProcessor processor = new DataProcessor();
        processor.processTimeseries(publishedReader, details, timeSeries, zebedee.getDataIndex());

        // When
        // we version and save
        DataWriter dataWriter = new DataWriter(collectionWriter.getReviewed(), collectionReader.getReviewed(), publishedReader);
        dataWriter.versionAndSave(processor, details);

        // Then
        // we expect no version path to have been created
        String versionPath = VersionedContentItem.getVersionUri(timeSeries.getUri().toString(), 1);
        assertFalse(fileExists(collectionReader.getReviewed(), versionPath));
    }

    @Test
    public void versionTimeseries_forUpdatedTimeSeries_doesCreateVersionFiles() throws ZebedeeException, IOException, URISyntaxException {
        // Given
        // a previously published timeseries
        TimeSeries timeSeries = republish.timeSeriesList.get(0);
        DataPublicationDetails details = republish.getDetails(publishedReader, collectionReader.getReviewed());

        DataProcessor processor = new DataProcessor();
        processor.processTimeseries(publishedReader, details, timeSeries, zebedee.getDataIndex());

        // When
        // we version and save
        DataWriter dataWriter = new DataWriter(collectionWriter.getReviewed(), collectionReader.getReviewed(), publishedReader);
        dataWriter.versionAndSave(processor, details);

        // Then
        // we expect no version path to have been created
        String versionPath = VersionedContentItem.getVersionUri(timeSeries.getUri().toString(), 1);
        assertTrue(fileExists(collectionReader.getReviewed(), versionPath));
    }

    @Test
    public void versionTimeseries_forUpdatedTimeSeries_doesCreateVersionData() throws ZebedeeException, IOException, URISyntaxException, ParseException {
        // Given
        // an updated timeseries with known version information in the dataset
        DataPagesSet original = generator.generateDataPagesSet("dataprocessor", "corrections", 2015, 2, "");
        dataBuilder.publishDataPagesSet(original);

        DataPagesSet corrected = generator.generateDataPagesSet("dataprocessor", "corrections", 2016, 2, "");
        Version datasetVersion = new Version();
        datasetVersion.setCorrectionNotice(Random.id());
        corrected.timeSeriesDataset.setVersions(new ArrayList<>());
        corrected.timeSeriesDataset.getVersions().add(datasetVersion);
        dataBuilder.addReviewedDataPagesSet(corrected, this.collection, this.collectionWriter);

        TimeSeries timeSeries = corrected.timeSeriesList.get(0);
        DataPublicationDetails details = corrected.getDetails(publishedReader, collectionReader.getReviewed());

        DataProcessor processor = new DataProcessor();
        processor.processTimeseries(publishedReader, details, timeSeries, zebedee.getDataIndex());

        // When
        // we version and save
        DataWriter dataWriter = new DataWriter(collectionWriter.getReviewed(), collectionReader.getReviewed(), publishedReader);
        dataWriter.versionAndSave(processor, details);

        // Then
        // we expect version information to have been created with the collection information
        TimeSeries versionedSeries = (TimeSeries) collectionReader.getReviewed().getContent(timeSeries.getUri().toString());

        assertNotNull(versionedSeries.getVersions());
        assertEquals(1, versionedSeries.getVersions().size());

        Version version = versionedSeries.getVersions().get(0);
        assertEquals(datasetVersion.getCorrectionNotice(), version.getCorrectionNotice());
    }
}