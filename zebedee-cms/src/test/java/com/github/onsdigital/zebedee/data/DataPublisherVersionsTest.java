package com.github.onsdigital.zebedee.data;

import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.junit.After;
import org.junit.Before;

import java.io.InputStream;
import java.nio.file.Files;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 03/11/15.
 */
public class DataPublisherVersionsTest {

    Zebedee zebedee;
    Builder bob;
    Session publisher;
    Collection collection;

    String publishedDatasetPath;

    TimeSerieses correctionSeries = null;
    TimeSerieses newDatapointSeries = null;

    DataPublisher dataPublisher = new DataPublisher();

    /**
     * The bootstrap resource contains master data and one collection "collection"
     *
     * "collection" contains two reviewed datasets
     *
     * i) /themea/landinga/producta/datasets/a4fk_dataset - this has existing data (the a4fk timeseries)
     * ii) /themea/landinga/producta/datasets/another_dataset - this has no existing data in master
     *
     * i) is a cut down version of Blue Book. ii) is a cut down version of PPI
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        // Loads a zebedee with two collections, each of which contain a dataset
        //
        bob = new Builder(DataPublisherTest.class, ResourceUtils.getPath("/bootstraps/data_publisher"));
        zebedee = new Zebedee(bob.zebedee);
        publisher = bob.createSession(bob.publisher1);

        collection = zebedee.collections.list().getCollection("collection");

        String brianPath = "/csdb/csdb_no_extension/brian.json";
        try (InputStream inputStream = getClass().getResourceAsStream(brianPath)) {
            correctionSeries = ContentUtil.deserialise(inputStream, TimeSerieses.class);
        }

    }

    @After
    public void tearDown() throws Exception {
        bob.delete();
    }
}