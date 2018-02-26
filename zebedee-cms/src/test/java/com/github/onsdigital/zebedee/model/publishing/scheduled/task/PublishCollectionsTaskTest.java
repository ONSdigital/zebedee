package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class PublishCollectionsTaskTest extends ZebedeeTestBaseFixture {

    Session session;

    @Override
    public void setUp() throws Exception {
        session = zebedee.openSession(builder.administratorCredentials);
    }

    @Test
    public void shouldCancelTask() throws IOException, ZebedeeException {

        CollectionDescription collectionDescription = new CollectionDescription("FirstCollection");
        collectionDescription.setPublishDate(new Date());
        collectionDescription.setType(CollectionType.scheduled);
        Collection collection = Collection.create(collectionDescription, zebedee, session);

        ArrayList<PublishCollectionTask> publishCollectionTasks = new ArrayList<>();
        DummyPublishCollectionTask publish1 = new DummyPublishCollectionTask(collection, 0);
        publishCollectionTasks.add(publish1);

        ArrayList<PostPublishCollectionTask> postPublishCollectionTasks = new ArrayList<>();
        DummyPostPublishCollectionTask postPublish1 = new DummyPostPublishCollectionTask(publish1);
        postPublishCollectionTasks.add(postPublish1);

        PublishCollectionsTask task = new PublishCollectionsTask(publishCollectionTasks, postPublishCollectionTasks);

        assertTrue(publishCollectionTasks.contains(publish1));
        assertTrue(postPublishCollectionTasks.contains(postPublish1));

        task.removeCollection(collection);

        assertFalse(publishCollectionTasks.contains(publish1));
        assertFalse(postPublishCollectionTasks.contains(postPublish1));
    }

    @Test
    public void shouldRunInTheCorrectOrder() throws IOException, CollectionNotFoundException {

        Collection collection = new Collection(builder.collections.get(0), zebedee);

        // Given 2 publish tasks and 2 post publish tasks in a PublishCollectionsTask.
        ArrayList<PublishCollectionTask> publishCollectionTasks = new ArrayList<>();
        DummyPublishCollectionTask publish1 = new DummyPublishCollectionTask(collection, 10);
        publishCollectionTasks.add(publish1);
        DummyPublishCollectionTask publish2 = new DummyPublishCollectionTask(collection, 5);
        publishCollectionTasks.add(publish2);
        ArrayList<PostPublishCollectionTask> postPublishCollectionTasks = new ArrayList<>();
        DummyPostPublishCollectionTask postPublish1 = new DummyPostPublishCollectionTask(2);
        postPublishCollectionTasks.add(postPublish1);
        DummyPostPublishCollectionTask postPublish2 = new DummyPostPublishCollectionTask(5);
        postPublishCollectionTasks.add(postPublish2);
        PublishCollectionsTask task = new PublishCollectionsTask(publishCollectionTasks, postPublishCollectionTasks);

        // When the publish task is run.
        task.run();

        // all tasks should be done
        assertTrue(publish1.isPublished());
        assertTrue(publish2.isPublished());
        assertTrue(postPublish1.isDone());
        assertTrue(postPublish2.isDone());

        // publish should start before post publish
        assertTrue(publish1.getStart().before(postPublish1.getStart()));
        assertTrue(publish1.getStart().before(postPublish2.getStart()));
        assertTrue(publish2.getStart().before(postPublish1.getStart()));
        assertTrue(publish2.getStart().before(postPublish2.getStart()));

        // publish should be finished before post publish
        assertTrue(publish1.getEnd().compareTo(postPublish1.getStart()) != 1);
        assertTrue(publish1.getEnd().compareTo(postPublish2.getStart()) != 1);
        assertTrue(publish2.getEnd().compareTo(postPublish1.getStart()) != 1);
        assertTrue(publish2.getEnd().compareTo(postPublish2.getStart()) != 1);
    }
}
