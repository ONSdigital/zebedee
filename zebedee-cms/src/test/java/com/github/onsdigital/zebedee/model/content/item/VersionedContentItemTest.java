package com.github.onsdigital.zebedee.model.content.item;

import org.junit.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;


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

    @Test
    public void createVersionShouldCreateIncrementalVersionNumbers() throws Exception {

        // Given an existing version of some content.
        Path path = Files.createTempDirectory("VersionedContentItemTest");
        VersionedContentItem versionedContentItem = new VersionedContentItem(URI.create(""), path);
        Files.createFile(versionedContentItem.getDataFilePath()); // create the data.json file in the content item directory
        ContentItemVersion version = versionedContentItem.createVersion(versionedContentItem.getPath());

        // When we create a new version with the root of the content as the source for the version.
        ContentItemVersion version2 = versionedContentItem.createVersion(versionedContentItem.getPath());
        ContentItemVersion version3 = versionedContentItem.createVersion(versionedContentItem.getPath());

        // Then a directory exists for the version and the version identifier is set as expected.
        assertTrue(Files.exists(version.getDataFilePath()));
        assertEquals("v1", version.getIdentifier());
        assertEquals("v2", version2.getIdentifier());
        assertEquals("v3", version3.getIdentifier());
    }

    @Test
    public void isVersionedUriShouldReturnFalseIfNotVersioned() {
        // Given a URI that is not versioned (does not live under the "previous" versions directory)
        String uri = "/some/content/not/versioned";

        // When the static isVersionedUri method is called
        boolean isVersioned = VersionedContentItem.isVersionedUri(uri);

        // Then the result should be false
        assertFalse(isVersioned);
    }

    @Test
    public void isVersionedUriShouldReturnTrueIfVersioned() {

        // Given a URI that is versioned (lives under "previous" directory and is prefixed with "v")
        String uri = "/some/content/previous/v1";

        // When the static isVersionedUri method is called
        boolean isVersioned = VersionedContentItem.isVersionedUri(uri);

        // Then the result should be true
        assertTrue(isVersioned);
    }
}
