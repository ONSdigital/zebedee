package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.onsdigital.zebedee.model.Collection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

/**
 * A scheduled task to run the publish process for multiple collections scheduled for the same time.
 * Once finished with the publish for each collection it will call the post publish process for each collection.
 */
public class PublishCollectionsTask extends ScheduledTask {

    private final ExecutorService executorService; // Thread pool to publish each collection concurrently.
    private List<PublishCollectionTask> publishCollectionTasks; // A task object for each collection to publish.
    private List<PostPublishCollectionTask> postPublishCollectionTasks;

    /**
     * Create a new instance of the PublishCollectionsTask.
     *
     * @param publishCollectionTasks A collection of tasks, one for each collection to publish.
     */
    public PublishCollectionsTask(List<PublishCollectionTask> publishCollectionTasks,
                                  List<PostPublishCollectionTask> postPublishCollectionTasks
    ) {
        this.publishCollectionTasks = publishCollectionTasks;
        this.postPublishCollectionTasks = postPublishCollectionTasks;
        this.executorService = Executors.newFixedThreadPool(publishCollectionTasks.size());
    }

    /**
     * The run method is called at the time this task has been scheduled for.
     * <p>
     * Publish each collection, and run the post publish for each collection only when all collections are published.
     */
    @Override
    public void run() {
        info().log("PUBLISH: Starting publish process.");
        long publishStart = System.currentTimeMillis();

        publishCollections();
        postPublishCollections();

        // all tasks should be completed now so cleanup the executorService.
        executorService.shutdown();

        info().data("timeTaken", (System.currentTimeMillis() - publishStart)).log("POST-PUBLISH: Publish complete");
    }

    /**
     * Run the publish task for each collection and wait for them all to complete before doing anything else.
     */
    protected void publishCollections() {
        info().data("collectionsSize", publishCollectionTasks.size()).log("PUBLISH: publishing collections");
        long start = System.currentTimeMillis();

        // run concurrently if there is more than one collection to publish
        if (publishCollectionTasks.size() > 1) {

            // run all the publish tasks and wait for them to complete
            try {
                executorService.invokeAll(publishCollectionTasks, 15, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                error().logException(e, "Error while publishing collections");
            }
        } else {
            // Just run the publish in this thread if there is only one.
            publishCollectionTasks.forEach(task -> {
                try {
                    task.call();
                } catch (Exception e) {
                    error().logException(e, "Error while publishing collections");
                }
            });
        }

        info().data("timeTaken", (System.currentTimeMillis() - start))
                .data("collectionsPublished", publishCollectionTasks.size())
                .log("PUBLISH: Finished publishing collections");
    }


    /**
     * Once the publish has finished for each collection, run the post-publish process for each collection.
     */
    protected void postPublishCollections() {
        info().log("POST-PUBLISH: Running post publish process");
        // run concurrently if there is more than one task
        if (postPublishCollectionTasks.size() > 1) {

            // run all the publish tasks and wait for them to complete
            try {
                executorService.invokeAll(postPublishCollectionTasks, 15, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                error().logException(e, "Error while post publishing collections");
            }
        } else {
            // Just run the task in this thread if there is only one.
            postPublishCollectionTasks.forEach(task -> {
                try {
                    task.call();
                } catch (Exception e) {
                    error().logException(e, "Error while post publishing collections");
                }
            });
        }
        info().log("POST-PUBLISH: Finished post publish process");
    }

    /**
     * Remove any tasks associated with the given collection.
     *
     * @param collection
     */
    public void removeCollection(Collection collection) {

        List<PublishCollectionTask> publishTasksToRemove = new ArrayList<>();
        // remove the publish tasks for that collection
        for (PublishCollectionTask task : publishCollectionTasks) {
            if (task.getCollection().equals(collection))
                publishTasksToRemove.add(task);
        }
        for (PublishCollectionTask task : publishTasksToRemove) {
            publishCollectionTasks.remove(task);
        }

        List<PostPublishCollectionTask> postPublishTasksToRemove = new ArrayList<>();
        // remove the post-publish tasks for that collection
        for (PostPublishCollectionTask task : postPublishCollectionTasks) {
            if (task.getPublishCollectionTask().getCollection().equals(collection))
                postPublishTasksToRemove.add(task);
        }
        for (PostPublishCollectionTask task : postPublishTasksToRemove) {
            postPublishCollectionTasks.remove(task);
        }
    }
}
