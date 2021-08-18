package com.github.onsdigital.zebedee.reader.api;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.FakeCollectionReaderFactory;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.reader.api.endpoint.PublishedData;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class PublishedDataTest {

    static {
        ReaderConfiguration cfg = ReaderConfiguration.init("target/test-classes/test-content/");

        if (ZebedeeReader.getCollectionReaderFactory() == null) {
            ZebedeeReader.setCollectionReaderFactory(new FakeCollectionReaderFactory(cfg.getCollectionsDir()));
        }
    }

    private PublishedData publishedData;

    private HttpServletRequest request = mock(HttpServletRequest.class);
    private HttpServletResponse response = mock(HttpServletResponse.class);
    private ServletOutputStream mockOutput = mock(ServletOutputStream.class);

    @Before
    public void initialize(){
        this.publishedData = new PublishedData();
    }

    @Test
    public void readReturnsSuccess() throws ZebedeeException, IOException {
        //given
        Map<String, String[]> filterType = new HashMap<String, String[]>();
        filterType.put("Title", new String[]{});
        when(request.getParameterMap()).thenReturn(filterType);
        when(request.getParameter("lang")).thenReturn(ContentLanguage.en.toString());
        when(request.getParameter("uri")).thenReturn("/economy/environmentalaccounts/articles/uknaturalcapitallandcoverintheuk/2015-03-17");
        when(request.getParameter("edition")).thenReturn("1.0.1");
        when(request.getParameter("releaseDate")).thenReturn((new Date()).toString());
        when(response.getOutputStream()).thenReturn(mockOutput);

        //when
        publishedData.read(request, response);

        //then
        verify(response, times(1)).setStatus(HttpStatus.SC_OK);
    }

    @Test
    public void readReturnsBadRequest() throws ZebedeeException, IOException {
        //given
        Map<String, String[]> filterType = new HashMap<String, String[]>();
        filterType.put("Title", new String[]{});
        when(request.getParameterMap()).thenReturn(filterType);
        when(request.getParameter("lang")).thenReturn(ContentLanguage.en.toString());
        when(request.getParameter("uri")).thenReturn("");
        when(request.getParameter("edition")).thenReturn("1.0.1");
        when(request.getParameter("releaseDate")).thenReturn((new Date()).toString());
        when(response.getOutputStream()).thenReturn(mockOutput);

        //when
        Exception exception = assertThrows(BadRequestException.class, () -> {
            publishedData.read(request, response);
        });

        //then
        String expectedMessage = "Please specify uri";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
        verify(response, times(0)).setStatus(HttpStatus.SC_OK);
    }
}
