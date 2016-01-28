package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.joda.time.DateTime;
import org.junit.Assert;

import java.io.IOException;
import java.util.concurrent.ScheduledFuture;

public class CollectionSchedulerTest {

    Zebedee zebedee;
    Builder builder;
    Session session;

    //@Before
    public void setUp() throws Exception {
        builder = new Builder(this.getClass());
        zebedee = new Zebedee(builder.zebedee);
        session = zebedee.openSession(builder.administratorCredentials);
    }

    //@After
    public void tearDown() throws Exception {
        builder.delete();
    }

    // @Test
    public void scheduleShouldAddCollectionTask() throws IOException, ZebedeeException {

        // Given a scheduled collection
        CollectionDescription description = new CollectionDescription("collectionName");
        description.type = CollectionType.scheduled;
        description.publishDate = DateTime.now().plusDays(1).toDate();
        Collection collection = Collection.create(description, zebedee, session);

        // When a new task is scheduled for the collection.
        CollectionScheduler scheduler = new CollectionScheduler();
        Assert.assertTrue(scheduler.schedule(collection, new DummyTask()));

        // Then the scheduler contains a task for the collection.
        Assert.assertTrue(scheduler.taskExistsForCollection(collection));
    }

    //@Test
    public void scheduleShouldCancelAnExistingCollectionTask() throws IOException, ZebedeeException, InterruptedException {

        // Given a scheduled collection
        CollectionDescription description = new CollectionDescription("collectionName");
        description.type = CollectionType.scheduled;
        final DummyTask firstTask = new DummyTask();
        description.publishDate = DateTime.now().plusSeconds(1).toDate();
        Collection collection = Collection.create(description, zebedee, session);
        CollectionScheduler scheduler = new CollectionScheduler();
        Assert.assertTrue(scheduler.schedule(collection, firstTask));

        final ScheduledFuture<?> taskForCollection = scheduler.getTaskForCollection(collection);
        Assert.assertFalse(taskForCollection.isCancelled());

        // When a second task is scheduled for the collection.
        final DummyTask secondTask = new DummyTask();
        collection.description.publishDate = DateTime.now().plusSeconds(1).toDate();
        Assert.assertTrue(scheduler.schedule(collection, secondTask));
        final ScheduledFuture<?> updatedForCollection = scheduler.getTaskForCollection(collection);

        Assert.assertFalse(updatedForCollection.isCancelled());
        Assert.assertTrue(taskForCollection.isCancelled());

        // Then the scheduler contains a task for the collection.
        Assert.assertTrue(scheduler.taskExistsForCollection(collection));
    }

    // @Test
    public void scheduleShouldReturnFalseForManualCollection() throws IOException, ZebedeeException {

        // Given a manual collection
        CollectionDescription description = new CollectionDescription("collectionName");
        description.type = CollectionType.manual;
        Collection collection = Collection.create(description, zebedee, session);

        // When a task is scheduled for the collection.
        CollectionScheduler scheduler = new CollectionScheduler();
        boolean result = scheduler.schedule(collection, new DummyTask());

        // Then the result is false
        Assert.assertFalse(result);
        Assert.assertFalse(scheduler.taskExistsForCollection(collection));
    }

    // @Test
    public void cancelShouldRemoveTaskFromScheduler() throws IOException, ZebedeeException {

        // Given a scheduled collection.
        CollectionDescription description = new CollectionDescription("collectionName");
        description.type = CollectionType.scheduled;
        description.publishDate = DateTime.now().plusDays(1).toDate();
        Collection collection = Collection.create(description, zebedee, session);

        CollectionScheduler scheduler = new CollectionScheduler();
        scheduler.schedule(collection, new DummyTask());

        // When the task is cancelled
        scheduler.cancel(collection);

        // Then the scheduler no longer contains a task for the collection
        Assert.assertFalse(scheduler.taskExistsForCollection(collection));
    }

    //@Test
    public void cancelShouldNotThrowExceptionIfTaskDoesNotExist() throws IOException, ZebedeeException {

        // Given a scheduler with no tasks.

        CollectionScheduler scheduler = new CollectionScheduler();

        // When the task that does not exist is cancelled
        CollectionDescription description = new CollectionDescription("collectionName");
        description.type = CollectionType.scheduled;
        description.publishDate = DateTime.now().plusDays(1).toDate();
        Collection collection = Collection.create(description, zebedee, session);
        scheduler.cancel(collection);

        // Then the scheduler no longer contains a task for the collection
        Assert.assertFalse(scheduler.taskExistsForCollection(collection));
    }
}
