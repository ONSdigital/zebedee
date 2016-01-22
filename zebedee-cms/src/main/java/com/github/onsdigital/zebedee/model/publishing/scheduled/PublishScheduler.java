package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.PostPublishCollectionTask;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.PrePublishCollectionsTask;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.PublishCollectionTask;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.PublishCollectionsTask;
import com.github.onsdigital.zebedee.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Public interface to schedule publishes in Zebedee
 */
public class PublishScheduler {

    public static final int prePublishSecondsBeforePublish = 5; // how many seconds before a publish should the pre-publish process start

    private final Zebedee zebedee;
    private final Map<Date, PrePublishCollectionsTask> prePublishTasks = new HashMap<>();
    private final Map<Date, PublishCollectionsTask> publishTasks = new HashMap<>();

    public PublishScheduler(Zebedee zebedee) {
        this.zebedee = zebedee;
    }

    /**
     * Validate and schedule the given collection to be published based on the publish date set in the collection.
     * @param collection The collection to publish.
     * @param prePublishStartDate The start date of the pre-publish date.
     * @param publishStartDate The start date of the publish process.
     */
    public void schedulePrePublish(Collection collection, Date prePublishStartDate, Date publishStartDate) {
        if (Configuration.isSchedulingEnabled()) {
            try {
                System.out.println("Attempting to schedule pre-publish for collection " + collection.description.name + " type=" + collection.description.type);
                if (collection.description.type == CollectionType.scheduled) {
                    schedulePrePublishCollection(collection, prePublishStartDate, publishStartDate);
                }
            } catch (Exception e) {
                System.out.println("Exception caught trying to schedule existing collection: " + e.getMessage());
            }
        } else {
            Log.print("Not scheduling collection %s, scheduling is not enabled", collection.description.name);
        }
    }


    public void schedulePublish(
            List<PublishCollectionTask> collectionPublishTasks,
            List<PostPublishCollectionTask> postPublishCollectionTasks,
            Date publishDate
    ) {
        if (Configuration.isSchedulingEnabled()) {
            try {
                // create and schedule publish task if all is well.
                // pass loaded collections into publish task so that everything is ready ahead of time.
                PublishCollectionsTask publishTask = new PublishCollectionsTask(collectionPublishTasks, postPublishCollectionTasks);
                publishTask.schedule(publishDate);
            } catch (Exception e) {
                System.out.println("Exception caught trying to schedule: " + e.getMessage());
            }
        } else {
            Log.print("Not scheduling publish, scheduling is not enabled");
        }
    }

    private void schedulePrePublishCollection(Collection collection, Date prePublishStartDate, Date publishStartDate) {
        // cancel existing publish for the collection
        prePublishTasks.values().forEach(task -> task.removeCollection(collection));
        PrePublishCollectionsTask task;

        // add a task for the publish date if there is not already one.
        if (prePublishTasks.containsKey(publishStartDate)) {
            task = prePublishTasks.get(publishStartDate);
        } else {
            task = new PrePublishCollectionsTask(zebedee, publishStartDate, this);
            task.schedule(prePublishStartDate);
            prePublishTasks.put(publishStartDate, task);
        }

        task.addCollection(collection);
    }

    /**
     * Cancel a publish for the given collection.
     *
     * @param collection
     */
    public void cancelPublish(Collection collection) {
        prePublishTasks.values().forEach(task -> task.removeCollection(collection));
    }

    /**
     * Remove the publish task once complete.
     *
     * @param date
     * @return
     */
    public boolean removeCompletedPublish(Date date) {
        // Verify its done
        PublishCollectionsTask task = publishTasks.get(date);
        if (!task.isComplete()) {
            return false;
        }

        publishTasks.remove(date);
        return true;
    }

    /**
     * Remove the publish task once complete.
     *
     * @param date
     * @return
     */
    public boolean removeCompletedPrePublish(Date date) {
        // Verify its done
        PrePublishCollectionsTask task = prePublishTasks.get(date);
        if (!task.isComplete()) {
            return false;
        }

        prePublishTasks.remove(date);
        return true;
    }
}
