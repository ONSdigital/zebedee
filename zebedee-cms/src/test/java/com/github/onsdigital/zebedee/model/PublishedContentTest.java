package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.json.ContentDetail;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PublishedContentTest {

    private String filename = "data.json";
    private String directoryAName = "subdira";
    private String directoryBName = "subdirb";
    private String directoryCName = "subdirc";
    private String bulletinsDirectoryName = "bulletins";
    private String timeseriesDirectoryName = "timeseries";
    private String exampleBulletinName = "gdppreliminaryestimateq32014";

    private Path basePath; // base path for the working directory
    private Path baseJsonFile; // path of the data.json file in the base directory (the home page)
    private ContentDetail baseContent; // The object to serialise into the base directory data.json

    private Path subDirectoryJsonFile; // a json file for the sub directory
    private ContentDetail subContent; // the object to serialise into the sub directory data.json

    private Path bulletinDirectory;
    private Path exampleBulletinDirectory;
    private Path timeseriesDirectory1;
    private Path timeseriesDirectory2;
    private Path exampleBulletinJsonFile;
    private ContentDetail bulletinContent;

    @Before
    public void setUp() throws Exception {
        basePath = Files.createTempDirectory(this.getClass().getSimpleName());
        baseJsonFile = basePath.resolve(filename);
        Path subDirectoryA = basePath.resolve(directoryAName);
        Path subDirectoryB = basePath.resolve(directoryBName);
        Path subDirectoryC = basePath.resolve(directoryCName);
        subDirectoryJsonFile = subDirectoryA.resolve(filename);
        Files.createFile(baseJsonFile);

        baseContent = new ContentDetail("Some release 2014", "/", PageType.HOME_PAGE);

        // Serialise
        try (OutputStream output = Files.newOutputStream(baseJsonFile)) {
            Serialiser.serialise(output, baseContent);
        }

        Files.createDirectory(subDirectoryC);
        Files.createDirectory(subDirectoryA);
        Files.createDirectory(subDirectoryB);

        subContent = new ContentDetail("Some sub 2015", "/some-path", PageType.TAXONOMY_LANDING_PAGE);

        // Serialise
        try (OutputStream output = Files.newOutputStream(subDirectoryJsonFile)) {
            Serialiser.serialise(output, subContent);
        }


        bulletinDirectory = subDirectoryA.resolve(bulletinsDirectoryName);
        Files.createDirectory(bulletinDirectory);
        exampleBulletinDirectory = bulletinDirectory.resolve(exampleBulletinName);
        Files.createDirectory(exampleBulletinDirectory);
        exampleBulletinJsonFile = exampleBulletinDirectory.resolve(filename);

        timeseriesDirectory1 = subDirectoryA.resolve(timeseriesDirectoryName);
        Files.createDirectory(timeseriesDirectory1);
        timeseriesDirectory2 = subDirectoryB.resolve(timeseriesDirectoryName);
        Files.createDirectory(timeseriesDirectory2);

        bulletinContent = new ContentDetail("Some bulletin 2010", "/some-path/bulletins/some-bulletin", PageType.BULLETIN);

        // Serialise
        try (OutputStream output = Files.newOutputStream(exampleBulletinJsonFile)) {
            Serialiser.serialise(output, bulletinContent);
        }
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(basePath.toFile());
    }


    @Test
    public void shouldGetNestedDetails() throws IOException {

        // Given an instance of content
        PublishedContent content = new PublishedContent(basePath);

        // When the nestedDetails method is called
        ContentDetail root = content.nestedDetails();

        // Then the result has child nodes defined.
        assertNotNull(root);
        assertNotNull(root.children);
        assertTrue(root.children.size() > 0);
        assertEquals(baseContent.description.title, root.description.title);
    }

    @Test
    public void getNestedDetailsShouldAlphabeticallyOrderFiles() throws IOException {

        // Given an instance of content with three subdirectories
        PublishedContent content = new PublishedContent(basePath);

        // When the nestedDetails method is called
        ContentDetail root = content.nestedDetails();

        // Then the result has child nodes ordered alphabetically
        assertNotNull(root);
        assertNotNull(root.children);
        assertTrue(root.children.size() > 0);
        assertEquals("Some sub 2015", root.children.get(0).description.title);
        assertEquals(directoryBName, root.children.get(1).description.title);
        assertEquals(directoryCName, root.children.get(2).description.title);
    }


    @Test
    public void shouldGetNestedDetailsWithNoDataJsonFile() throws IOException {

        // Given an instance of content
        PublishedContent content = new PublishedContent(basePath);

        // When the nestedDetails method is called
        ContentDetail root = content.nestedDetails();

        // Then a directory with no data.json file will still be evaluated but only the name returned without the URI.
        ContentDetail bulletinDirectoryDetails = root.children.get(0).children.get(0);

        assertNotNull(bulletinDirectoryDetails);
        assertEquals(bulletinsDirectoryName, bulletinDirectoryDetails.description.title);
        assertTrue(bulletinDirectoryDetails.children.size() > 0);

        ContentDetail bulletinDetails = bulletinDirectoryDetails.children.get(0);
        assertNotNull(bulletinDetails);
        assertEquals(bulletinContent.description.title, bulletinDetails.description.title);
        assertEquals("/" + basePath.relativize(exampleBulletinDirectory), bulletinDetails.uri);
        assertTrue(bulletinDetails.children.size() == 0);
    }
}
