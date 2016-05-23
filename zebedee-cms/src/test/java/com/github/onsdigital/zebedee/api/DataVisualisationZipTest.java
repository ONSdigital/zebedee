package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.content.page.visualisation.Visualisation;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests Verifies the {@link DataVisualisationZip} endpoint behaves correctly in the happy path scenario.
 */
public class DataVisualisationZipTest {

    private static final String ZIP_PATH_KEY = "zipPath";
    private static final String ZIP_PATH = "/data-visualisation/dataVis.zip";
    private static final String ZIP_WRITE_PATH = "/data-visualisation/";
    private static List<String> expectedZipContent;
    private static Set<String> filenamesSet = new HashSet<>(Arrays.asList(new String[]{"index.html"}));

    private DataVisualisationZip endpoint;

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

    @Mock
    private com.github.onsdigital.zebedee.model.Content mockContent;

    @Mock
    private BinaryOperator extractHtmlFilenames;

    private Resource zipResource;
    private Visualisation visualisation;
    private Path inProgressPath = Paths.get("/inProgress");

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        endpoint = new DataVisualisationZip();
        zipResource = new Resource();
        visualisation = new Visualisation();
        visualisation.setUid("1234657890");
        ReflectionTestUtils.setField(endpoint, "zebedeeApiHelper", apiHelperMock);
        ReflectionTestUtils.setField(endpoint, "extractHtmlFilenames", extractHtmlFilenames);
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
        when(mockCollectionReader.getContent("/"))
                .thenReturn(visualisation);
        when(mockCollection.getInProgress())
                .thenReturn(mockContent);
        when(mockContent.getPath())
                .thenReturn(inProgressPath);
        when(extractHtmlFilenames.apply(any(Path.class), any(Path.class)))
                .thenReturn(filenamesSet);

        zipResource.setData(getZipInputStream());

        // Run the test.
        endpoint.unpackDataVisualizationZip(mockRequest, mockResponse);

        // Verify
        verify(mockCollectionWriter, times(2)).getInProgress();
        verify(apiHelperMock, times(1)).getSession(mockRequest);
        verify(apiHelperMock, times(1)).getCollection(mockRequest);
        verify(apiHelperMock, times(1)).getZebedeeCollectionReader(mockCollection, mockSession);
        verify(mockCollectionReader, times(1)).getResource(ZIP_PATH);
        verify(apiHelperMock, times(1)).getZebedeeCollectionWriter(mockCollection, mockSession);

        //TODO work out what the hell this should be and uncomment it.
/*        verify(mockContentWriter, times(expectedZipContent.size())).write(any(ZipInputStream.class), anyString());

        for (String zipEntryName : expectedZipContent) {
            verify(mockContentWriter, times(1)).write(any(ZipInputStream.class), eq(zipEntryName));
        }*/
    }

    private static InputStream getZipInputStream() throws Exception {
        try {
            return DataVisualisationZipTest.class.getResourceAsStream(ZIP_PATH);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
