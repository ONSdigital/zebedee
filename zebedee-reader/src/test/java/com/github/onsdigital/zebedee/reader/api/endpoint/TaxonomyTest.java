package com.github.onsdigital.zebedee.reader.api.endpoint;


import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TaxonomyTest {
    private Taxonomy taxonomy = new Taxonomy();
    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;


    @Test
    public void taxonomy() throws ZebedeeException, IOException {
        assertThrows(RuntimeException.class, () -> {
            taxonomy.get(mockRequest, mockResponse);
        });
        verify(mockResponse, atLeastOnce()).addHeader("Cache-Control", "max-age=" + 1800 + ", public");

    }
}