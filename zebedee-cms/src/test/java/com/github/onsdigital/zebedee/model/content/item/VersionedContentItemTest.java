package com.github.onsdigital.zebedee.model.content.item;

import org.junit.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class VersionedContentItemTest {
    @Test
    public void createVersionShouldCreateVersionDirectoryWithFilesCopied() throws Exception {

        // Given an instance of VersionedContentItem with a path to some content.
        Path path = Files.createTempDirectory("VersionedContentItemTest");
        VersionedContentItem versionedContentItem = new VersionedContentItem(URI.create(""), path);
        Files.createFile(versionedContentItem.getDataFilePath()); // create the data.json file in the content item directory

        // When we create a new version with the root of the content as the source for the version.
        ContentItemVersion version = versionedContentItem.createVersion(versionedContentItem.getPath());

        // Then a directory exists for the version and the version identifier is set as expected.
        assertTrue(Files.exists(version.getDataFilePath()));
        assertEquals("v1", version.getIdentifier());
    }
}
