package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.authorisation.UserIdentity;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

import static com.github.onsdigital.zebedee.api.Identity.GET_SESSION_ERROR;
import static com.github.onsdigital.zebedee.api.Identity.SESSION_NOT_FOUND;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by dave on 07/03/2018.
 */
public class IdentityTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private PrintWriter printWriterMock;

    @Mock
    private ZebedeeCmsService zebedeeCmsServiceMock;

    private Identity api;

    @Before
    public void setUp() throws Exception {
        api = new Identity();

        MockitoAnnotations.initMocks(this);

        ReflectionTestUtils.setField(api, "zebedeeCmsService", zebedeeCmsServiceMock);
    }

    @Test
    public void getIdentitySuccess() throws Exception {
        Session session = new Session();
        session.setEmail("el@strangerThings.com");
        session.setId("11");

        when(zebedeeCmsServiceMock.getSession(mockRequest))
                .thenReturn(session);
        when(mockResponse.getWriter())
                .thenReturn(printWriterMock);

        api.identifyUser(mockRequest, mockResponse);

        verify(zebedeeCmsServiceMock, times(1)).getSession(mockRequest);
        verify(mockResponse, times(1)).getWriter();
        verify(printWriterMock, times(1)).write(new UserIdentity(session).toJSON());
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_OK);
    }

    @Test
    public void sessionNotFound() throws Exception {
        when(zebedeeCmsServiceMock.getSession(mockRequest))
                .thenReturn(null);
        when(mockResponse.getWriter())
                .thenReturn(printWriterMock);

        api.identifyUser(mockRequest, mockResponse);

        verify(zebedeeCmsServiceMock, times(1)).getSession(mockRequest);
        verify(mockResponse, times(1)).getWriter();
        verify(printWriterMock, times(1)).write(SESSION_NOT_FOUND.toJSON());
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void getSessionError() throws Exception {
        when(zebedeeCmsServiceMock.getSession(mockRequest))
                .thenThrow(new NotFoundException("not found"));
        when(mockResponse.getWriter())
                .thenReturn(printWriterMock);

        api.identifyUser(mockRequest, mockResponse);

        verify(zebedeeCmsServiceMock, times(1)).getSession(mockRequest);
        verify(mockResponse, times(1)).getWriter();
        verify(printWriterMock, times(1)).write(GET_SESSION_ERROR.toJSON());
        verify(mockResponse, times(1)).setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }
}
