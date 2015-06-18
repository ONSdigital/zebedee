package com.github.onsdigital.zebedee.model;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class PathUtilsTest {

    Path folder;

    @Before
    public void setUp() throws Exception {
        folder = Files.createTempDirectory(this.getClass().getSimpleName());
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(folder.toFile());
    }

    @Test
    public void shouldCleanString() {

        // Given
        String string = "name.é+!@#$%^&*(){}][/=?+-_\\|;:`~!'\",<>";

        // When
        String result = PathUtils.toFilename(string);

        // Then
        assertEquals("name.é$_", result);
    }

    @Test
    public void shouldLowerCaseString() {

        // Given
        String string = "NaMe";

        // When
        String result = PathUtils.toFilename(string);

        // Then
        assertEquals("name", result);
    }

    @Test
    public void shouldAbbreviateString() {

        // Given
        String string = RandomStringUtils.random(500, "aoeuidhtns");

        // When
        String result = PathUtils.toFilename(string);

        // Then
        assertEquals(PathUtils.MAX_LENGTH, result.length());
        assertEquals(StringUtils.substring(string, 0, 127),
                StringUtils.substring(result, 0, 127));
        assertEquals(StringUtils.substring(string, -127),
                StringUtils.substring(result, -127));
    }

    @Test
    public void shouldCopyFile() throws IOException {

        // Given
        Path source = Files.createFile(folder.resolve("source"));
        Path destination = folder.resolve("destination");

        // When
        PathUtils.copy(source, destination);

        // Then
        assertTrue(Files.exists(destination));
        assertTrue(Files.exists(source));
    }

    @Test
    public void shouldCopyFileToNonexistentFolder() throws IOException {

        // Given
        // A destination where the parent folders don't exist:
        Path source = Files.createFile(folder.resolve("source"));
        Path destination = folder.resolve("non/existent/folder/destination");

        // When
        PathUtils.copy(source, destination);

        // Then
        assertTrue(Files.exists(destination));
        assertTrue(Files.exists(source));
    }

    @Test(expected = NoSuchFileException.class)
    public void shouldNotCopyNonexistentFile() throws IOException {

        // Given
        // The source file does not actually exist:
        Path source = folder.resolve("source");
        Path destination = folder.resolve("destination");

        // When
        PathUtils.copy(source, destination);

        // Then
        // We should have an exception
    }

    @Test
    public void shouldMoveFile() throws IOException {

        // Given
        Path source = Files.createFile(folder.resolve("source"));
        Path destination = folder.resolve("destination");

        // When
        PathUtils.move(source, destination);

        // Then
        assertTrue(Files.exists(destination));
        assertFalse(Files.exists(source));
    }

    @Test
    public void shouldMoveFileToDestinationThatAlreadyExists()
            throws IOException {

        // Given
        // The destination file already exists:
        Path source = Files.createFile(folder.resolve("source"));
        Path destination = Files.createFile(folder.resolve("destination"));

        // When
        PathUtils.move(source, destination);

        // Then
        assertTrue(Files.exists(destination));
        assertFalse(Files.exists(source));
    }

    @Test
    public void shouldMoveFileToNonexistentFolder() throws IOException {

        // Given
        // A destination where the parent folders don't exist:
        Path source = Files.createFile(folder.resolve("source"));
        Path destination = folder.resolve("non/existent/folder/destination");

        // When
        PathUtils.move(source, destination);

        // Then
        assertTrue(Files.exists(destination));
        assertFalse(Files.exists(source));
    }

    @Test(expected = NoSuchFileException.class)
    public void shouldNotMoveNonexistentFile() throws IOException {

        // Given
        // The source file does not actually exist:
        Path source = folder.resolve("source");
        Path destination = folder.resolve("destination");

        // When
        PathUtils.move(source, destination);

        // Then
        // We should have an exception
    }

    @Test
    public void shouldMoveAllFilesInDirectory() throws IOException {

        // Given - a source directory with two files.
        Path source = Files.createDirectory(folder.resolve("source"));
        Path sourceFile = source.resolve("data.json");
        Files.createFile(sourceFile);
        Files.createFile(source.resolve("uploaded.csv"));
        Path destination = folder.resolve("destination");
        Path destinationFile = destination.resolve("data.json");

        // When - we call moveFilesInDirectory() method with the path to
        // one of the files.
        PathUtils.moveFilesInDirectory(sourceFile, destinationFile);

        // Then - all files in the source directory are now in the destination directory.
        assertTrue(Files.exists(destinationFile));
        assertTrue(Files.exists(destination.resolve("uploaded.csv")));
        assertFalse(Files.exists(sourceFile));
    }

    @Test
    public void shouldNotMoveSubDirectories() throws IOException {

        // Given - a source directory with a sub directory containing a file
        Path source = Files.createDirectory(folder.resolve("source"));
        Path sourceFile = source.resolve("data.json");
        Files.createFile(sourceFile);
        Path subDirectory = Files.createDirectory(source.resolve("subdirectory"));
        Files.createFile(subDirectory.resolve("some.csv"));
        Path destination = folder.resolve("destination");
        Path destinationFile = destination.resolve("data.json");

        // When - we call moveFilesInDirectory()
        PathUtils.moveFilesInDirectory(sourceFile, destinationFile);

        // Then - The files are moved but the sub directory is not moved.
        assertTrue(Files.exists(destinationFile));
        assertFalse(Files.exists(sourceFile));
        Path destinationSubDirectory = destination.resolve("subdirectory");
        assertFalse(Files.exists(destinationSubDirectory.resolve("some.csv")));
        assertFalse(Files.exists(destinationSubDirectory));
    }

    @Test
    public void shouldCopyAllFilesInDirectory() throws IOException {

        // Given - a source directory with two files.
        Path source = Files.createDirectory(folder.resolve("source"));
        Path sourceFile = source.resolve("data.json");
        Files.createFile(sourceFile);
        Files.createFile(source.resolve("uploaded.csv"));
        Path destination = folder.resolve("destination");
        Path destinationFile = destination.resolve("data.json");

        // When - we call copyFilesInDirectory() method with the path to
        // one of the files.
        PathUtils.copyFilesInDirectory(sourceFile, destinationFile);

        // Then - all files in the source directory are now in the destination directory,
        // and still exist in the source directory.
        assertTrue(Files.exists(destinationFile));
        assertTrue(Files.exists(destination.resolve("uploaded.csv")));
        assertTrue(Files.exists(source));
        assertTrue(Files.exists(sourceFile));
    }

    @Test
    public void shouldNotCopySubDirectories() throws IOException {

        // Given - a source directory with a sub directory containing a file
        Path source = Files.createDirectory(folder.resolve("source"));
        Path sourceFile = source.resolve("data.json");
        Files.createFile(sourceFile);
        Path subDirectory = Files.createDirectory(source.resolve("subdirectory"));
        Files.createFile(subDirectory.resolve("some.csv"));
        Path destination = folder.resolve("destination");
        Path destinationFile = destination.resolve("data.json");

        // When - we call copyFilesInDirectory()
        PathUtils.copyFilesInDirectory(sourceFile, destinationFile);

        // Then - the files are copied but the sub directory is not copied.
        assertTrue(Files.exists(destinationFile));
        assertTrue(Files.exists(sourceFile));
        Path destinationSubDirectory = destination.resolve("subdirectory");
        assertFalse(Files.exists(destinationSubDirectory.resolve("some.csv")));
        assertFalse(Files.exists(destinationSubDirectory));
    }

    @Test
    public void shouldDeleteAllFilesInDirectory() throws IOException {

        // Given - a directory with two files.
        Path target = Files.createDirectory(folder.resolve("target"));
        Path targetJsonFile = target.resolve("data.json");
        Files.createFile(targetJsonFile);
        Path targetCsvFile = target.resolve("uploaded.csv");
        Files.createFile(targetCsvFile);

        // When - we call deleteFilesInDirectory() method with the path to
        // one of the files.
        PathUtils.deleteFilesInDirectory(targetJsonFile);

        // Then - all files in the directory are deleted
        assertFalse(Files.exists(targetCsvFile));
        assertFalse(Files.exists(targetJsonFile));
    }

    @Test
    public void shouldNotDeleteSubDirectories() throws IOException {

        // Given - a directory with a sub directory containing a file
        Path target = Files.createDirectory(folder.resolve("target"));
        Path targetFile = target.resolve("data.json");
        Files.createFile(targetFile);
        Path subDirectory = Files.createDirectory(target.resolve("subdirectory"));
        Path subDirectoryFile = subDirectory.resolve("some.csv");
        Files.createFile(subDirectoryFile);

        // When - we call deleteFilesInDirectory()
        PathUtils.deleteFilesInDirectory(targetFile);

        // Then - the file is deleted but the sub directory is not copied.
        assertFalse(Files.exists(targetFile.resolve("some.csv")));
        assertTrue(Files.exists(subDirectory));
        assertTrue(Files.exists(subDirectoryFile));
    }

    @Test
    public void shouldCreateUri() {

        // Given - a path
        String expected = "/some/path";
        Path path = Paths.get(expected);

        // When - we call toUri on the path
        String actual = PathUtils.toUri(path);

        // Then - the uri string is returned.
        assertEquals(expected, actual);
    }

    @Test
    public void shouldAddLeadingSlashToUri() {

        // Given - a path
        String expected = "/some/path";
        Path path = Paths.get(expected.replaceFirst("/", ""));

        // When - we call toUri on the path
        String actual = PathUtils.toUri(path);

        // Then - the uri string is returned.
        assertEquals(expected, actual);
    }
}
