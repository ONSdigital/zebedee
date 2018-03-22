package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.ContentDetailDescription;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.github.onsdigital.zebedee.model.Content.isVisible;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ContentTest {

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

        baseContent = new ContentDetail();
        baseContent.description = new ContentDetailDescription("Some release 2014");
        baseContent.type = "home";

        // Serialise
        try (OutputStream output = Files.newOutputStream(baseJsonFile)) {
            Serialiser.serialise(output, baseContent);
        }

        Files.createDirectory(subDirectoryC);
        Files.createDirectory(subDirectoryA);
        Files.createDirectory(subDirectoryB);

        subContent = new ContentDetail();
        subContent.description = new ContentDetailDescription("Some sub 2015");
        subContent.type = "t2";

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

        bulletinContent = new ContentDetail();
        bulletinContent.description = new ContentDetailDescription("Some bulletin 2010");
        bulletinContent.type = "bulletin";

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
    public void shouldGetDetailsForUri() throws IOException {

        // Given an instance of content
        Content content = new Content(basePath);

        // When the details method is called with a uri
        ContentDetail result = content.details(baseJsonFile);

        // The result has the expected values
        assertEquals(baseContent.description.title, result.description.title);
        assertEquals(baseContent.type, result.type);
        assertEquals("/", result.uri);
    }

    @Test
    public void shouldGetNestedDetails() throws IOException {

        // Given an instance of content
        Content content = new Content(basePath, basePath);

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
        Content content = new Content(basePath, basePath);

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
        Content content = new Content(basePath, basePath);

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

    @Test
    public void listTimeSeriesDirectoriesShouldReturnListOfTimeseriesDirectories() throws IOException {

        // Given an instance of content with a timeseries folder nested under another time
        Content content = new Content(basePath);

        // When the listTimeSeriesDirectories method is called
        List<Path> paths = content.listTimeSeriesDirectories();

        // Then the releases directory will not be in the children.
        assertNotNull(paths);

        assertEquals(2, paths.size());
    }

    @Test
    public void shouldGetAllUris() throws IOException {

        // Given a content instance with a json file and csv file in it.
        Path basePath = Files.createTempDirectory(this.getClass().getSimpleName());
        String jsonFile = Random.id() + ".json";
        String csvFile = Random.id() + ".csv";
        Files.createFile(basePath.resolve(jsonFile));
        Files.createFile(basePath.resolve(csvFile));
        Content content = new Content(basePath);

        // When the uris method is called
        List<String> results = content.uris();

        // The result has the expected values
        assertEquals(2, results.size());
        assertTrue(results.contains("/" + jsonFile));
        assertTrue(results.contains("/" + csvFile));
    }

    @Test
    public void shouldApplyGlobToUris() throws IOException {

        // Given a content instance with a json file and csv file in it.
        Path basePath = Files.createTempDirectory(this.getClass().getSimpleName());
        Path directoryPath = Files.createDirectory(basePath.resolve("somedirectory"));
        String jsonFile = Random.id() + ".json";
        String csvFile = Random.id() + ".csv";
        Files.createFile(directoryPath.resolve(jsonFile));
        Files.createFile(directoryPath.resolve(csvFile));
        Content content = new Content(basePath);

        // When the details method is called with a uri
        List<String> results = content.uris("*.json");

        // The result has the expected values
        assertEquals(1, results.size());
        assertTrue(results.contains("/somedirectory/" + jsonFile));
    }

    @Test
    public void isVisibleForCollectionOwner_ShouldReturnFalseIfPathEndsWithTimeseriesDir() throws Exception {
        Path p = Files.createTempDirectory("master");
        p = p.resolve("timeseries");
        p.toFile().mkdir();

        assertThat(isVisible(p), is(false));
    }

    @Test
    public void isVisibleForCollectionOwner_ShouldReturnFalseIfPathContainsTimeseriesDir() throws Exception {
        Path zebedeeURI = Files.createTempDirectory("master");
        zebedeeURI = zebedeeURI.resolve("timeseries");
        zebedeeURI.toFile().mkdir();
        zebedeeURI = zebedeeURI.resolve("nested");
        zebedeeURI.toFile().mkdir();

        assertThat(isVisible(zebedeeURI), is(false));
    }

    @Test
    public void isVisibleForCollectionOwner_ShouldReturnTrueIfPathDoesNotConatinTimeseriesDir() throws Exception {
        Path p = Files.createTempDirectory("master");
        p = p.resolve("datasets");
        p.toFile().mkdir();

        assertThat(isVisible(p), is(true));
    }


    @Test
    public void isVisibleForCollectionOwner_ShouldReturnTrueIfPathContainsDirWhereTimeseriesIsASubStringOfTheDirName() throws Exception {
        Path p = Files.createTempDirectory("master");
        p = p.resolve("thisisnotatimeseriesdir");
        p.toFile().mkdir();

        boolean result = isVisible(p);
        assertThat(result, is(true));
    }
}
