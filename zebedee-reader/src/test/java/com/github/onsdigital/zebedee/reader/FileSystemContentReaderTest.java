package com.github.onsdigital.zebedee.reader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileSystemContentReaderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test(expected = NullPointerException.class)
    public void constructorShouldThrowExceptionIfPathNull() {
        new FileSystemContentReader(null);
    }

    @Test(expected = NullPointerException.class)
    public void constructorShouldThrowExceptionIfPathEmpty() {
        new FileSystemContentReader(Paths.get(""));
    }

    @Test
    public void constructorShouldCreateObject() {
        Path rootFolder = Paths.get("path");
        FileSystemContentReader reader = new FileSystemContentReader(rootFolder);
        assertThat(reader, notNullValue());
        assertThat(reader.getRootFolder(),  equalTo(rootFolder));
    }

    @Test
    public void shouldReturnCorrectMimeType_forCssFile() throws Exception {
        assertFileMimeType("test.css", "text/css");
    }

    @Test
    public void shouldReturnCorrectMimeType_forJSFile() throws Exception {
        assertFileMimeType("test.js", "application/javascript");
    }

    @Test
    public void shouldReturnCorrectMimeType_forHTMLFile() throws Exception {
        assertFileMimeType("test.html", "text/html");
    }

    @Test
    public void shouldReturnCorrectMimeType_forJSONFile() throws Exception {
        assertFileMimeType("test.json", "application/json");
    }

    @Test
    public void shouldReturnCorrectMimeType_forPNGFile() throws Exception {
        assertFileMimeType("test.png", "image/png");
    }

    void assertFileMimeType(String filename, String expectedMimeType) throws Exception {
        File f = temporaryFolder.newFile(filename);
        try {
            assertThat(FileSystemContentReader.determineMimeType(f.toPath()), equalTo(expectedMimeType));
        } finally {
            Files.deleteIfExists(f.toPath());
        }
    }
}
