package com.github.onsdigital.zebedee.reader.api.endpoint;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.api.bean.PublishedIndexResponse;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class PublishedIndexTest {

    private PublishedIndex publishedIndex;

    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);

    @Before
    public void initialize(){ publishedIndex = new PublishedIndex();}

    @Test
    public void readReturnsSuccess() throws ZebedeeException, IOException {

        // When
        PublishedIndexResponse actual = publishedIndex.read(request, response);

        // Then
        assertNotNull(actual);
    }
}
