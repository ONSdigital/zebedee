package com.github.onsdigital.zebedee.data.framework;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ContentWriter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Date;


/**
 * Created by thomasridd on 1/18/16.
 */
public class DataBuilder {
    Zebedee zebedee;
    Session publisher;
    Session reviewer;

    public DataBuilder(Zebedee zebedee, Session publisher, Session reviewer) {
        this.zebedee = zebedee;
        this.publisher = publisher;
        this.reviewer = reviewer;
    }

    /**
     * @param dataPagesSet a generated set of pages for the data publisher
     * @throws URISyntaxException
     * @throws IOException
     * @throws BadRequestException
     * @throws ParseException
     */
    public void publishDataPagesSet(DataPagesSet dataPagesSet) throws URISyntaxException, IOException, BadRequestException, ParseException {

        publishPage(dataPagesSet.datasetLandingPage, dataPagesSet.datasetLandingPage.getUri().toString());
        publishPage(dataPagesSet.timeSeriesDataset, dataPagesSet.timeSeriesDataset.getUri().toString());

        // timeseries in the old format
        for (TimeSeries timeSeries : dataPagesSet.timeSeriesList) {
            publishPage(timeSeries, timeSeries.getUri().toString());
        }

        // We have added pages directly to the master so need to reindex
        zebedee.getDataIndex().reindex();
        zebedee.getDataIndex().pauseUntilComplete(60);
    }

    /**
     * Publish a page object
     *
     * @param page any zebedee page
     * @param uri  the uri to publish to
     * @throws IOException
     * @throws BadRequestException
     */
    public void publishPage(Page page, String uri) throws IOException, BadRequestException {
        String publishTo = uri;
        if (publishTo.startsWith("/"))
            publishTo = publishTo.substring(1);
        ContentWriter writer = new ContentWriter(zebedee.getPublished().path);

        writer.writeObject(page, publishTo + "/data.json");
    }

    public Collection createCollection(String name) throws IOException, ZebedeeException {
        // Create the description:
        CollectionDescription description = new CollectionDescription();
        description.setName(name);

        return Collection.create(description, zebedee, publisher);
    }


    /**
     * Add a dataset upload to a collection plus dummy upload file
     *
     * @param dataPagesSet     a dataPagesSet to upload
     * @param collection       a collection to load into
     * @param collectionWriter a collection writer with permissions for the collection
     * @throws IOException
     * @throws BadRequestException
     */
    public void addReviewedDataPagesSet(DataPagesSet dataPagesSet, Collection collection, CollectionWriter collectionWriter) throws IOException, BadRequestException {
        if (dataPagesSet.datasetLandingPage != null)
            addReviewedPage(dataPagesSet.datasetLandingPage.getUri().toString(), dataPagesSet.datasetLandingPage, collection, collectionWriter);
        if (dataPagesSet.timeSeriesDataset != null)
            addReviewedPage(dataPagesSet.timeSeriesDataset.getUri().toString(), dataPagesSet.timeSeriesDataset, collection, collectionWriter);
        if (dataPagesSet.fileUri != null)
            addReviewedFile(dataPagesSet.fileUri, collection.find(dataPagesSet.timeSeriesDataset.getUri().toString()).resolve("data.json"), collection, collectionWriter);
    }

    public void addReviewedPage(String uri, Page page, Collection collection, CollectionWriter collectionWriter) throws IOException, BadRequestException {

        collectionWriter.getReviewed().writeObject(page, uri + "/data.json");
        addReviewEventsToCollectionJson(uri, collection);
    }

    public void addInProgressPage(String uri, Page page, Collection collection, CollectionWriter collectionWriter) throws IOException, BadRequestException {

        collectionWriter.getInProgress().writeObject(page, uri + "/data.json");
        addReviewEventsToCollectionJson(uri, collection);
    }

    public void addCompletedPage(String uri, Page page, Collection collection, CollectionWriter collectionWriter) throws IOException, BadRequestException {

        collectionWriter.getComplete().writeObject(page, uri + "/data.json");
        addReviewEventsToCollectionJson(uri, collection);
    }

    public void addReviewedFile(String uri, Path path, Collection collection, CollectionWriter collectionWriter) throws IOException, BadRequestException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            collectionWriter.getReviewed().write(inputStream, uri);
            addReviewEventsToCollectionJson(uri, collection);
        }
    }

    private void addReviewEventsToCollectionJson(String uri, Collection collection) throws IOException {
        collection.addEvent(uri, new Event(new Date(), EventType.CREATED, this.publisher.getEmail()));
        collection.addEvent(uri, new Event(new Date(), EventType.COMPLETED, this.publisher.getEmail()));
        collection.addEvent(uri, new Event(new Date(), EventType.REVIEWED, this.reviewer.getEmail()));
        collection.reviewedUris().add(uri);
        collection.save();
    }

}
