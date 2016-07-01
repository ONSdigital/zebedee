package com.github.onsdigital.zebedee.data;

import com.github.davidcarboni.cryptolite.Random;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.importing.TimeseriesUpdateCommand;
import com.github.onsdigital.zebedee.data.processing.DataIndex;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.DummyCollectionReader;
import com.github.onsdigital.zebedee.model.DummyCollectionWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DataPublisherTest {

    @Test
    public void preprocessCollectionShouldApplyUpdateCommands() throws IOException, ZebedeeException, URISyntaxException {

        // given a published file that does not exist in the collection, and an update command for it
        Path collectionDirectory = Files.createTempDirectory(Random.id()); // create a temp directory to generate content into
        Path publishedDirectory = Files.createTempDirectory(Random.id()); // create a temp directory to generate content into

        // readers and writers for published and collection.
        ContentWriter publishedContentWriter = new ContentWriter(publishedDirectory);
        ContentReader publishedContentReader = new FileSystemContentReader(publishedDirectory);
        CollectionReader collectionReader = new DummyCollectionReader(collectionDirectory);
        CollectionWriter collectionWriter = new DummyCollectionWriter(collectionDirectory);


        String expectedTitle = "the updated title";
        String cdid = "abcd";
        String datasetId = "qwef";
        String uri = String.format("/timeseries/%s/%s", cdid, datasetId);

        // create the published time series and write it.
        TimeSeries timeSeries = new TimeSeries();
        timeSeries.setCdid(cdid);
        publishedContentWriter.writeObject(timeSeries, uri + "/data.json");

        List<TimeseriesUpdateCommand> updateCommands = new ArrayList<>();
        updateCommands.add(new TimeseriesUpdateCommand(cdid, datasetId, expectedTitle));

        DataIndex dataIndex = new DataIndex(publishedContentReader);
        dataIndex.pauseUntilComplete(10);

        // when the update command is passed into the pre process collection method.
        DataPublisher dataPublisher = new DataPublisher();
        dataPublisher.preprocessCollection(publishedContentReader, collectionReader, collectionWriter.getReviewed(), false, dataIndex, updateCommands);

        // then the command should be applied and the file should be in the collection with the updated title.
        String actualTitle = collectionReader.getReviewed().getContent(uri).getDescription().getTitle();

        assertEquals(expectedTitle, actualTitle);
    }
}
