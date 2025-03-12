package com.github.onsdigital.zebedee.reader.api.endpoint;


import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TaxonomyTest {
    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;


    @Before
    public void initialize() {
        MockitoAnnotations.openMocks(this);
        ReaderConfiguration.init("target/test-classes/test-content/");
    }

    @Test
    public void taxonomy() {
        Taxonomy taxonomy = new Taxonomy();

        assertThrows(RuntimeException.class, () -> {
            taxonomy.get(mockRequest, mockResponse);
        });
        verify(mockResponse, atLeastOnce()).addHeader("Cache-Control", "max-age=" + 1800 + ", public");

    }
}
