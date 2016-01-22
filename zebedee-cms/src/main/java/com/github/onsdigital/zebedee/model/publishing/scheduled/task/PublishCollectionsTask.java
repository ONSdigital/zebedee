package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A scheduled task to run the publish process for multiple collections scheduled for the same time.
 * Once finished with the publish for each collection it will call the post publish process for each collection.
 */
public class PublishCollectionsTask extends ScheduledTask {

    private final ExecutorService executorService; // Thread pool to publish each collection concurrently.
    private List<PublishCollectionTask> publishCollectionTasks; // A task object for each collection to publish.
    private List<PostPublishCollectionTask> postPublishCollectionTasks;
    private Zebedee zebedee;

    /**
     * Create a new instance of the PublishCollectionsTask.
     * @param publishCollectionTasks A collection of tasks, one for each collection to publish.
     * @param zebedee
     */
    public PublishCollectionsTask(List<PublishCollectionTask> publishCollectionTasks,
                                  List<PostPublishCollectionTask> postPublishCollectionTasks,
                                  Zebedee zebedee) {
        this.publishCollectionTasks = publishCollectionTasks;
        this.postPublishCollectionTasks = postPublishCollectionTasks;
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
        Log.print("PUBLISH: Starting publish process.");
        long publishStart = System.currentTimeMillis();

        publishCollections();
        postPublishCollections();

        // all tasks should be completed now so cleanup the executorService.
        executorService.shutdown();

        Log.print("POST-PUBLISH: Publish complete total time taken: %dms", (System.currentTimeMillis() - publishStart));
    }

    /**
     * Run the publish task for each collection and wait for them all to complete before doing anything else.
     */
    protected void publishCollections() {
        Log.print("PUBLISH: Publishing %d collections.", publishCollectionTasks.size());
        // run concurrently if there is more than one collection to publish
        if (publishCollectionTasks.size() > 1) {

            // run all the publish tasks and wait for them to complete
            try {
                executorService.invokeAll(publishCollectionTasks, 120, TimeUnit.SECONDS); // Timeout after 2 minutes
            } catch (InterruptedException e) {
                Log.print(e);
            }
        } else {
            // Just run the publish in this thread if there is only one.
            publishCollectionTasks.forEach(task -> {
                try {
                    task.call();
                } catch (Exception e) {
                    Log.print(e);
                }
            });
        }
        Log.print("PUBLISH: Finished publishing collections.", publishCollectionTasks.size());
    }


    /**
     * Once the publish has finished for each collection, run the post-publish process for each collection.
     */
    protected void postPublishCollections() {
        Log.print("POST-PUBLISH: Running post publish process");
        // run concurrently if there is more than one task
        if (postPublishCollectionTasks.size() > 1) {

            // run all the publish tasks and wait for them to complete
            try {
                executorService.invokeAll(postPublishCollectionTasks, 120, TimeUnit.SECONDS); // Timeout after 2 minutes
            } catch (InterruptedException e) {
                Log.print(e);
            }
        } else {
            // Just run the task in this thread if there is only one.
            postPublishCollectionTasks.forEach(task -> {
                try {
                    task.call();
                } catch (Exception e) {
                    Log.print(e);
                }
            });
        }
        Log.print("POST-PUBLISH: Finished post publish process");
    }
}
