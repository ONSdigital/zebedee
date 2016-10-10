package com.github.onsdigital.zebedee.model.content.deleted;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

/**
 * Created by carlhembrough on 10/10/2016.
 */
public class DeletedContentEventTest {

    @Test
    public void testAddFileInitialisesList() throws Exception {
        // Given a DeletedContentEvent
        DeletedContentEvent event = new DeletedContentEvent("collectionid", "collectionNAme", new Date(), "/some/uri", "Page title");

        // When the addFile method is called without having initialised the files list
        event.addDeletedFile("/some/uri");

        // Then the file as added
        Assert.assertTrue(event.getDeletedFiles().size() == 1);
    }
}
