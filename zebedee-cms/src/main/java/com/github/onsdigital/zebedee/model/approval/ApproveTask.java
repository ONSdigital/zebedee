package com.github.onsdigital.zebedee.model.approval;

import com.github.onsdigital.zebedee.data.DataPublisher;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.util.Log;
import com.github.onsdigital.zebedee.util.ReleasePopulator;
import com.github.onsdigital.zebedee.util.SlackNotification;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Callable implementation for the approval process.
 */
public class ApproveTask implements Callable<Boolean> {

    private final Collection collection;
    private final Session session;
    private final CollectionReader collectionReader;
    private final CollectionWriter collectionWriter;
    private final ContentReader publishedReader;
    private final DataIndex dataIndex;

    public ApproveTask(
            Collection collection,
            Session session,
            CollectionReader collectionReader,
            CollectionWriter collectionWriter,
            ContentReader publishedReader,
            DataIndex dataIndex
    ) {
        this.collection = collection;
        this.session = session;
        this.collectionReader = collectionReader;
        this.collectionWriter = collectionWriter;
        this.publishedReader = publishedReader;
        this.dataIndex = dataIndex;
    }

    @Override
    public Boolean call() {

        try {
            // If the collection is associated with a release then populate the release page.
            ReleasePopulator.populateQuietly(collection, collectionReader, collectionWriter);

            // Generate timeseries if required.
            List<String> uriList = new DataPublisher().preprocessCollection(
                    publishedReader,
                    collectionReader,
                    collectionWriter.getReviewed(), collection, true, dataIndex);

            // set the approved state on the collection
            collection.description.approvedStatus = true;
            collection.description.AddEvent(new Event(new Date(), EventType.APPROVED, session.email));
            boolean result = collection.save();

            // Send a notification to the website with the publish date for caching.
            new PublishNotification(collection, uriList).sendNotification(EventType.APPROVED);

            return result;

        } catch (IOException | ZebedeeException | URISyntaxException e) {
            Log.print(e);
            SlackNotification.alarm(String.format("Exception approving collection %s : %s", collection.description.name, e.getMessage()));
            return false;
        }
    }
}
