package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.mertics.service.MetricsService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * A scheduled task to run the publish process for multiple collections scheduled for the same time.
 * Once finished with the publish for each collection it will call the post publish process for each collection.
 */
public class PublishCollectionsTask extends ScheduledTask {

    private static MetricsService metricsService = MetricsService.getInstance();

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
        logInfo("PUBLISH: Starting publish process.").log();
        long publishStart = System.currentTimeMillis();

        publishCollections();
        postPublishCollections();

        // all tasks should be completed now so cleanup the executorService.
        executorService.shutdown();

        logInfo("POST-PUBLISH: Publish complete").timeTaken((System.currentTimeMillis() - publishStart)).log();
    }

    /**
     * Run the publish task for each collection and wait for them all to complete before doing anything else.
     */
    protected void publishCollections() {
        logInfo("PUBLISH: publishing collections").addParameter("collectionsSize", publishCollectionTasks.size()).log();
        long start = System.currentTimeMillis();

        // run concurrently if there is more than one collection to publish
        if (publishCollectionTasks.size() > 1) {

            // run all the publish tasks and wait for them to complete
            try {
                executorService.invokeAll(publishCollectionTasks, 15, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                logError(e, "Error while publishing collections").log();
            }
        } else {
            // Just run the publish in this thread if there is only one.
            publishCollectionTasks.forEach(task -> {
                try {
                    task.call();
                } catch (Exception e) {
                    logError(e, "Error while publishing collections").log();
                }
            });
        }

        logInfo("PUBLISH: Finished publishing collections").timeTaken((System.currentTimeMillis() - start))
                .addParameter("collectionsPublished", publishCollectionTasks.size()).log();
    }


    /**
     * Once the publish has finished for each collection, run the post-publish process for each collection.
     */
    protected void postPublishCollections() {
        logInfo("POST-PUBLISH: Running post publish process").log();
        // run concurrently if there is more than one task
        if (postPublishCollectionTasks.size() > 1) {

            // run all the publish tasks and wait for them to complete
            try {
                executorService.invokeAll(postPublishCollectionTasks, 15, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                logError(e, "Error while post publishing collections").log();
            }
        } else {
            // Just run the task in this thread if there is only one.
            postPublishCollectionTasks.forEach(task -> {
                try {
                    task.call();
                } catch (Exception e) {
                    logError(e, "Error while post publishing collections").log();
                }
            });
        }
        logInfo("POST-PUBLISH: Finished post publish process").log();
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
