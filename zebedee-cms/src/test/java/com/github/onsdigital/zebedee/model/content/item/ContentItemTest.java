package com.github.onsdigital.zebedee.model.content.item;

import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import org.junit.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class ContentItemTest {

    @Test
    public void contentItemConstructorShouldSetPath() throws Exception {

        // Given an existing directory path
        Path path = Files.createTempDirectory("ContentItemTest");

        // When a new instance of content item is created with the path
        ContentItem contentItem = new ContentItem(URI.create(""), path);

        // Then the path of the content item is set as the path given in the constructor.
        assertEquals(path, contentItem.getPath());
    }

    @Test(expected = NotFoundException.class)
    public void contentItemConstuctorShouldThrowNotFoundIfPathDoesNotExist() throws Exception {

        // Given an existing directory path
        Path path = Paths.get("some/path/does/not/exist");

        // When a new instance of content item is created with the path
        ContentItem contentItem = new ContentItem(URI.create(""), path);

        // Then the path of the content item is set as the path given in the constructor.
        assertEquals(path, contentItem.getPath());
    }
}
