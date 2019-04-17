package com.github.onsdigital.zebedee.model.approval.task.timeseries;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.approval.tasks.timeseries.TimeSeriesCompressionTask;
import com.github.onsdigital.zebedee.model.approval.tasks.timeseries.TimeSeriesCompressor;
import com.github.onsdigital.zebedee.model.approval.tasks.timeseries.TimeseriesCompressionResult;
import com.github.onsdigital.zebedee.model.approval.tasks.timeseries.ZipFileVerifier;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeseriesCompressionTaskTest {

    ContentReader contentReader = mock(ContentReader.class);
    ContentWriter contentWriter = mock(ContentWriter.class);
    boolean isEncrypted = false;
    Collection collection = mock(Collection.class);
    private CollectionWriter collectionWriter = mock(CollectionWriter.class);
    private CollectionReader collectionReader = mock(CollectionReader.class);

    private Path timeSeriesDirectoryPath = Paths.get("some/path/timeseries");
    private Path timeSeriesZipPath = Paths.get("some/path/timeseries-to-publish.zip");

    @Before
    public void setUp() throws Exception {

        // mock the properties required of collection.getDescription()
        CollectionDescription collectionDescription = new CollectionDescription();
        collectionDescription.isEncrypted = isEncrypted;
        collectionDescription.setName("test collection");
        when(collection.getDescription()).thenReturn(collectionDescription);

        // provide mock instances for reviewed content reader / writers.
        when(collectionReader.getReviewed()).thenReturn(contentReader);
        when(collectionReader.getRoot()).thenReturn(contentReader);
        when(collectionWriter.getReviewed()).thenReturn(contentWriter);
        when(collectionWriter.getRoot()).thenReturn(contentWriter);
    }

    @Test
    public void shouldReturnTrueIfVerified() throws ZebedeeException, IOException {

        // given a mocked setup of a successful zip file creation.
        List<TimeseriesCompressionResult> zipFiles = createExampleZipFileResults();
        TimeSeriesCompressor timeSeriesCompressor = getMockedCompressor(zipFiles);

        ZipFileVerifier zipFileVerifier = mock(ZipFileVerifier.class);
        when(zipFileVerifier.verifyZipFiles(zipFiles, contentReader, contentReader, contentWriter))
                .thenReturn(new ArrayList<>());

        // When the compress time series task is run.
        TimeSeriesCompressionTask task = new TimeSeriesCompressionTask(timeSeriesCompressor, zipFileVerifier);
        boolean result = task.compressTimeseries(collection, collectionReader, collectionWriter);

        // Then the method returns true with no exceptions thrown
        assertTrue(result);
    }

    @Test
    public void shouldRetryIfFailed() throws ZebedeeException, IOException {

        // given a mocked setup of a failed initial attempt of zip creation the process is retried.
        List<TimeseriesCompressionResult> zipFiles = createExampleZipFileResults();
        TimeSeriesCompressor timeSeriesCompressor = getMockedCompressor(zipFiles);

        ZipFileVerifier zipFileVerifier = mock(ZipFileVerifier.class);
        when(zipFileVerifier.verifyZipFiles(zipFiles, contentReader, contentReader, contentWriter))
                .thenReturn(zipFiles)
                .thenReturn(new ArrayList<>()); // attempt 1

        // When the compress time series task is run.
        TimeSeriesCompressionTask task = new TimeSeriesCompressionTask(timeSeriesCompressor, zipFileVerifier);
        boolean result = task.compressTimeseries(collection, collectionReader, collectionWriter);

        // Then the method returns true
        assertTrue(result);
    }

    @Test
    public void shouldReturnFalseAfterFiveFailedAttempts() throws ZebedeeException, IOException {

        // given a mocked setup of a 5 failed verifications.
        List<TimeseriesCompressionResult> zipFiles = createExampleZipFileResults();
        TimeSeriesCompressor timeSeriesCompressor = getMockedCompressor(zipFiles);

        ZipFileVerifier zipFileVerifier = mock(ZipFileVerifier.class);
        when(zipFileVerifier.verifyZipFiles(zipFiles, contentReader, contentReader, contentWriter))
                .thenReturn(zipFiles);

        // When the compress time series task is run.
        TimeSeriesCompressionTask task = new TimeSeriesCompressionTask(timeSeriesCompressor, zipFileVerifier);
        boolean result = task.compressTimeseries(collection, collectionReader, collectionWriter);

        // Then the method returns false
        assertFalse(result);
    }

    @Test
    public void shouldReturnTrueIfFifthAttemptSucceeds() throws ZebedeeException, IOException {
        List<TimeseriesCompressionResult> zipFiles = createExampleZipFileResults();
        TimeSeriesCompressor timeSeriesCompressor = getMockedCompressor(zipFiles);

        ZipFileVerifier zipFileVerifier = mock(ZipFileVerifier.class);
        when(zipFileVerifier.verifyZipFiles(zipFiles, contentReader, contentReader, contentWriter))
                .thenReturn(zipFiles) // attempt 1
                .thenReturn(zipFiles) // attempt 2
                .thenReturn(zipFiles) // attempt 3
                .thenReturn(zipFiles) // attempt 4
                .thenReturn(new ArrayList<>());  // attempt 5

        // When the compress time series task is run.
        TimeSeriesCompressionTask task = new TimeSeriesCompressionTask(timeSeriesCompressor, zipFileVerifier);
        boolean result = task.compressTimeseries(collection, collectionReader, collectionWriter);

        // Then the method returns true
        assertTrue(result);
    }

    public TimeSeriesCompressor getMockedCompressor(List<TimeseriesCompressionResult> zipFiles) throws ZebedeeException, IOException {
        TimeSeriesCompressor timeSeriesCompressor = mock(TimeSeriesCompressor.class);
        when(timeSeriesCompressor.compressFiles(contentReader, contentWriter, isEncrypted))
                .thenReturn(zipFiles); // first attempt
        when(timeSeriesCompressor.compressFiles(contentReader, contentWriter, isEncrypted, zipFiles))
                .thenReturn(zipFiles); // attempt 2+
        return timeSeriesCompressor;
    }


    public List<TimeseriesCompressionResult> createExampleZipFileResults() {
        List<TimeseriesCompressionResult> zipFiles = new ArrayList<>();
        zipFiles.add(new TimeseriesCompressionResult(timeSeriesDirectoryPath, timeSeriesZipPath, 1234));
        return zipFiles;
    }
}
