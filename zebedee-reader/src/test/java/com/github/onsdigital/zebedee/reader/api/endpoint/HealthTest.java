package com.github.onsdigital.zebedee.reader.api.endpoint;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class HealthTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private Health api;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.api = new Health();
    }

    @Test
    public void getHealth_shouldReturnStatusOK() {
        api.getHealth(request, response);

        verify(response, times(1)).setStatus(HttpStatus.SC_OK);
    }
}
