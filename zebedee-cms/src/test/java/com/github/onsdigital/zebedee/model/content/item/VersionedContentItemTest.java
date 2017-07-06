package com.github.onsdigital.zebedee.model.content.item;

import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;


public class VersionedContentItemTest {
    @Test
    public void createVersionShouldCreateVersionDirectoryWithFilesCopied() throws Exception {

        // Given an instance of VersionedContentItem with a path to some content.
        Path rootPath = Files.createTempDirectory("VersionedContentItemTest");
        VersionedContentItem versionedContentItem = new VersionedContentItem("economy");
        Path contentItemPath = rootPath.resolve(versionedContentItem.getUri());
        FileUtils.touch(contentItemPath.resolve("data.json").toFile()); // create the data.json file in the content item directory

        ContentReader contentReader = new FileSystemContentReader(rootPath);
        // When we create a new version with the root of the content as the source for the version.
        ContentItemVersion version = versionedContentItem.createVersion(rootPath, contentReader, new ContentWriter(rootPath));

        // Then a directory exists for the version and the version identifier is set as expected.
        assertTrue(Files.exists(contentItemPath.resolve(VersionedContentItem.getVersionDirectoryName())));
        assertTrue(Files.exists(rootPath.resolve(version.getUri())));
        assertEquals("v1", version.getIdentifier());
    }

    @Test
    public void createVersionShouldCreateExpectedDirectoryWhenGapsExistsInVersions() throws Exception {

        // Given an instance of VersionedContentItem with a path to some content which has a v1 and V3 (explicitly missing v2).
        Path rootPath = Files.createTempDirectory("VersionedContentItemTest");
        String path = "economy";

        Files.createDirectories(rootPath.resolve(path).resolve(VersionedContentItem.VERSION_DIRECTORY).resolve("v1"));
        Files.createDirectories(rootPath.resolve(path).resolve(VersionedContentItem.VERSION_DIRECTORY).resolve("v3"));

        VersionedContentItem versionedContentItem = new VersionedContentItem(path);
        Path contentItemPath = rootPath.resolve(versionedContentItem.getUri());
        FileUtils.touch(contentItemPath.resolve("data.json").toFile()); // create the data.json file in the content item directory

        ContentReader contentReader = new FileSystemContentReader(rootPath);

        // When we create a new version.
        ContentItemVersion version = versionedContentItem.createVersion(rootPath, contentReader, new ContentWriter(rootPath));

        // Then a it creates a version after the highest version, ignoring the gap.
        assertEquals("v4", version.getIdentifier());
        assertTrue(Files.exists(contentItemPath.resolve(VersionedContentItem.getVersionDirectoryName())));
        assertTrue(Files.exists(rootPath.resolve(version.getUri())));

    }

    @Test
    public void createVersionShouldCreateIncrementalVersionNumbers() throws Exception {

        // Given an existing version of some content.
        Path rootPath = Files.createTempDirectory("VersionedContentItemTest");
        VersionedContentItem versionedContentItem = new VersionedContentItem("economy");
        Path contentItemPath = rootPath.resolve(versionedContentItem.getUri());
        FileUtils.touch(contentItemPath.resolve("data.json").toFile()); // create the data.json file in the content item directory

        ContentReader contentReader = new FileSystemContentReader(rootPath);
        ContentWriter contentWriter = new ContentWriter(rootPath);
        ContentItemVersion version = versionedContentItem.createVersion(rootPath, contentReader, contentWriter);

        // When we create a new version with the root of the content as the source for the version.
        ContentItemVersion version2 = versionedContentItem.createVersion(rootPath, contentReader, contentWriter);
        ContentItemVersion version3 = versionedContentItem.createVersion(rootPath, contentReader, contentWriter);

        // Then a directory exists for the version and the version identifier is set as expected.
        assertTrue(Files.exists(contentItemPath.resolve(VersionedContentItem.getVersionDirectoryName())));
        assertTrue(Files.exists(rootPath.resolve(version.getUri())));
        assertTrue(Files.exists(rootPath.resolve(version2.getUri())));
        assertTrue(Files.exists(rootPath.resolve(version3.getUri())));
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

    @Test
    public void resolveBaseUriShouldReturnSameUriForNonVersionedUri() {
        // Given a URI that is not versioned (does not live under the "previous" versions directory)
        String uri = "/some/content/not/versioned";

        // When the static resolveBaseUri method is called
        String baseUri = VersionedContentItem.resolveBaseUri(uri);

        // Then the result should be the same
        assertEquals(baseUri, uri);
    }

    @Test
    public void resolveBaseUriShouldReturnBaseUriForVersionedUri() {
        String expectedBaseUri = "/some/content";
        String uri = expectedBaseUri + "/previous/v1";

        String baseUri = VersionedContentItem.resolveBaseUri(uri);

        // Then the result should be the same
        assertEquals(expectedBaseUri, baseUri);
    }

    @Test
    public void resolveBaseUriShouldReturnBaseUriWithFileExtensionForVersionedUri() {
        String basePath = "/some/content";
        String expectedBaseUri = basePath + "/data.json";
        String uri = basePath + "/previous/v1/data.json";

        String baseUri = VersionedContentItem.resolveBaseUri(uri);

        // Then the result should be the same
        assertEquals(expectedBaseUri, baseUri);
    }

    @Test
    public void getLastVersionIdentifier() throws Exception {

        // Given an instance of VersionedContentItem with a path to some content which has a v1 and V3 (explicitly missing v2).
        Path rootPath = Files.createTempDirectory("VersionedContentItemTest");
        String path = "economy";

        Files.createDirectories(rootPath.resolve(path).resolve(VersionedContentItem.VERSION_DIRECTORY).resolve("v1"));
        Files.createDirectories(rootPath.resolve(path).resolve(VersionedContentItem.VERSION_DIRECTORY).resolve("v3"));

        // When we create a new version.
        String lastVersionIdentifier = VersionedContentItem.getLastVersionIdentifier(rootPath.resolve(path));

        // Then a it creates a version after the highest version, ignoring the gap.
        assertEquals("v3", lastVersionIdentifier);
    }
}
