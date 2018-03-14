package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.authorisation.AuthorisationService;
import com.github.onsdigital.zebedee.authorisation.UserIdentity;
import com.github.onsdigital.zebedee.authorisation.UserIdentityException;
import com.github.onsdigital.zebedee.json.JSONable;
import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.session.model.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class IdentityTest {

	private static final String JSON_CONTENT_TYPE = "application/json";
	private static final String CHAR_ENCODING = "UTF-8";
	private static final String AUTH_TOKEN = "666";
	private static final String AUTH_TOKEN_HEADER = "X-Florence-Token";

	@Mock
	private HttpServletRequest mockRequest;

	@Mock
	private HttpServletResponse mockResponse;

	@Mock
	private PrintWriter printWriterMock;

	@Mock
	private AuthorisationService authorisationService;

	private Identity api;

	@Before
	public void setUp() throws Exception {
		api = new Identity();

		MockitoAnnotations.initMocks(this);

		ReflectionTestUtils.setField(api, "authorisationService", authorisationService);
	}

	@Test
	public void shouldReturnUnauthorisedIfNoAuthTokenProvided() throws Exception {
		when(mockResponse.getWriter())
				.thenReturn(printWriterMock);

		api.identifyUser(mockRequest, mockResponse);

		verifyZeroInteractions(authorisationService);
		verifyResponseInteractions(new Error("user not authenticated"), SC_UNAUTHORIZED);
	}

	@Test
	public void shouldReturnExpectedErrorStatusIfIdentifyUserFails() throws Exception {
		when(mockRequest.getHeader(AUTH_TOKEN_HEADER))
				.thenReturn(AUTH_TOKEN);
		when(authorisationService.identifyUser(AUTH_TOKEN))
				.thenThrow(new UserIdentityException("bang!", SC_FORBIDDEN));
		when(mockResponse.getWriter())
				.thenReturn(printWriterMock);

		api.identifyUser(mockRequest, mockResponse);

		verify(authorisationService, times(1)).identifyUser(AUTH_TOKEN);
		verifyResponseInteractions(new Error("bang!"), SC_FORBIDDEN);
	}

	@Test
	public void shouldReturnIdentityAndOKResponseForSuccess() throws Exception {
		Session session = new Session();
		session.setEmail("dartagnan@strangerThings.com");
		session.setId(AUTH_TOKEN);

		UserIdentity identity = new UserIdentity(session);

		when(mockRequest.getHeader(AUTH_TOKEN_HEADER))
				.thenReturn(AUTH_TOKEN);
		when(authorisationService.identifyUser(AUTH_TOKEN))
				.thenReturn(identity);
		when(mockResponse.getWriter())
				.thenReturn(printWriterMock);

		api.identifyUser(mockRequest, mockResponse);

		verify(authorisationService, times(1)).identifyUser(AUTH_TOKEN);
		verifyResponseInteractions(identity, SC_OK);
	}

	@Test(expected = IOException.class)
	public void shouldThrowIOExIfFailsToWriteResponse() throws Exception {
		Session session = new Session();
		session.setEmail("dartagnan@strangerThings.com");
		session.setId(AUTH_TOKEN);

		UserIdentity identity = new UserIdentity(session);

		when(mockRequest.getHeader(AUTH_TOKEN_HEADER))
				.thenReturn(AUTH_TOKEN);
		when(authorisationService.identifyUser(AUTH_TOKEN))
				.thenReturn(identity);
		when(mockResponse.getWriter())
				.thenReturn(printWriterMock);
		when(mockResponse.getWriter())
				.thenThrow(new IOException("BOOM!"));

		try {
			api.identifyUser(mockRequest, mockResponse);
		} catch (IOException e) {
			verify(authorisationService, times(1)).identifyUser(AUTH_TOKEN);
			verify(mockResponse, times(1)).getWriter();
			verify(mockResponse, times(1)).setCharacterEncoding(CHAR_ENCODING);
			verify(mockResponse, times(1)).setContentType(JSON_CONTENT_TYPE);
			verifyZeroInteractions(printWriterMock);
			throw e;
		}
	}

	private void verifyResponseInteractions(JSONable body, int statusCode) throws IOException {
		verify(mockResponse, times(1)).getWriter();
		verify(mockResponse, times(1)).setCharacterEncoding(CHAR_ENCODING);
		verify(mockResponse, times(1)).setContentType(JSON_CONTENT_TYPE);
		verify(printWriterMock, times(1)).write(body.toJSON());
		verify(mockResponse, times(1)).setStatus(statusCode);
	}
}
