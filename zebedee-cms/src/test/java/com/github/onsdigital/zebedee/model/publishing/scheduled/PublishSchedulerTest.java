package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;

public class PublishSchedulerTest {

    @Mock
    private Zebedee zebedee;

    @Mock
    private Collection collection;

    @Mock
    private Collection collection2;

    private CollectionDescription collectionDescription;
    private PublishScheduler scheduler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        scheduler = new PublishScheduler();

        collectionDescription = new CollectionDescription("Collection Name");
        when(collection.getDescription()).thenReturn(collectionDescription);
    }

    @Test
    public void schedulePrePublish_givenScheduledPublish_shouldSchedule() {

        // Given a scheduled collection
        DateTime publishDateTime = DateTime.now().plusSeconds(2000).withMillisOfSecond(0);
        collectionDescription.setPublishDate(publishDateTime.toDate());
        Date prePublishStartDate = publishDateTime.minusSeconds(1).toDate();

        // When the collection is scheduled for pre-publish
        scheduler.schedulePrePublish(collection, zebedee, prePublishStartDate, collectionDescription.getPublishDate());

        // Then the scheduled date should match the date it was given to be scheduled for.
        List<ScheduledPublishTaskData> prePublishTaskData = scheduler.getPrePublishTaskData(zebedee);

        Assert.assertNotNull(prePublishTaskData);
        ScheduledPublishTaskData taskData = prePublishTaskData.get(0);
        Assert.assertNotNull(taskData);
        Assert.assertTrue(taskData.collectionIds.contains(collectionDescription.getId()));

        // check scheduled time is as expected within 500ms tolerance
        Assert.assertTrue(taskData.scheduledPublishDate.getTime() - prePublishStartDate.getTime() <= 500);

        // cleanup
        scheduler.cancel(collection);
    }

    @Test
    public void cancel_givenScheduledPublish_shouldRemoveCollectionFromQueue() {

        // Given a scheduled collection
        Date startDate = DateTime.now().plusSeconds(2000).toDate();
        collectionDescription.setPublishDate(startDate);

        Date prePublishStartDate = new DateTime(startDate).minusSeconds(1).toDate();
        scheduler.schedulePrePublish(collection, zebedee, prePublishStartDate, startDate);

        // When we cancel the scheduled collection publish
        scheduler.cancel(collection);

        // Then the collection task is no longer queued.
        List<ScheduledPublishTaskData> prePublishTaskData = scheduler.getPrePublishTaskData(zebedee);
        Assert.assertNotNull(prePublishTaskData);
        ScheduledPublishTaskData taskData = prePublishTaskData.get(0);
        Assert.assertNotNull(taskData);
        Assert.assertFalse(taskData.collectionIds.contains(collectionDescription.getId()));
    }

    @Test
    public void cancel_givenTwoScheduledPublishes_shouldRemoveSecondCollectionFromQueueIfOtherPublishesAreScheduledFirst() {

        // Given two scheduled collections
        Date startDate = DateTime.now().plusSeconds(2000).toDate();
        collectionDescription.setPublishDate(startDate);
        Date prePublishStartDate = new DateTime(collectionDescription.getPublishDate()).minusSeconds(1).toDate();
        scheduler.schedulePrePublish(collection, zebedee, prePublishStartDate, startDate);

        Date startDate2 = DateTime.now().plusSeconds(3000).toDate();
        CollectionDescription description2 = new CollectionDescription("collection2");
        description2.setPublishDate(startDate2);
        when(collection2.getDescription()).thenReturn(description2);
        Date prePublishStartDate2 = new DateTime(description2.getPublishDate()).minusSeconds(1).toDate();
        scheduler.schedulePrePublish(collection2, zebedee, prePublishStartDate2, startDate2);

        // When the second collection is cancelled.
        scheduler.cancel(collection2);

        // Then it is removed from the scheduler.
        List<ScheduledPublishTaskData> prePublishTaskData = scheduler.getPrePublishTaskData(zebedee);
        Assert.assertNotNull(prePublishTaskData);
        ScheduledPublishTaskData taskData = prePublishTaskData.get(1);
        Assert.assertNotNull(taskData);
        Assert.assertFalse(taskData.collectionIds.contains(description2.getId()));
    }
}
