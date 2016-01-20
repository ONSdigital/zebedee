package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.PrePublishCollectionsTask;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.PublishCollectionsTask;
import com.github.onsdigital.zebedee.util.Log;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Public interface to schedule publishes in Zebedee
 */
public class PublishScheduler {

    private static final int prePublishSecondsBeforePublish = 5; // how many seconds before a publish should the pre-publish process start

    private final Zebedee zebedee;
    private final Map<Date, PrePublishCollectionsTask> prePublishTasks = new HashMap<>();
    private final Map<Date, PublishCollectionsTask> publishTasks = new HashMap<>();

    public PublishScheduler(Zebedee zebedee) {
        this.zebedee = zebedee;
    }

    /**
     * Validate and schedule the given collection to be published based on the publish date set in the collection.
     * @param collection
     */
    public void schedulePublish(Collection collection) {
        if (Configuration.isSchedulingEnabled()) {
            try {
                System.out.println("Attempting to schedule publish for collection " + collection.description.name + " type=" + collection.description.type);
                if (collection.description.type == CollectionType.scheduled) {
                    if (collection.description.publishDate != null) {
                        scheduleCollection(collection);
                    } else {
                        Log.print("Could not schedule publish for collection %s, the publish date is null", collection.description.name);
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception caught trying to schedule existing collection: " + e.getMessage());
            }
        } else {
            Log.print("Not scheduling collection %s, scheduling is not enabled", collection.description.name);
        }
    }

    private void scheduleCollection(Collection collection) {
        // cancel existing publish for the collection
        prePublishTasks.values().forEach(task -> task.removeCollection(collection));
        Date publishDate = collection.description.publishDate;
        PrePublishCollectionsTask task;

        // add a task for the publish date if there is not already one.
        if (prePublishTasks.containsKey(publishDate)) {
            task = prePublishTasks.get(publishDate);
        } else {
            task = new PrePublishCollectionsTask(zebedee, publishDate);

            // take the defined number of seconds off the publish time to calculate when to start pre-publish.
            Date prePublishDate = new DateTime(publishDate).minusSeconds(prePublishSecondsBeforePublish).toDate();
            task.schedule(prePublishDate);
            prePublishTasks.put(publishDate, task);
        }

        task.addCollection(collection);
    }

    /**
     * Cancel a publish for the given collection.
     * @param collection
     */
    public void cancelPublish(Collection collection) {
        prePublishTasks.values().forEach(task -> task.removeCollection(collection));
    }

    /**
     * Remove the publish task once complete.
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
