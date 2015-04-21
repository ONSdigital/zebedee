package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.ContentDetail;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ContentTest {

    String filename = Random.id() + ".json";
    private Path basePath;
    private Path jsonFilePath;
    private ContentDetail contentDetail;

    @Before
    public void setUp() throws Exception {
        basePath = Files.createTempDirectory(this.getClass().getSimpleName());
        jsonFilePath = basePath.resolve(filename);
        Files.createFile(jsonFilePath);
        contentDetail = new ContentDetail();
        contentDetail.name = "Some release 2014";

        // Serialise
        try (OutputStream output = Files.newOutputStream(jsonFilePath)) {
            Serialiser.serialise(output, contentDetail);
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
        ContentDetail result = content.details(filename);

        // The result has the expected values
        assertEquals(contentDetail.name, result.name);
    }

    @Test
    public void shouldGetDetails() throws IOException {

        // Given an instance of content
        Content content = new Content(basePath);

        // When the details method is called with a uri
        List<ContentDetail> results = content.details();

        // The result has the expected values
        assertEquals(1, results.size());
        assertEquals(contentDetail.name, results.get(0).name);
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
}
