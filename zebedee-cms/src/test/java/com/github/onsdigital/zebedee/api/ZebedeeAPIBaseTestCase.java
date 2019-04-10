package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.session.model.Session;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.MessageFormat;

import static org.mockito.Mockito.when;

/**
 * Created by dave on 6/3/16.
 */
public abstract class ZebedeeAPIBaseTestCase {

    protected static final String TEST_EMAIL = "test@email.com";
    protected static final String COLLECTION_ID = "123456789";
    protected static String REQUESTED_URI = "/{0}/" + COLLECTION_ID;

    @Mock
    protected HttpServletRequest mockRequest;

    @Mock
    protected HttpServletResponse mockResponse;

    protected Session session;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        session = new Session();
        session.setEmail(TEST_EMAIL);

        customSetUp();
        REQUESTED_URI = MessageFormat.format(REQUESTED_URI, getAPIName());

        when(mockRequest.getRequestURI())
                .thenReturn(REQUESTED_URI);

    }

    protected abstract void customSetUp() throws Exception;

    protected abstract Object getAPIName();
}
