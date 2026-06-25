package com.github.onsdigital.zebedee.data.processing;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.item.ContentItemVersion;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.reader.ContentReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DataWriterTest {

    @Mock
    private ContentReader publishedContentReader;

    @Mock
    private ContentReader reviewedContentReader;

    @Mock
    private ContentWriter reviewedContentWriter;

    @Mock
    private DataProcessor dataProcessor;

    private MockedConstruction<VersionedContentItem> versionedContentItemMock;

    private DataPagesSet pageSet;
    private DataPublicationDetails publicationDetails;
    private TimeSeries timeSeries;

    /**
     * Setup common test data and mocks.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Generate example sets of published and unpublished data pages
        DataPagesGenerator generator = new DataPagesGenerator();
        pageSet = generator.generateDataPagesSet("dataprocessor", "inreview", 2015, 2, "");
        pageSet.timeSeriesDataset.getDescription().setReleaseDate(new Date());

        Path rootPath = Files.createTempDirectory(Random.id());
        when(reviewedContentReader.getRootFolder()).thenReturn(rootPath);

        publicationDetails = new DataPublicationDetails(pageSet.timeSeriesDataset, pageSet.datasetLandingPage, pageSet.fileUri);
        timeSeries = pageSet.timeSeriesList.get(0);
        when(dataProcessor.getTimeSeries()).thenReturn(timeSeries);
    }

    @After
    public void tearDown() {
        if (versionedContentItemMock != null) {
            versionedContentItemMock.close();
        }
    }

    @Test
    public void versionAndSave_forBrandNewData_doesNotVersion() throws ZebedeeException, IOException {
        // Given
        // A completed processor for a brand new timeseries (i.e. no corrections and all data points are new insertions)
        when(publishedContentReader.getContent(timeSeries.getUri().toString())).thenThrow(new IOException("file not found"));
        when(dataProcessor.getCorrections()).thenReturn(0);
        when(dataProcessor.getInsertions()).thenReturn(10);

        // Mock the VersionedContentItem that gets instantiated inside the DataWriter code as it is tangled with the file
        // system content access which is not in the scope of these tests
        versionedContentItemMock = Mockito.mockConstruction(VersionedContentItem.class, (mock, context) -> {});

        // When
        // we version and save
        DataWriter dataWriter = new DataWriter(reviewedContentWriter, reviewedContentReader, publishedContentReader);
        dataWriter.versionAndSave(dataProcessor, publicationDetails);

        // Then
        // the new file should save
        ArgumentCaptor<TimeSeries> argCaptor = ArgumentCaptor.forClass(TimeSeries.class);
        verify(reviewedContentWriter, times(1)).writeObject(argCaptor.capture(), anyString());
        TimeSeries writtenTimeseries = argCaptor.getValue();

        assertEquals(timeSeries.getUri(), writtenTimeseries.getUri());

        // And
        // the time series should not be versioned
        assertTrue(versionedContentItemMock.constructed().isEmpty());
        assertNull(writtenTimeseries.getVersions());
    }

    @Test
    public void versionAndSave_forIdenticalData_shouldNotVersion() throws ZebedeeException, IOException {
        // Given
        // we republish a published timeseries with identical values (i.e. no corrections or insertions)
        when(publishedContentReader.getContent(timeSeries.getUri().toString())).thenReturn(timeSeries);
        when(dataProcessor.getCorrections()).thenReturn(0);
        when(dataProcessor.getInsertions()).thenReturn(0);

        // Mock the VersionedContentItem that gets instantiated inside the DataWriter code as it is tangled with the file
        // system content access which is not in the scope of these tests
        versionedContentItemMock = Mockito.mockConstruction(VersionedContentItem.class, (mock, context) -> {});

        // When
        // we version and save
        DataWriter dataWriter = new DataWriter(reviewedContentWriter, reviewedContentReader, publishedContentReader);
        dataWriter.versionAndSave(dataProcessor, publicationDetails);

        // Then
        // the new file should save
        ArgumentCaptor<TimeSeries> argCaptor = ArgumentCaptor.forClass(TimeSeries.class);
        verify(reviewedContentWriter, times(1)).writeObject(argCaptor.capture(), anyString());
        TimeSeries writtenTimeseries = argCaptor.getValue();

        assertEquals(timeSeries.getUri(), writtenTimeseries.getUri());

        // And
        // the time series should not be versioned
        assertTrue(versionedContentItemMock.constructed().isEmpty());
        assertNull(writtenTimeseries.getVersions());
    }

    @Test
    public void versionAndSave_forAppendedData_shouldVersion() throws ZebedeeException, IOException {
        // Given
        // we republish a published timeseries to append new data (i.e. no corrections and all data points are new insertions)
        when(publishedContentReader.getContent(timeSeries.getUri().toString())).thenReturn(timeSeries);
        when(dataProcessor.getCorrections()).thenReturn(0);
        when(dataProcessor.getInsertions()).thenReturn(10);

        // Mock the VersionedContentItem that gets instantiated inside the DataWriter code as it is tangled with the file
        // system content access which is not in the scope of these tests
        String versionIdentifier = "v1";
        versionedContentItemMock = Mockito.mockConstruction(VersionedContentItem.class, (mock, context) -> {
            String uri = (String) context.arguments().get(0);
            String versionUri = String.format("%s/%s/%s", uri, VersionedContentItem.getVersionDirectoryName(), versionIdentifier);

            ContentItemVersion contentItemVersion = new ContentItemVersion(versionIdentifier, mock, versionUri);

            // And given version not yet created in collection
            when(mock.versionExists(any(ContentReader.class))).thenReturn(false);

            // Then when versioning create v1
            when(mock.createVersion(any(ContentReader.class), any(ContentWriter.class))).thenReturn(contentItemVersion);
        });

        // When
        // we version and save
        DataWriter dataWriter = new DataWriter(reviewedContentWriter, reviewedContentReader, publishedContentReader);
        dataWriter.versionAndSave(dataProcessor, publicationDetails);

        // Then
        // the new file should save
        ArgumentCaptor<TimeSeries> argCaptor = ArgumentCaptor.forClass(TimeSeries.class);
        verify(reviewedContentWriter, times(1)).writeObject(argCaptor.capture(), anyString());
        TimeSeries writtenTimeseries = argCaptor.getValue();

        // And
        // the time series should have one version
        assertEquals(timeSeries.getUri(), writtenTimeseries.getUri());
        assertEquals(1, writtenTimeseries.getVersions().size());
        assertEquals(versionIdentifier, writtenTimeseries.getVersions().get(0).getLabel());
        assertEquals(pageSet.datasetLandingPage.getDescription().getReleaseDate(), writtenTimeseries.getVersions().get(0).getUpdateDate());
    }

    @Test
    public void versionAndSave_forUpdatedTimeSeriesWithCorrection_shouldVersionWithCorrectDetails() throws ZebedeeException, IOException, URISyntaxException, ParseException {
        // Given
        // an updated timeseries with known version information in the dataset
        when(publishedContentReader.getContent(timeSeries.getUri().toString())).thenReturn(timeSeries);
        when(dataProcessor.getCorrections()).thenReturn(2);
        when(dataProcessor.getInsertions()).thenReturn(0);

        Version version = new Version();
        version.setUpdateDate(new Date());
        version.setCorrectionNotice("correction notice");
        publicationDetails.datasetPage.setVersions(new ArrayList<>());
        publicationDetails.datasetPage.getVersions().add(version);

        // Mock the VersionedContentItem that gets instantiated inside the DataWriter code as it is tangled with the file
        // system content access which is not in the scope of these tests
        String versionIdentifier = "v2";
        versionedContentItemMock = Mockito.mockConstruction(VersionedContentItem.class, (mock, context) -> {
            String uri = (String) context.arguments().get(0);
            String versionUri = String.format("%s/%s/%s", uri, VersionedContentItem.getVersionDirectoryName(), versionIdentifier);

            ContentItemVersion contentItemVersion = new ContentItemVersion(versionIdentifier, mock, versionUri);

            // And given version not yet created in collection
            when(mock.versionExists(any(ContentReader.class))).thenReturn(false);

            // Then when versioning create v1
            when(mock.createVersion(any(ContentReader.class), any(ContentWriter.class))).thenReturn(contentItemVersion);
        });

        // When
        // we version and save
        DataWriter dataWriter = new DataWriter(reviewedContentWriter, reviewedContentReader, publishedContentReader);
        dataWriter.versionAndSave(dataProcessor, publicationDetails);

        // Then
        // the new file should save
        ArgumentCaptor<TimeSeries> argCaptor = ArgumentCaptor.forClass(TimeSeries.class);
        verify(reviewedContentWriter, times(1)).writeObject(argCaptor.capture(), anyString());
        TimeSeries writtenTimeseries = argCaptor.getValue();

        // And
        // the time series should have one version
        assertEquals(timeSeries.getUri(), writtenTimeseries.getUri());
        assertEquals(1, writtenTimeseries.getVersions().size());
        assertEquals(versionIdentifier, writtenTimeseries.getVersions().get(0).getLabel());
        assertEquals(version.getUpdateDate(), writtenTimeseries.getVersions().get(0).getUpdateDate());
        assertEquals(version.getCorrectionNotice(), writtenTimeseries.getVersions().get(0).getCorrectionNotice());
    }
}