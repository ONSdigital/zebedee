package com.github.onsdigital.zebedee.persistence.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

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
