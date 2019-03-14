package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.publishing.PostPublisher;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;

import java.io.IOException;
import java.util.concurrent.Callable;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;

/**
 * The task that runs after a collection is published.
 */
public class PostPublishCollectionTask implements Callable<Boolean> {

    protected boolean done;
    private PublishCollectionTask publishCollectionTask; // The publish task that ran prior to this task.
    private Zebedee zebedee;

    public PostPublishCollectionTask(Zebedee zebedee, PublishCollectionTask publishCollectionTask) {
        this.zebedee = zebedee;
        this.publishCollectionTask = publishCollectionTask;
    }

    @Override
    public Boolean call() throws Exception {
        if (publishCollectionTask.isPublished()) {

            new PublishNotification(publishCollectionTask.getCollection()).sendNotification(EventType.PUBLISHED);
            return doPostPublish(publishCollectionTask.getCollection(), publishCollectionTask.getCollectionReader());
        }

        return false;
    }

    /**
     * Run the post publish process for a single collection.
     *
     * @param collection
     * @param collectionReader
     */
    protected boolean doPostPublish(Collection collection, ZebedeeCollectionReader collectionReader) {

        String collectionId = collection.getDescription().getId();

        info().data("collectionId", collectionId).log("POST-PUBLISH: Running collection post publish process");
        long onPublishCompleteStart = System.currentTimeMillis();
        boolean skipVerification = false;
        boolean result = false;

        try {
            result = PostPublisher.postPublish(zebedee, collection, skipVerification, collectionReader);
        } catch (IOException e) {
            error().data("collectionId", collectionId).logException(e, "Error while Running collection post publish process");
        }

        info().data("collectionId", collectionId).data("timeTaken", (System.currentTimeMillis() - onPublishCompleteStart))
                .log("POST-PUBLISH: collectiom postPublish process complete.");

        return result;
    }

    public PublishCollectionTask getPublishCollectionTask() {
        return publishCollectionTask;
    }

    public boolean isDone() {
        return done;
    }
}
