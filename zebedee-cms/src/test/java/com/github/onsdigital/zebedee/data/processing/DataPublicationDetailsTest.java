package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataPublicationDetailsTest {

    @Mock
    private ContentReader publishedContentReader;

    @Mock
    private ContentReader reviewedContentReader;

    private DataPagesSet pagesSet;
    private Path reviewedPath;
    private String datasetURI;
    private String landingPageURI;

    /**
     * Setup common test data and mocks.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Create a fake reviewed path
        reviewedPath = Paths.get("/some/path");
        when(reviewedContentReader.getRootFolder()).thenReturn(reviewedPath);

        // Generate an example set of dataset page details
        DataPagesGenerator generator = new DataPagesGenerator();
        pagesSet = generator.generateDataPagesSet("mynode", "mydata", 2015, 2, "mydata.csdb");
        datasetURI = pagesSet.timeSeriesDataset.getUri().toString();
        landingPageURI = pagesSet.datasetLandingPage.getUri().toString();

        /*
         Mock ContentReader.getDirectoryStream for the findFileUri method to avoid refactoring the app code.
         The getDirectoryStream method should not exist as it is exposing the underlying filestore to other classes
         when this should be abstracted. Unfortunately the testing required for this wider refactor is not possible
         at this time so this mocking approach is used to work around this failed abstraction for testing purposes.
         */
        List<Path> fileList = new ArrayList<>();
        fileList.add(reviewedPath.resolve(reviewedPath + pagesSet.fileUri));
        DirectoryStream<Path> ds = mock(DirectoryStream.class);
        when(ds.iterator()).thenReturn(fileList.iterator());
        when(reviewedContentReader.getDirectoryStream(anyString())).thenReturn(ds);
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(reviewedPath.toFile());
    }

    @Test
    public void initialiser_givenReviewedDataPageSet_shouldSucceed() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // a reviewed dataset page and landing page
        when(reviewedContentReader.getContent(eq(datasetURI))).thenReturn(pagesSet.timeSeriesDataset);
        when(reviewedContentReader.getContent(eq(landingPageURI))).thenReturn(pagesSet.datasetLandingPage);

        // When
        // we initialise details
        DataPublicationDetails actual = new DataPublicationDetails(publishedContentReader, reviewedContentReader, datasetURI);

        // Then
        // landing page should be correctly identified
        assertEquals(PageType.DATASET_LANDING_PAGE, actual.landingPage.getType());
        assertEquals(pagesSet.datasetLandingPage.getUri().toString(), actual.landingPageUri);

        // And
        // dataset page should be correctly identified
        assertEquals(PageType.TIMESERIES_DATASET, actual.datasetPage.getType());
        assertEquals(pagesSet.timeSeriesDataset.getUri().toString(), actual.datasetUri);

        // And
        // we expect the uri for the csdb file to be set
        assertNotNull(actual.fileUri);
        assertEquals(pagesSet.fileUri, actual.fileUri);

        // And
        // timeseries parent directory should be correctly identified
        assertEquals("/mynode/timeseries", actual.getTimeseriesFolder());
    }

    @Test
    public void initialiser_ifReviewedLandingPageNotPresent_pullsItFromPublished() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // a reviewed dataset page but no reviewed landing page (only published)
        when(reviewedContentReader.getContent(eq(datasetURI))).thenReturn(pagesSet.timeSeriesDataset);
        when(publishedContentReader.getContent(eq(landingPageURI))).thenReturn(pagesSet.datasetLandingPage);

        // When
        // we initialise details
        DataPublicationDetails details = new DataPublicationDetails(publishedContentReader, reviewedContentReader, datasetURI);

        // Then
        // published landing page should be identified
        assertEquals(PageType.TIMESERIES_DATASET, details.datasetPage.getType());
        assertEquals(pagesSet.timeSeriesDataset.getUri().toString(), details.datasetUri);
    }
}


