package com.github.onsdigital.zebedee;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

}
