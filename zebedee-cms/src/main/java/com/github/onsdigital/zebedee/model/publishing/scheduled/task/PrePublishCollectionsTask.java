package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.publishing.Publisher;
import com.github.onsdigital.zebedee.model.publishing.preprocess.CollectionPublishPreprocessor;
import com.github.onsdigital.zebedee.model.publishing.scheduled.PublishScheduler;
import com.github.onsdigital.zebedee.util.Log;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.onsdigital.zebedee.logging.SimpleLogBuilder.logMessage;

/**
 * A scheduled task to run the pre-publish process for a number of collections.
 * <p>
 * Once this task is complete it will create a schedule the actual publish task.
 */
public class PrePublishCollectionsTask extends ScheduledTask {

    private final Set<String> collectionIds; // The list of collections ID's used in the task.
    private final Zebedee zebedee;
    private final Date publishDate; // the date of the actual publish, NOT the prepublish date associated with this task.
    private PublishScheduler publishScheduler;

    /**
     * Create a new instance of the PrePublishCollectionsTask.
     *
     * @param zebedee     The instance of Zebedee this task will run under.
     * @param publishDate The date the actual publish is scheduled for.
     */
    public PrePublishCollectionsTask(Zebedee zebedee, Date publishDate, PublishScheduler publishScheduler) {
        this.publishDate = publishDate;
        this.publishScheduler = publishScheduler;
        this.collectionIds = new HashSet<>();
        this.zebedee = zebedee;
    }

    /**
     * The run method is called at the time this task has been scheduled for.
     * <p>
     * Do everything possible before the publish so its ready to go with everything it needs.
     */
    @Override
    public void run() {

        long startTime = System.currentTimeMillis();
        Log.print("PRE-PUBLISH: Starting Pre-publish process.");

        // load collections into memory
        Set<Collection> collections = loadCollections();

        preProcessCollectionsForPublish(collections);

        // create a publish task for each collection ready to publish.
        List<PublishCollectionTask> collectionPublishTasks = createCollectionPublishTasks(collections);

        // create a post-publish task for each collection
        List<PostPublishCollectionTask> postPublishCollectionTasks = createCollectionPostPublishTasks(collectionPublishTasks, zebedee);

        Log.print("PRE-PUBLISH: Scheduling publish task for %s.", publishDate);
        publishScheduler.schedulePublish(collectionPublishTasks, postPublishCollectionTasks, publishDate);

        Log.print("PRE-PUBLISH: Finished Pre-publish process total time taken: %dms", (System.currentTimeMillis() - startTime));
    }

    private void preProcessCollectionsForPublish(Set<Collection> collections) {
        Log.print("PRE-PUBLISH: Preprocessing collections...");
        for (Collection collection : collections) {
            Log.print("PRE-PUBLISH: Preprocessing collection: " + collection.description.name);
            SecretKey key = zebedee.keyringCache.schedulerCache.get(collection.description.id);
            CollectionPublishPreprocessor.preProcessCollectionForPublish(collection, key);
            Log.print("PRE-PUBLISH: Preprocessing finished for collection: " + collection.description.name);
        }
    }

    /**
     * Load all of the collection objects for each collection to be published in this task.
     * <p>
     * Loading them into memory in the pre-publish step takes the overhead off of the publish process
     *
     * @return
     */
    private Set<Collection> loadCollections() {
        Set<Collection> collections = new HashSet<>();

        Log.print("PRE-PUBLISH: Loading collections into memory.");
        collectionIds.forEach(collectionId -> {

            Log.print("PRE-PUBLISH: Loading collection job for collection: " + collectionId);
            try {
                Collection collection = zebedee.collections.getCollection(collectionId);

                if (collection.description.approvedStatus == false) {
                    logMessage("Scheduled collection has not been approved - switching to manual");

                    // Switch to manual
                    collection.description.type = CollectionType.manual;
                    // TODO Alarm message
                    collection.save();

                } else {
                    collections.add(collection);
                }
            } catch (IOException e) {
                Log.print("Exception publishing collection for ID" + collectionId + " exception:" + e.getMessage());
            }
        });
        return collections;
    }

    /**
     * Prepare a task object for each collection to be published.
     * <p>
     * Doing this in the pre-publish step trims time off of the publish process and ensures everything that is
     * required is ready in memory as soon as the publish starts.
     *
     * @param collections
     * @return
     */
    private List<PublishCollectionTask> createCollectionPublishTasks(Set<Collection> collections) {
        List<PublishCollectionTask> collectionPublishTasks = new ArrayList<>(collections.size());
        List<Future<Boolean>> futures = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(collections.size()); // thread per collection

        // create a publish task for each collection that will publish the content to the website.
        // creating the individual collection publish tasks here to do all the work ahead of the actual publish.
        try {
            for (Collection collection : collections) {
                futures.add(pool.submit(() -> {
                    try {
                        Log.print("PRE-PUBLISH: creating collection publish task for collection: " + collection.description.name);
                        SecretKey key = zebedee.keyringCache.schedulerCache.get(collection.description.id);
                        ZebedeeCollectionReader collectionReader = new ZebedeeCollectionReader(collection, key);

                        String encryptionPassword = Random.password(100);

                        // begin the publish ahead of time. This creates the transaction on the train.
                        Map<String, String> hostToTransactionIdMap = Publisher.BeginPublish(collection, encryptionPassword);

                        // send versioned files manifest ahead of time. allowing files to be copied from the website into the transaction.
                        Publisher.SendManifest(collection, encryptionPassword);

                        PublishCollectionTask publishCollectionTask = new PublishCollectionTask(collection, collectionReader, encryptionPassword, hostToTransactionIdMap);

                        Log.print("PRE-PUBLISH: Adding publish task for collection %s", collection.description.name);
                        collectionPublishTasks.add(publishCollectionTask);
                        return true;
                    } catch (BadRequestException | IOException | UnauthorizedException | NotFoundException e) {
                        Log.print(e);
                        return false;
                    }
                }));

            }
        } finally {
            if (pool != null) pool.shutdown();
        }

        for (Future<Boolean> future : futures) {
            try {
                future.get().booleanValue();
            } catch (InterruptedException | ExecutionException e) {
                Log.print(e);
            }
        }

        return collectionPublishTasks;
    }

    /**
     * Prepare a post publish task for each collection ahead of the publish.
     *
     * @param collectionPublishTasks
     * @param zebedee
     * @return
     */
    private List<PostPublishCollectionTask> createCollectionPostPublishTasks(List<PublishCollectionTask> collectionPublishTasks, Zebedee zebedee) {
        List<PostPublishCollectionTask> postPublishCollectionTasks = new ArrayList<>(collectionPublishTasks.size());

        collectionPublishTasks.forEach(publishTask -> {
            Log.print("PRE-PUBLISH: creating collection post-publish task for collection: " + publishTask.getCollection().description.name);
            PostPublishCollectionTask postPublishCollectionTask = new PostPublishCollectionTask(zebedee, publishTask);
            postPublishCollectionTasks.add(postPublishCollectionTask);
        });

        return postPublishCollectionTasks;
    }

    /**
     * Add a collection to this task.
     *
     * @param collection
     */
    public void addCollection(Collection collection) {
        collectionIds.add(collection.description.id);
    }

    /**
     * Remove a collection from this task.
     *
     * @param collection
     */
    public void removeCollection(Collection collection) {
        collectionIds.remove(collection.description.id);
    }

}
