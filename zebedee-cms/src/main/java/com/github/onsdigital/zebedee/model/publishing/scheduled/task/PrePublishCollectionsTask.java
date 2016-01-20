package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.util.Log;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.*;

/**
 * A scheduled task to run the pre-publish process for a number of collections.
 * <p>
 * Once this task is complete it will create a schedule the actual publish task.
 */
public class PrePublishCollectionsTask extends ScheduledTask {

    private final Set<String> collectionIds; // The list of collections ID's used in the task.
    private final Zebedee zebedee;
    private final Date publishDate; // the date of the actual publish, NOT the prepublish date associated with this task.

    /**
     * Create a new instance of the PrePublishCollectionsTask.
     *
     * @param zebedee     The instance of Zebedee this task will run under.
     * @param publishDate The date the actual publish is scheduled for.
     */
    public PrePublishCollectionsTask(Zebedee zebedee, Date publishDate) {
        this.publishDate = publishDate;
        this.collectionIds = new HashSet<>();
        this.zebedee = zebedee;
    }

    /**
     * The run method is called at the time this task has been scheduled for.
     *
     * Do everything possible before the publish so its ready to go with everything it needs.
     */
    @Override
    public void run() {

        Log.print("Starting Pre-publish process...");

        // load collections into memory
        Set<Collection> collections = loadCollections();

        // create a publish task for each collection ready to publish.
        List<PublishCollectionTask> collectionPublishTasks = createCollectionPublishTasks(collections);

        // create and schedule publish task if all is well.
        // pass loaded collections into publish task so that everything is ready ahead of time.
        PublishCollectionsTask publishTask = new PublishCollectionsTask(collectionPublishTasks, zebedee);
        publishTask.schedule(publishDate);
    }

    /**
     * Load all of the collection objects for each collection to be published in this task.
     *
     * Loading them into memory in the pre-publish step takes the overhead off of the publish process
     * @return
     */
    private Set<Collection> loadCollections() {
        Set<Collection> collections = new HashSet<>();

        Log.print("Loading collections into memory.");
        collectionIds.forEach(collectionId -> {

            Log.print("Running scheduled job for collection id: " + collectionId);
            try {
                Collection collection = zebedee.collections.getCollection(collectionId);

                if (collection.description.approvedStatus == false) {
                    System.out.println("Scheduled collection has not been approved - switching to manual");

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
     *
     * Doing this in the pre-publish step trims time off of the publish process and ensures everything that is
     * required is ready in memory as soon as the publish starts.
     * @param collections
     * @return
     */
    private List<PublishCollectionTask> createCollectionPublishTasks(Set<Collection> collections) {
        List<PublishCollectionTask> collectionPublishTasks = new ArrayList<>(collections.size());

        // create a publish task for each collection that will publish the content to the website.
        // creating the individual collection publish tasks here to do all the work ahead of the actual publish.
        collections.forEach(collection -> {
            try {
                SecretKey key = zebedee.keyringCache.schedulerCache.get(collection.description.id);
                ZebedeeCollectionReader collectionReader = new ZebedeeCollectionReader(collection, key);
                PublishCollectionTask publishCollectionTask = new PublishCollectionTask(collection, collectionReader);
                Log.print("Adding publish task for collection %s", collection.description.name);
                collectionPublishTasks.add(publishCollectionTask);
            } catch (BadRequestException | IOException | UnauthorizedException | NotFoundException e) {
                Log.print(e);
            }

            // start transaction for each collection?
            // send versioned files / all files over to the train?
        });
        return collectionPublishTasks;
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
