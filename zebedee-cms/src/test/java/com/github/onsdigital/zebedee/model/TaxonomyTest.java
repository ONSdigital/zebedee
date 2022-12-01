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
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.reader.api.endpoint.Taxonomy;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.user.model.User;
import com.google.common.net.MediaType;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.ws.rs.HttpMethod;

public class TaxonomyTest {
	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpServletResponse response;

	@Mock
	private Taxonomy taxonomy1;

	private static final String JWT_TOKEN             = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCIsInVzZXJuYW1lIjoiamFuZWRvZUBleGFtcGxlLmNvbSJ9.fC3P6jnpnhmOxdlw0u4nOhehz7dCXsqX7RvqI1gEC4wrJoE6rlKH1mo7lR16K-EXWdXRoeN0_z0PZQzo__xOprAsY2XSNOexOcIo3hoydx6CkGWGmNNsLp35iGY3DgW6SLpQsdGF8HicJ9D9KCTPXKAGmOrkX3t92WSCLiQXXuER9gndzC6oLMU0akvKDstoTfwLWeSsogOQBn7_lUqGaHC8T06ZR37n_eOgUGSXwSFuYbg1zcY2xK3tMh4Wo8TOrADOrfLg660scpXuu-oDf0PNdgpXGU318IK1R0A2LiqqJWIV1sDE88uuPcX9-xgKc0eUn6qABXM9qhEyr6MS6g";
	public static final String AUTH_HEADER = "Authorization";
	Path path;
	Content taxonomy;
	ReaderConfiguration cfg;

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

	@Test
	public void shouldGetAddCacheControl() {

		when(response.getHeader(AUTH_HEADER)).thenReturn(JWT_TOKEN);
		try {
			doNothing().when(taxonomy1).get(request,response);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ZebedeeException e) {
			throw new RuntimeException(e);
		}
		verify(response, times(1)).addHeader(anyString(),anyString());

		System.out.println(request);
		System.out.println(response);
	}
}
