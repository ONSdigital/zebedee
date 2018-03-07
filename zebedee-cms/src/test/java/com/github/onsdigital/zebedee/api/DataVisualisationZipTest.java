package com.github.onsdigital.zebedee.api;

import com.github.onsdigital.zebedee.content.page.visualisation.Visualisation;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.SimpleZebedeeResponse;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory;
import com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BinaryOperator;

import static com.github.onsdigital.zebedee.persistence.CollectionEventType.DATA_VISUALISATION_ZIP_UNPACKED;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests Verifies the {@link DataVisualisationZip} endpoint behaves correctly in the happy path scenario.
 */
public class DataVisualisationZipTest {

    private static final String ZIP_PATH_KEY = "zipPath";
    private static final String ZIP_PATH = "/data-visualisation/dataVis.zip";
    private static final String EXPECTED_PATH = "/data-visualisation/dataVis/unitTest.html";
    private static final String TEST_EMAIL = "hodor@gameOfThrones.com";

    private static Set<String> filenamesSet = new HashSet<>(Arrays.asList(new String[]{"unitTest.html"}));

    private DataVisualisationZip endpoint;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private ZebedeeCmsService zebedeeCmsServiceMock;

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
    private CollectionHistoryDao collectionHistoryDaoMock;

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
        CollectionHistoryDaoFactory.setCollectionHistoryDao(collectionHistoryDaoMock);
        ReflectionTestUtils.setField(endpoint, "zebedeeCmsService", zebedeeCmsServiceMock);
        ReflectionTestUtils.setField(endpoint, "extractHtmlFilenames", extractHtmlFilenames);
    }

    /**
     * Happy path test case.
     */
    @Test
    public void shouldUnzipSuccessfully() throws Exception {
        when(mockRequest.getParameter(ZIP_PATH_KEY))
                .thenReturn(ZIP_PATH);
        when(zebedeeCmsServiceMock.getSession(mockRequest))
                .thenReturn(mockSession);
        when(zebedeeCmsServiceMock.getCollection(mockRequest))
                .thenReturn(mockCollection);
        when(zebedeeCmsServiceMock.getZebedeeCollectionReader(mockCollection, mockSession))
                .thenReturn(mockCollectionReader);
        when(zebedeeCmsServiceMock.getZebedeeCollectionWriter(mockCollection, mockSession))
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
        SimpleZebedeeResponse response = endpoint.unpackDataVisualizationZip(mockRequest, mockResponse);

        // Verify
        verify(mockCollectionWriter, times(2)).getInProgress();
        verify(zebedeeCmsServiceMock, times(1)).getSession(mockRequest);
        verify(zebedeeCmsServiceMock, times(1)).getCollection(mockRequest);
        verify(zebedeeCmsServiceMock, times(1)).getZebedeeCollectionReader(mockCollection, mockSession);
        verify(mockCollectionReader, times(1)).getResource(ZIP_PATH);
        verify(zebedeeCmsServiceMock, times(1)).getZebedeeCollectionWriter(mockCollection, mockSession);
        verify(mockContentWriter, times(1)).writeObject(eq(visualisation), any());
        verify(mockContentWriter, times(1)).write(any(InputStream.class), eq(EXPECTED_PATH));
        verify(mockContentWriter, never()).write(any(InputStream.class), contains("__MACOSX"));
        verify(mockContentWriter, never()).write(any(InputStream.class), contains(".DS_Store"));
        verify(collectionHistoryDaoMock, times(1)).saveCollectionHistoryEvent(eq(mockCollection), eq(mockSession),
                eq(DATA_VISUALISATION_ZIP_UNPACKED), Matchers.<CollectionEventMetaData>anyVararg());

        assertThat(response, is(equalTo(DataVisualisationZip.unzipSuccessResponse)));
    }

    /**
     * Happy path test case for {@link DataVisualisationZip#deleteZipAndContent(HttpServletRequest, HttpServletResponse)}.
     */
    @Test
    public void shouldDeleteZipAndContent() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_EMAIL);

        when(mockRequest.getParameter(ZIP_PATH_KEY))
                .thenReturn(ZIP_PATH);
        when(zebedeeCmsServiceMock.getSession(mockRequest))
                .thenReturn(session);
        when(zebedeeCmsServiceMock.getCollection(mockRequest))
                .thenReturn(mockCollection);

        assertThat(endpoint.deleteZipAndContent(mockRequest, mockResponse),
                is(equalTo(DataVisualisationZip.deleteContentSuccessResponse)));

        verify(mockRequest, times(1)).getParameter(ZIP_PATH_KEY);
        verify(zebedeeCmsServiceMock, times(1)).getCollection(mockRequest);
        verify(mockCollection, times(1)).deleteDataVisContent(session, Paths.get(ZIP_PATH));
    }

    /**
     * Test verifies behaviour for cases where no zipPath parameter is provided.
     *
     * @throws Exception expected.
     */
    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestExWhenZipParamIsMissing() throws Exception {
        try {
            endpoint.deleteZipAndContent(mockRequest, mockResponse);
        } catch (BadRequestException br) {
            assertThat(br.getMessage(), equalTo("Please specify the zip file path."));

            verify(zebedeeCmsServiceMock, never()).getSession(mockRequest);
            verify(zebedeeCmsServiceMock, never()).getCollection(mockRequest);
            verify(mockCollection, never()).deleteContentDirectory(any(), any());
            throw br;
        }
    }

    /**
     * Test verifies API behaves as expected in cases where an exception is thrown when trying to get the requested collection.
     *
     * @throws Exception expected.
     */
    @Test(expected = UnexpectedErrorException.class)
    public void shouldThrowNotFoundExceptionForGetCollectionError() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_EMAIL);

        try {
            when(mockRequest.getParameter(ZIP_PATH_KEY))
                    .thenReturn(ZIP_PATH);
            when(zebedeeCmsServiceMock.getSession(mockRequest))
                    .thenReturn(session);
            when(zebedeeCmsServiceMock.getCollection(mockRequest))
                    .thenThrow(new UnexpectedErrorException(null, 0));

            // Run the test.
            endpoint.deleteZipAndContent(mockRequest, mockResponse);

        } catch (UnexpectedErrorException ex) {
            verify(mockRequest, times(1)).getParameter(ZIP_PATH_KEY);
            verify(zebedeeCmsServiceMock, times(1)).getCollection(mockRequest);
            verify(mockCollection, never()).deleteContentDirectory(session.getEmail(), ZIP_PATH);
            throw ex;
        }
    }

    /**
     * Test verifies API behaves as expected in cases where an exception is thrown when there is an error while trying to
     * delete the data vis zip/content in the specified collection.
     */
    @Test(expected = UnexpectedErrorException.class)
    public void shouldThrowUnexpectedErrorForErrorWhileDeletingContent() throws Exception {
        Session session = new Session();
        session.setEmail(TEST_EMAIL);

        when(mockRequest.getParameter(ZIP_PATH_KEY))
                .thenReturn(ZIP_PATH);
        when(zebedeeCmsServiceMock.getSession(mockRequest))
                .thenReturn(session);
        when(zebedeeCmsServiceMock.getCollection(mockRequest))
                .thenReturn(mockCollection);
        when(mockCollection.deleteDataVisContent(session, Paths.get(ZIP_PATH)))
                .thenThrow(new IOException());

        try {
            endpoint.deleteZipAndContent(mockRequest, mockResponse);
        } catch (ZebedeeException ex) {
            verify(mockRequest, times(1)).getParameter(ZIP_PATH_KEY);
            verify(zebedeeCmsServiceMock, times(1)).getCollection(mockRequest);
            verify(mockCollection, times(1)).deleteDataVisContent(session, Paths.get(ZIP_PATH));
            throw ex;
        }
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
