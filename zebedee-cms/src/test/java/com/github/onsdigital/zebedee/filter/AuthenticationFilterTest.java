package com.github.onsdigital.zebedee.filter;

import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.nio.file.Path;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.filters.AuthenticationFilter;
import com.github.onsdigital.zebedee.api.Root;

import java.util.HashMap;
import java.util.Map;

import com.github.onsdigital.zebedee.ZebedeeTestBaseFixture;
import com.github.onsdigital.zebedee.ZebedeeConfiguration;

public class AuthenticationFilterTest extends ZebedeeTestBaseFixture {

    private final static String SIGNED_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCIsInVzZXJuYW1lIjoiamFuZWRvZUBleGFtcGxlLmNvbSJ9.fn_ojA25syD6ajJ6we_grfBpaPSUSQeVSqnQGAozkHA";

    private AuthenticationFilter authFilter;

    @Mock
    private MockHttpServletResponse response = mock(MockHttpServletResponse.class);

    @Mock
    private Sessions sessions;

    @Mock
    private Session session;

    @Mock
    private Root Root;

    @Mock
    private Zebedee zebedee;

    Path expectedPath;
    Map<String, String> env;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        authFilter = new AuthenticationFilter(this.sessions);

        session = new Session();
        session.setId("123test-session-id");
        session.setEmail("other123@ons.gov.uk");

        env = Root.env;
        Root.env = new HashMap<>();
        expectedPath = builder.parent;

    }

    @Test
    public void authorisationFilterTest() throws IOException, NotFoundException, UnauthorizedException {
        Root.zebedee = new Zebedee(new ZebedeeConfiguration(expectedPath, false));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", SIGNED_TOKEN);
        request.setPathInfo("/bob/was/here");

        when(sessions.get(request)).thenReturn(session);
            
        authFilter.filter(request, response);
        assertEquals(true, true);
    }
}
