package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.TimeSeriesDataset;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateCommand;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.ContentReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for the data publication object
 */
public class DataPublicationTest {

    @Mock
    private ContentReader publishedContentReader;

    @Mock
    private ContentReader reviewedContentReader;

    @Mock
    private ContentWriter reviewedContentWriter;

    @Mock
    private DataIndex dataIndex;

    @Mock
    private DataLink dataLink;

    private final List<TimeseriesUpdateCommand> updateCommands = new ArrayList<>();
    private DataPagesSet csdbPageSet;
    private DataPagesSet csvPageSet;
    private List<DownloadSection> downloadSections;

    private MockedConstruction<DataProcessor> dataProcessorMock;
    private MockedConstruction<DataWriter> dataWriterMock;
    private MockedConstruction<DataFileGenerator> dataFileGeneratorMock;

    /**
     * Setup generates an instance of zebedee, a collection, and various DataPagesSet objects (that are test framework generators)
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Generate example sets of dataset pages
        DataPagesGenerator generator = new DataPagesGenerator();
        csdbPageSet = generator.generateDataPagesSet("datasetIds", "temp", 2015, 2, "abcd.csdb");
        csvPageSet = generator.generateDataPagesSet("datasetIds", "temp", 2015, 2, "upload-abcd.csv");

        // Generate an example set of download sections
        DownloadSection downloadSection1 = new DownloadSection();
        downloadSection1.setTitle("Download 1");
        DownloadSection downloadSection2 = new DownloadSection();
        downloadSection2.setTitle("Download 2");
        downloadSections = new ArrayList<>();
        downloadSections.add(downloadSection1);
        downloadSections.add(downloadSection2);

        // Mock classes that are instantiated by the process function rather than refactoring the app code to use dependency
        // injection as there is no scope for the manual regression testing required should these changes be made
        dataProcessorMock = Mockito.mockConstruction(DataProcessor.class, (mock, context) -> {});
        dataWriterMock = Mockito.mockConstruction(DataWriter.class, (mock, context) -> {});
        dataFileGeneratorMock = Mockito.mockConstruction(DataFileGenerator.class, (mock, context) -> {
            when(mock.generateDataDownloads(any(DataPublicationDetails.class), any(TimeSerieses.class))).thenReturn(downloadSections);
        });

        when(dataLink.callCSDBProcessor(anyString(),any(ContentReader.class))).thenReturn(csdbPageSet.getTimeSerieses());
        when(dataLink.callCSVProcessor(anyString(),any(ContentReader.class))).thenReturn(csdbPageSet.getTimeSerieses());
    }

    @After
    public void tearDown() {
        dataProcessorMock.close();
        dataWriterMock.close();
        dataFileGeneratorMock.close();
    }

    @Test
    public void process_givenPublicationInReview_isSuccessful() throws ZebedeeException, IOException, URISyntaxException {
        // Given
        // our data in review
        DataPublicationDetails publicationDetails = new DataPublicationDetails(csdbPageSet.timeSeriesDataset, csdbPageSet.datasetLandingPage, csdbPageSet.fileUri);

        DataPublication publication = new DataPublication(publicationDetails);
        publication.setDataLink(dataLink);

        // When
        // we initialise publication
        publication.process(publishedContentReader, reviewedContentReader, reviewedContentWriter, dataIndex, updateCommands);

        // Then
        // the processing runs successfully
        ArgumentCaptor<TimeSeriesDataset> timeSeriesDatasetArgumentCaptor = ArgumentCaptor.forClass(TimeSeriesDataset.class);
        verify(reviewedContentWriter, times(1)).writeObject(timeSeriesDatasetArgumentCaptor.capture(), anyString());
        List<DownloadSection> actualDownloadSections = timeSeriesDatasetArgumentCaptor.getValue().getDownloads();
        for (int i=0; i < downloadSections.size(); i++) {
            assertEquals(downloadSections.get(i).getTitle(), actualDownloadSections.get(i).getTitle());
        }
    }

    @Test
    public void process_givenLandingPageWithoutDatasetId_generatesFromCSDBFileName() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // we generate a publish with a fresh csdb upload
        DataPagesSet pagesSet = csdbPageSet;
        pagesSet.datasetLandingPage.getDescription().setDatasetId("");

        DataPublicationDetails publicationDetails = new DataPublicationDetails(pagesSet.timeSeriesDataset, pagesSet.datasetLandingPage, pagesSet.fileUri);
        DataPublication publication = new DataPublication(publicationDetails);
        publication.setDataLink(dataLink);

        // When
        // we process the publish
        publication.process(publishedContentReader, reviewedContentReader, reviewedContentWriter, dataIndex, updateCommands);

        // Then
        // we expect datasetId to be extracted using the [datasetId].csdb pattern
        assertEquals("abcd", publication.getDetails().landingPage.getDescription().getDatasetId());
    }

    @Test
    public void process_givenLandingPageWithoutDatasetId_generatesFromCSVFileName() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // we generate a publish with a fresh csv upload
        DataPagesSet pagesSet = csvPageSet;
        pagesSet.datasetLandingPage.getDescription().setDatasetId("");

        DataPublicationDetails publicationDetails = new DataPublicationDetails(pagesSet.timeSeriesDataset, pagesSet.datasetLandingPage, pagesSet.fileUri);
        DataPublication publication = new DataPublication(publicationDetails);
        publication.setDataLink(dataLink);

        // When
        // we process the publish
        publication.process(publishedContentReader, reviewedContentReader, reviewedContentWriter, dataIndex, updateCommands);

        // Then
        // we expect datasetId to be extracted using the upload-[datasetId].csv pattern
        assertEquals("abcd", publication.getDetails().landingPage.getDescription().getDatasetId());
    }

    @Test
    public void process_givenCSDBFile_callsCSDBDataLink() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // we generate a publish with a csdb upload
        DataPagesSet pagesSet = csdbPageSet;
        pagesSet.datasetLandingPage.getDescription().setDatasetId("");

        DataPublicationDetails publicationDetails = new DataPublicationDetails(pagesSet.timeSeriesDataset, pagesSet.datasetLandingPage, pagesSet.fileUri);
        DataPublication publication = new DataPublication(publicationDetails);
        publication.setDataLink(dataLink);

        // When
        // we process the publish
        publication.process(publishedContentReader, reviewedContentReader, reviewedContentWriter, dataIndex, updateCommands);

        // Then
        // we expect the csdb datalink to be called
        verify(dataLink, times(1)).callCSDBProcessor(anyString(),any(ContentReader.class));
    }

    @Test
    public void process_givenCSVFile_callsCSVDataLink() throws IOException, ParseException, URISyntaxException, ZebedeeException {
        // Given
        // we generate a publish with a csv upload
        DataPagesSet pagesSet = csvPageSet;
        pagesSet.datasetLandingPage.getDescription().setDatasetId("");

        DataPublicationDetails publicationDetails = new DataPublicationDetails(pagesSet.timeSeriesDataset, pagesSet.datasetLandingPage, pagesSet.fileUri);
        DataPublication publication = new DataPublication(publicationDetails);
        publication.setDataLink(dataLink);

        // When
        // we process the publish
        publication.process(publishedContentReader, reviewedContentReader, reviewedContentWriter, dataIndex, updateCommands);

        // Then
        // we expect the csv datalink to be called
        verify(dataLink, times(1)).callCSVProcessor(anyString(),any(ContentReader.class));
    }
}