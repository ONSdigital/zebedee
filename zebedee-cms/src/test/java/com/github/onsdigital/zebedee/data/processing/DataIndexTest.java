package com.github.onsdigital.zebedee.data.processing;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.framework.DataPagesGenerator;
import com.github.onsdigital.zebedee.data.framework.DataPagesSet;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DataIndexTest {

    private Path path;
    private ContentReader publishedReader;

    /**
     * Setup common test data and mocks.
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        path = Files.createTempDirectory(Random.id());
        Path publishedPath = path.resolve("zebedee/master");
        publishedReader = new FileSystemContentReader(publishedPath);

        // Unable to mock reader config as DataIndex uses a thread pool to reindex. Initialise the real thing instead.
        ReaderConfiguration.init(path.toString());

        // add a set of data to published
        DataPagesGenerator generator = new DataPagesGenerator();
        DataPagesSet published = generator.generateDataPagesSet("dataprocessor", "published", 2015, 2, "");
        publishDataPagesSet(publishedPath, published);
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deleteDirectory(path.toFile());
    }

    @Test
    public void dataIndex_givenContent_buildsIndex() throws BadRequestException {
        // Given
        // content
        ContentReader contentReader = publishedReader;

        // When
        // we build a DataIndex
        DataIndex dataIndexTest = new DataIndex(contentReader);
        dataIndexTest.pauseUntilComplete(60);

        // Then
        // indexing should complete with the published timeseries referenced
        assertFalse(dataIndexTest.cdids().isEmpty());
        assertEquals(2, dataIndexTest.cdids().size());
    }

    /**
     * @param dataPagesSet a generated set of pages for the data publisher
     * @throws IOException
     * @throws BadRequestException
     */
    public void publishDataPagesSet(Path publishedPath, DataPagesSet dataPagesSet) throws IOException, BadRequestException {
        publishPage(publishedPath, dataPagesSet.datasetLandingPage, dataPagesSet.datasetLandingPage.getUri().toString());
        publishPage(publishedPath, dataPagesSet.timeSeriesDataset, dataPagesSet.timeSeriesDataset.getUri().toString());

        // timeseries in the old format
        for (TimeSeries timeSeries : dataPagesSet.timeSeriesList) {
            publishPage(publishedPath, timeSeries, timeSeries.getUri().toString());
        }
    }

    /**
     * Publish a page object
     *
     * @param page any zebedee page
     * @param uri  the uri to publish to
     * @throws IOException
     * @throws BadRequestException
     */
    private void publishPage(Path publishedPath, Page page, String uri) throws IOException, BadRequestException {
        String publishTo = uri;
        if (publishTo.startsWith("/"))
            publishTo = publishTo.substring(1);
        ContentWriter writer = new ContentWriter(publishedPath);

        writer.writeObject(page, publishTo + "/data.json");
    }
}