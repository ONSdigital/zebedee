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
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TimeseriesCompressionTaskTest {

    ContentReader contentReader = mock(ContentReader.class);
    ContentWriter contentWriter = mock(ContentWriter.class);
    boolean isEncrypted = false;
    Collection collection = mock(Collection.class);
    private CollectionWriter collectionWriter = mock(CollectionWriter.class);
    private CollectionReader collectionReader = mock(CollectionReader.class);

    @Before
    public void setUp() throws Exception {

        // mock the properties required of collection.getDescription()
        CollectionDescription collectionDescription = new CollectionDescription();
        collectionDescription.isEncrypted = isEncrypted;
        collectionDescription.name = "test collection";
        when(collection.getDescription()).thenReturn(collectionDescription);

        // provide mock instances for reviewed content reader / writers.
        when(collectionReader.getReviewed()).thenReturn(contentReader);
        when(collectionWriter.getReviewed()).thenReturn(contentWriter);
    }

    @Test
    public void shouldReturnIfVerified() throws ZebedeeException, IOException {

        // given a mocked setup of a successful zip file creation.
        List<TimeseriesCompressionResult> zipFiles = new ArrayList<>();
        zipFiles.add(new TimeseriesCompressionResult("some/path", 1234));

        TimeSeriesCompressor timeSeriesCompressor = mock(TimeSeriesCompressor.class);
        when(timeSeriesCompressor.compressFiles(contentReader, contentWriter, isEncrypted))
                .thenReturn(zipFiles);

        ZipFileVerifier zipFileVerifier = mock(ZipFileVerifier.class);
        when(zipFileVerifier.verifyZipFiles(zipFiles, contentReader, contentReader, contentWriter))
                .thenReturn(zipFiles);

        // When the compress time series task is run.
        TimeSeriesCompressionTask task = new TimeSeriesCompressionTask(timeSeriesCompressor, zipFileVerifier);
        task.compressTimeseries(collection, collectionReader, collectionWriter);

        // Then the method returns with no exceptions thrown
    }
}
