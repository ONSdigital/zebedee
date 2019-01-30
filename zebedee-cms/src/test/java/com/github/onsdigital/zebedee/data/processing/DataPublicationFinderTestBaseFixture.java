package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import com.github.onsdigital.zebedee.data.framework.DataBuilder;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by thomasridd on 1/19/16.
 */
public class DataPublicationFinderTestBaseFixture extends ZebedeeTestBaseFixture {

    Session publisher;
    Session reviewer;

    Collection collection;
    ContentReader publishedReader;
    CollectionReader collectionReader;
    CollectionWriter collectionWriter;
    DataBuilder dataBuilder;
    DataPagesGenerator generator;

    public void setUp() throws Exception {
        publisher = zebedee.openSession(builder.publisher1Credentials);
        reviewer = zebedee.openSession(builder.reviewer1Credentials);

        dataBuilder = new DataBuilder(zebedee, publisher, reviewer);
        generator = new DataPagesGenerator();

        CollectionDescription collectionDescription = new CollectionDescription();
        collectionDescription.setName("DataPublicationFinder");
        collectionDescription.setType(CollectionType.scheduled);
        collectionDescription.setPublishDate(new Date());
        collection = Collection.create(collectionDescription, zebedee, publisher);

        publishedReader = new FileSystemContentReader(zebedee.getPublished().path);
        collectionReader = new ZebedeeCollectionReader(zebedee, collection, publisher);
        collectionWriter = new ZebedeeCollectionWriter(zebedee, collection, publisher);
    }

    @Test
    public void findPublications_givenCollectionWithNoData_returnsEmptyList() throws IOException, ZebedeeException {
        // Given
        // One of the default collections built by Builder
        Collection collection = zebedee.getCollections().list().get(0);

        // When
        // we search for publications
        List<DataPublication> publications = new DataPublicationFinder().findPublications(publishedReader, collectionReader.getReviewed());

        // Then
        // no data turns up
        assertEquals(0, publications.size());
    }

    @Test
    public void findPublications_givenCollectionWithData_returnsPublication() throws IOException, ZebedeeException, ParseException, URISyntaxException {
        // Given
        // One of the default collections built by Builder that we add data to
        DataPagesSet dataPagesSet = generator.generateDataPagesSet("node", "test", 2015, 2, "data.csdb");
        dataBuilder.addReviewedDataPagesSet(dataPagesSet, collection, collectionWriter);

        // When
        // we search for publications
        List<DataPublication> publications = new DataPublicationFinder().findPublications(publishedReader, collectionReader.getReviewed());

        // Then
        // the publication is identified
        assertEquals(1, publications.size());
        assertEquals(dataPagesSet.timeSeriesDataset.getUri().toString(), publications.get(0).getDetails().datasetUri);

    }

    @Test
    public void findPublications_givenCollectionWithTwoDataPublications_returnsBoth() throws IOException, ZebedeeException, ParseException, URISyntaxException {
        // Given
        // One of the default collections built by Builder that we add data to
        DataPagesSet dataPagesSet = generator.generateDataPagesSet("node", "test", 2015, 2, "data.csdb");
        dataBuilder.addReviewedDataPagesSet(dataPagesSet, collection, collectionWriter);

        DataPagesSet dataPagesSet2 = generator.generateDataPagesSet("node2", "test", 2015, 2, "data.csdb");
        dataBuilder.addReviewedDataPagesSet(dataPagesSet2, collection, collectionWriter);

        // When
        // we search for publications
        List<DataPublication> publications = new DataPublicationFinder().findPublications(publishedReader, collectionReader.getReviewed());

        // Then
        // the publication is identified
        assertEquals(2, publications.size());

    }
}