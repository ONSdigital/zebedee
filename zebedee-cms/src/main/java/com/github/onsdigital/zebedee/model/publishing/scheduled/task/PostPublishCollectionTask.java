package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.publishing.PostPublisher;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;

import java.io.IOException;
import java.util.concurrent.Callable;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

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

        logInfo("POST-PUBLISH: Running collection post publish process").collectionName(collection).log();
        long onPublishCompleteStart = System.currentTimeMillis();
        boolean skipVerification = false;
        boolean result = false;

        try {
            result = PostPublisher.postPublish(zebedee, collection, skipVerification, collectionReader);
        } catch (IOException e) {
            logError(e, "Error while Running collection post publish process").collectionName(collection).log();
        }

        logInfo("POST-PUBLISH: collectiom postPublish process complete.")
                .collectionName(collection).timeTaken((System.currentTimeMillis() - onPublishCompleteStart)).log();

        return result;
    }

    public PublishCollectionTask getPublishCollectionTask() {
        return publishCollectionTask;
    }

    public boolean isDone() {
        return done;
    }
}
