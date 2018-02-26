package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.CollectionBase;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.PostPublishCollectionTask;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.PrePublishCollectionsTask;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.PublishCollectionTask;
import com.github.onsdigital.zebedee.model.publishing.scheduled.task.PublishCollectionsTask;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Public interface to schedule publishes in Zebedee
 */
public class PublishScheduler extends Scheduler {

    private final Map<Date, PrePublishCollectionsTask> prePublishTasks = new HashMap<>();
    private final Map<Date, PublishCollectionsTask> publishTasks = new HashMap<>();

    @Override
    protected void schedule(Collection collection, Zebedee zebedee) {
        logInfo("Scheduling collection using optimised publisher").collectionName(collection).log();
        Date publishStartDate = collection.getDescription().getPublishDate();
        int getPreProcessSecondsBeforePublish = Configuration.getPreProcessSecondsBeforePublish();
        Date prePublishStartDate = new DateTime(publishStartDate).minusSeconds(getPreProcessSecondsBeforePublish).toDate();

        logInfo("Scheduling collection prepublish").collectionName(collection)
                .addParameter("prePublishStartDate", prePublishStartDate)
                .addParameter("publishStartDate", publishStartDate).log();
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
                logError(e, "Exception caught trying to schedule").log();
            }
        } else {
            logInfo("Not scheduling publish, scheduling is not enabled").log();
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

    public List<ScheduledPublishTaskData> getPrePublishTaskData(Zebedee zebedee) {

        List<ScheduledPublishTaskData> dataList = new ArrayList<>();

        for (PrePublishCollectionsTask task : prePublishTasks.values()) {
            ScheduledPublishTaskData data = new ScheduledPublishTaskData();
            data.delay = task.getDelay(TimeUnit.MILLISECONDS);
            data.scheduledPublishDate = new Date(System.currentTimeMillis() + data.delay);
            data.collectionIds = task.getCollectionIds();
            data.expectedPublishDate = task.getPublishDate();

            Set<Collection> collections = PrePublishCollectionsTask.loadCollections(task, zebedee);

            Set<CollectionBase> collectionData = new HashSet<>();
            for (Collection collection : collections) {
                collectionData.add(collection.description);
            }
            data.collections = collectionData;


            dataList.add(data);
        }

        return dataList;
    }
}
