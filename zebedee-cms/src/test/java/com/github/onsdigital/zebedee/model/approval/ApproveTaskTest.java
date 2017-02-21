package com.github.onsdigital.zebedee.model.approval;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.PendingDelete;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionTest;
import com.github.onsdigital.zebedee.model.DummyCollectionReader;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class ApproveTaskTest {

    @Test
    public void createPublishNotificationShouldIncludePendingDeletes() throws Exception {


        // Given a collection that contains pending deletes.
        Path collectionPath = Files.createTempDirectory(Random.id()); // create a temp directory to generate content into
        CollectionReader collectionReader = new DummyCollectionReader(collectionPath);
        Collection collection = CollectionTest.CreateCollection(collectionPath, "createPublishNotificationShouldIncludePendingDeletes");
        String uriToDelete = "some/uri/to/check";
        ContentDetail contentDetail = new ContentDetail("Title", uriToDelete, "type");
        PendingDelete pendingDelete = new PendingDelete("", contentDetail);
        collection.description.getPendingDeletes().add(pendingDelete);

        // When the publish notification is created as part of the approval process.
        PublishNotification publishNotification = ApproveTask.createPublishNotification(collectionReader, collection);

        // Then the publish notification contains the expected directory to delete.
        Assert.assertNotNull(publishNotification);
        Assert.assertTrue(publishNotification.hasUriToDelete(uriToDelete));
    }
}