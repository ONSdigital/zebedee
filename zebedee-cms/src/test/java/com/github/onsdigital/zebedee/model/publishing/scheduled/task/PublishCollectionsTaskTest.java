package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.model.Collection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class PublishCollectionsTaskTest {

    @Mock
    private Collection collection;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void removeCollection_givenAScheduledCollectionPublish_shouldCancelCollectionTasks() {

        // Given a scheduled publish collection task
        ArrayList<PublishCollectionTask> publishCollectionTasks = new ArrayList<>();
        DummyPublishCollectionTask publish1 = new DummyPublishCollectionTask(collection, 0);
        publishCollectionTasks.add(publish1);

        ArrayList<PostPublishCollectionTask> postPublishCollectionTasks = new ArrayList<>();
        DummyPostPublishCollectionTask postPublish1 = new DummyPostPublishCollectionTask(publish1);
        postPublishCollectionTasks.add(postPublish1);

        PublishCollectionsTask task = new PublishCollectionsTask(publishCollectionTasks, postPublishCollectionTasks);

        assertTrue(publishCollectionTasks.contains(publish1));
        assertTrue(postPublishCollectionTasks.contains(postPublish1));

        // When collection publish is cancelled
        task.removeCollection(collection);

        // Then all publish and post-publish tasks for the collection should be removed
        assertFalse(publishCollectionTasks.contains(publish1));
        assertFalse(postPublishCollectionTasks.contains(postPublish1));
    }

    @Test
    public void run_givenTwoScheduledPublishes_shouldRunInTheCorrectOrder() throws IOException, CollectionNotFoundException {
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
