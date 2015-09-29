package com.github.onsdigital.zebedee.model.publishing;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;
import org.elasticsearch.common.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class CollectionSchedulerTest {

    Zebedee zebedee;
    Builder builder;

    @Before
    public void setUp() throws Exception {
        builder = new Builder(this.getClass());
        zebedee = new Zebedee(builder.zebedee);
    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
    }

    @Test
    public void scheduleShouldAddCollectionTask() throws IOException, ZebedeeException {

        // Given a scheduled collection
        CollectionDescription description = new CollectionDescription("collectionName");
        description.type = CollectionType.scheduled;
        description.publishDate = DateTime.now().plusDays(1).toDate();
        Collection collection = Collection.create(description, zebedee, builder.administrator.email);

        // When a new task is scheduled for the collection.
        CollectionScheduler scheduler = new CollectionScheduler();
        Assert.assertTrue(scheduler.schedule(collection, new DummyTask()));

        // Then the scheduler contains a task for the collection.
        Assert.assertTrue(scheduler.taskExistsForCollection(collection));
    }

    @Test
    public void scheduleShouldReturnFalseForManualCollection() throws IOException, ZebedeeException {

        // Given a manual collection
        CollectionDescription description = new CollectionDescription("collectionName");
        description.type = CollectionType.manual;
        Collection collection = Collection.create(description, zebedee, builder.administrator.email);

        // When a task is scheduled for the collection.
        CollectionScheduler scheduler = new CollectionScheduler();
        boolean result = scheduler.schedule(collection, new DummyTask());

        // Then the result is false
        Assert.assertFalse(result);
        Assert.assertFalse(scheduler.taskExistsForCollection(collection));
    }

    @Test
    public void cancelShouldRemoveTaskFromScheduler() throws IOException, ZebedeeException {

        // Given a scheduled collection.
        CollectionDescription description = new CollectionDescription("collectionName");
        description.type = CollectionType.scheduled;
        description.publishDate = DateTime.now().plusDays(1).toDate();
        Collection collection = Collection.create(description, zebedee, builder.administrator.email);

        CollectionScheduler scheduler = new CollectionScheduler();
        scheduler.schedule(collection, new DummyTask());

        // When the task is cancelled
        scheduler.cancel(collection);

        // Then the scheduler no longer contains a task for the collection
        Assert.assertFalse(scheduler.taskExistsForCollection(collection));
    }
}
