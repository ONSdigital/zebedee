package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.model.Collection;

import java.util.Date;
import java.util.Map;

/**
 * Public interface to schedule publishes in Zebedee
 */
public class ZebedeePublishScheduler {

    private static final int prePublishMinutesBeforePublish = 1;

    //    private Map<Date, AbstractScheduledCollectionsTask> prePublishTasks;
    private Map<Date, PublishCollectionsTask> publishTasks;


    public void schedulePublish(Collection collection) {

        // cancel existing publish for the collection
        publishTasks.values().forEach(task -> task.removeCollection(collection));

        Date publishDate = collection.description.publishDate;
        PublishCollectionsTask task;

        // add a task for the publish date if there is not already one.
        if (publishTasks.containsKey(publishDate)) {
            task = publishTasks.get(publishDate);
        } else {
            task = new PublishCollectionsTask();
            task.schedule(publishDate);
            publishTasks.put(publishDate, task);
        }

        task.addCollection(collection);
    }

    /**
     * Cancel a publish for the given collection.
     * @param collection
     */
    public void cancelPublish(Collection collection) {
        publishTasks.values().forEach(task -> task.removeCollection(collection));
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
}
