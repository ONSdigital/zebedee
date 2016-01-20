package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.publishing.Publisher;
import com.github.onsdigital.zebedee.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A scheduled task to run the publish process for multiple collections scheduled for the same time.
 */
public class PublishCollectionsTask extends ScheduledTask {

    private List<PublishCollectionTask> publishCollectionTasks; // A task object for each collection to publish.
    private Zebedee zebedee;
    private final ExecutorService executorService; // Thread pool to publish each collection concurrently.

    /**
     * Create a new instance of the PublishCollectionsTask.
     * @param publishCollectionTasks A collection of tasks, one for each collection to publish.
     * @param zebedee
     */
    public PublishCollectionsTask(List<PublishCollectionTask> publishCollectionTasks, Zebedee zebedee) {
        this.publishCollectionTasks = publishCollectionTasks;
        this.zebedee = zebedee;
        this.executorService = Executors.newFixedThreadPool(publishCollectionTasks.size());
    }

    /**
     * The run method is called at the time this task has been scheduled for.
     *
     * Publish each collection, and run the post publish for each collection only when all collections are published.
     */
    @Override
    public void run() {
        Log.print("Starting publish process.");
        long publishStart = System.currentTimeMillis();

        publishCollections();
        postPublishCollections();

        // all tasks should be completed now so cleanup the executorService.
        executorService.shutdown();

        Log.print("Publish complete total time taken: %dms", (System.currentTimeMillis() - publishStart));
    }

    /**
     * Run the publish task for each collection and wait for them all to complete before doing anything else.
     */
    private void publishCollections() {
        // run all the publish tasks and wait for them to complete
        try {
            executorService.invokeAll(publishCollectionTasks, 120, TimeUnit.SECONDS); // Timeout after 2 minutes
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Once the publish has finished for each collection, run the post-publish process for each collection.
     */
    private void postPublishCollections() {
        publishCollectionTasks.forEach(task -> {
            if (task.isPublished()) {
                doPostPublish(task.getCollection(), task.getCollectionReader());
            }
        });
    }

    /**
     * Run the post publish process for a single collection.
     * @param collection
     * @param collectionReader
     */
    private void doPostPublish(Collection collection, ZebedeeCollectionReader collectionReader) {

        long onPublishCompleteStart = System.currentTimeMillis();
        boolean skipVerification = false;

        try {
            Publisher.postPublish(zebedee, collection, skipVerification, collectionReader);
        } catch (IOException e) {
            Log.print(e);
        }

        Log.print("postPublish process finished for collection %s time taken: %dms",
                collection.description.name,
                (System.currentTimeMillis() - onPublishCompleteStart));
    }
}
