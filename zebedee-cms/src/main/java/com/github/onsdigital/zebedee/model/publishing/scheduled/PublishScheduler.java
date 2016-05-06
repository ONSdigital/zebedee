package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.PostPublishCollectionTask;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.PrePublishCollectionsTask;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.PublishCollectionTask;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.PublishCollectionsTask;
import com.github.onsdigital.zebedee.util.Log;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.onsdigital.zebedee.logging.SimpleLogBuilder.logError;

/**
 * Public interface to schedule publishes in Zebedee
 */
public class PublishScheduler extends Scheduler {

    private final Map<Date, PrePublishCollectionsTask> prePublishTasks = new HashMap<>();
    private final Map<Date, PublishCollectionsTask> publishTasks = new HashMap<>();

    @Override
    protected void schedule(Collection collection, Zebedee zebedee) {
        Log.print("Scheduling collection using optimised publisher: %s", collection.description.name);
        Date publishStartDate = collection.description.publishDate;
        int getPreProcessSecondsBeforePublish = Configuration.getPreProcessSecondsBeforePublish();
        Date prePublishStartDate = new DateTime(publishStartDate).minusSeconds(getPreProcessSecondsBeforePublish).toDate();

        Log.print("Scheduling collection %s prepublish: %s, publish %s", collection.description.name, prePublishStartDate, publishStartDate);
        schedulePrePublish(collection, zebedee, prePublishStartDate, publishStartDate);
    }

    @Override
    public void cancel(Collection collection) {
        prePublishTasks.values().forEach(task -> task.removeCollection(collection));
        publishTasks.values().forEach(task -> task.removeCollection(collection));
    }

    /**
     * Validate and schedule the given collection to be published based on the publish date set in the collection.
     *
     * @param collection          The collection to publish.
     * @param prePublishStartDate The start date of the pre-publish date.
     * @param publishStartDate    The start date of the publish process.
     */
    void schedulePrePublish(Collection collection, Zebedee zebedee, Date prePublishStartDate, Date publishStartDate) {
        // cancel existing publish for the collection
        cancel(collection);

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
                logError("Exception caught trying to schedule: " + e.getMessage());
            }
        } else {
            Log.print("Not scheduling publish, scheduling is not enabled");
        }
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
