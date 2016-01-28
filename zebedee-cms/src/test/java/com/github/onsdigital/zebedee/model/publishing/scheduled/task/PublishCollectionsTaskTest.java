package com.github.onsdigital.zebedee.model.publishing.scheduled.task;

import org.junit.Test;

import java.util.ArrayList;

import static junit.framework.TestCase.assertTrue;

public class PublishCollectionsTaskTest {

    // test a single task runs
    // multiple tasks all run concurrent

    @Test
    public void test() {

        // Given 2 publish tasks and 2 post publish tasks in a PublishCollectionsTask.
        ArrayList<PublishCollectionTask> publishCollectionTasks = new ArrayList<>();
        DummyPublishCollectionTask publish1 = new DummyPublishCollectionTask(5);
        publishCollectionTasks.add(publish1);
        DummyPublishCollectionTask publish2 = new DummyPublishCollectionTask(3);
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
