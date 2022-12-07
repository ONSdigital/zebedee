package com.github.onsdigital.zebedee.model;

import static com.github.onsdigital.zebedee.reader.util.RequestUtils.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.*;
import java.util.Collection;

import com.github.onsdigital.zebedee.ZebedeeConfiguration;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.reader.api.endpoint.Taxonomy;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import com.github.onsdigital.zebedee.user.model.User;
import com.google.common.net.MediaType;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.ws.rs.HttpMethod;

public class TaxonomyTest {

	protected static final String COLLECTION_ID = "123456789";
	Content taxonomy;

	Path path;
		@Before
	public void setUp() throws Exception {
		path = Files.createTempDirectory(this.getClass().getSimpleName());
		taxonomy = new Content(path);
	}

	@After
	public void tearDown() throws Exception {
		FileUtils.deleteDirectory(path.toFile());
	}

	@Test
	public void shouldExist() throws IOException {

		// Given
		String folder = "economy";
		String name = "data.json";
		URI uri = URI.create("/" + folder + "/" + name);
		Files.createDirectory(path.resolve(folder));
		Files.createFile(path.resolve(folder).resolve(name));

		// When
		boolean exists = taxonomy.exists(uri);

		// Then
		assertTrue(exists);
	}

	@Test
	public void shouldGet() throws IOException {

		// Given
		String folder = "economy";
		String name = "data.json";
		URI uri = URI.create("/" + folder + "/" + name);
		Files.createDirectory(path.resolve(folder));
		Files.createFile(path.resolve(folder).resolve(name));

		// When
		Path path = taxonomy.get(uri);

		// Then
		assertNotNull(path);
	}

	@Test
	public void shouldNotExist() throws IOException {

		// Given
		String folder = "economy";
		String name = "nothere.json";
		URI uri = URI.create("/" + folder + "/" + name);

		// When
		boolean exists = taxonomy.exists(uri);

		// Then
		assertFalse(exists);
	}

	@Test
	public void shouldNotGet() throws IOException {

		// Given
		String folder = "economy";
		String name = "nothere.json";
		URI uri = URI.create("/" + folder + "/" + name);

		// When
		Path path = taxonomy.get(uri);

		// Then
		assertNull(path);
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldFailIfFolderDoesntExist() throws IOException {

		// Given
		FileUtils.deleteDirectory(path.toFile());

		// When
		taxonomy = new Content(path);

		// Then
		// We should get an exception because the directory is invalid.
	}

	@Test
	public void shouldListUris() throws IOException {

		// Given
		String uri1 = "/some/content.html";
		String uri2 = "/some/other/content.html";
		Path path1 = taxonomy.toPath(uri1);
		Path path2 = taxonomy.toPath(uri2);
		Files.createDirectories(path1.getParent());
		Files.createDirectories(path2.getParent());
		Files.createFile(path1);
		Files.createFile(path2);

		// When
		List<String> uris = taxonomy.uris();

		// Then
		assertEquals(2, uris.size());
		assertTrue(uris.contains(uri1));
		assertTrue(uris.contains(uri2));
	}
}
