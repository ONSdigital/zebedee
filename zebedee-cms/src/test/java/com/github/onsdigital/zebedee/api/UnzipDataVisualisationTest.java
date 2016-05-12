package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.ZebedeeApiHelper;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests Verifies the {@link UnzipDataVisualisation} endpoint behaves correctly in the happy path scenario.
 */
public class UnzipDataVisualisationTest {

    private static final String ZIP_PATH_KEY = "zipPath";
    private static final String ZIP_PATH = "/data-visualisation/dataVis.zip";
    private static final String ZIP_WRITE_PATH = "/data-visualisation/";
    private static List<String> expectedZipContent;

    private UnzipDataVisualisation endpoint;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private ZebedeeApiHelper apiHelperMock;

    @Mock
    private Session mockSession;

    @Mock
    private com.github.onsdigital.zebedee.model.Collection mockCollection;

    @Mock
    private CollectionWriter mockCollectionWriter;

    @Mock
    private CollectionReader mockCollectionReader;

    @Mock
    private ContentWriter mockContentWriter;

    private Resource zipResource;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        endpoint = new UnzipDataVisualisation();
        zipResource = new Resource();
        ReflectionTestUtils.setField(endpoint, "zebedeeApiHelper", apiHelperMock);
    }

    @BeforeClass
    public static void setUpZipExpectations() {
        expectedZipContent = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(getZipInputStream())) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                String name = ZIP_WRITE_PATH + zipEntry.getName();
                if (zipEntry.isDirectory() && name.endsWith("/")) {
                    name = name.substring(0, name.lastIndexOf("/"));
                }
                expectedZipContent.add(name);
                zipEntry = zipInputStream.getNextEntry();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Happy path test case.
     */
    @Test
    public void shouldUnzipSuccessfully() throws Exception {
        when(mockRequest.getParameter(ZIP_PATH_KEY))
                .thenReturn(ZIP_PATH);
        when(apiHelperMock.getSession(mockRequest))
                .thenReturn(mockSession);
        when(apiHelperMock.getCollection(mockRequest))
                .thenReturn(mockCollection);
        when(apiHelperMock.getZebedeeCollectionReader(mockCollection, mockSession))
                .thenReturn(mockCollectionReader);
        when(apiHelperMock.getZebedeeCollectionWriter(mockCollection, mockSession))
                .thenReturn(mockCollectionWriter);
        when(mockCollectionReader.getResource(ZIP_PATH))
                .thenReturn(zipResource);
        when(mockCollectionWriter.getInProgress())
                .thenReturn(mockContentWriter);
        zipResource.setData(getZipInputStream());

        // Run the test.
        endpoint.unpackDataVisualizationZip(mockRequest, mockResponse);

        // Verify
        verify(mockCollectionWriter, times(1)).getInProgress();
        verify(apiHelperMock, times(1)).getSession(mockRequest);
        verify(apiHelperMock, times(1)).getCollection(mockRequest);
        verify(apiHelperMock, times(1)).getZebedeeCollectionReader(mockCollection, mockSession);
        verify(mockCollectionReader, times(1)).getResource(ZIP_PATH);
        verify(apiHelperMock, times(1)).getZebedeeCollectionWriter(mockCollection, mockSession);
        verify(mockContentWriter, times(expectedZipContent.size())).write(any(ZipInputStream.class), anyString());

        for (String zipEntryName : expectedZipContent) {
            verify(mockContentWriter, times(1)).write(any(ZipInputStream.class), eq(zipEntryName));
        }
    }

    private static InputStream getZipInputStream() throws Exception {
        try {
            return UnzipDataVisualisationTest.class.getResourceAsStream(ZIP_PATH);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
