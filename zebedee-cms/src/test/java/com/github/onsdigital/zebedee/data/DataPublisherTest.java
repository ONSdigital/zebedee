package com.github.onsdigital.zebedee.data;

import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.content.page.base.PageDescription;
import com.github.onsdigital.content.page.base.PageType;
import com.github.onsdigital.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.content.partial.TimeseriesValue;
import com.github.onsdigital.content.util.ContentUtil;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Content;
import org.apache.commons.io.FileUtils;
import org.apache.poi.util.IOUtils;
import org.bouncycastle.util.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 11/06/15.
 */
public class DataPublisherTest {
    Zebedee zebedee;
    Builder bob;
    Session publisher;

    Map<String, String> envVariables;

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

    }
    @After
    public void tearDown() throws Exception {
        bob.delete();
        DataPublisher.env = envVariables;
    }


    /**
     *
     * Check the test is setting itself up as a collection
     * and the files we are using to run the test are current
     *
     */
    @Test
    public void testFramework_afterBeforeMethod_isProperlySetUp() throws IOException {
        // Given
        // we have run the before method

        // We expect
        // Correct setup
        assertEquals(1, zebedee.collections.list().size());

        // The collection exists
        Collection collection = zebedee.collections.list().getCollection("collection");
        assertNotNull(collection);
        // It has four items
        assertEquals(4, collection.reviewedUris().size());
        assertTrue(Files.exists(collection.reviewed.toPath("/themea/landinga/producta/datasets/a4fk_dataset/data.json")));
        assertTrue(Files.exists(collection.reviewed.toPath("/themea/landinga/producta/datasets/a4fk_dataset/BB.csdb")));
        assertTrue(Files.exists(collection.reviewed.toPath("/themea/landinga/producta/datasets/another_dataset/data.json")));
        assertTrue(Files.exists(collection.reviewed.toPath("/themea/landinga/producta/datasets/another_dataset/PPI.csdb")));

        // The pre-existing items exist
        assertTrue(Files.exists(zebedee.published.toPath("/themea/landinga/producta/datasets/a4fk_dataset/data.json")));
        assertTrue(Files.exists(zebedee.published.toPath("/themea/landinga/producta/timeseries/a4fk/data.json")));
    }
    @Test
    public void contentUtil_givenTestData_shouldDeserialise() throws IOException {

        // Given
        // the files in our collection
        Collection collection = zebedee.collections.list().getCollection("collection");
        Path dataset1 = collection.reviewed.toPath("/themea/landinga/producta/datasets/a4fk_dataset/data.json");
        Path dataset2 = collection.reviewed.toPath("/themea/landinga/producta/datasets/another_dataset/data.json");
        Path existingDataset = zebedee.published.toPath("/themea/landinga/producta/datasets/a4fk_dataset/data.json");
        Path existingTimeSeries = zebedee.published.toPath("/themea/landinga/producta/timeseries/a4fk/data.json");

        // Then
        // we expect the files in our collection to deserialise properly
        try(InputStream stream = Files.newInputStream(dataset1)) {
            assertNotNull(ContentUtil.deserialise(stream, Dataset.class));
        }
        try(InputStream stream = Files.newInputStream(dataset2)) {
            assertNotNull(ContentUtil.deserialise(stream, Dataset.class));
        }
        try(InputStream stream = Files.newInputStream(existingDataset)) {
            assertNotNull(ContentUtil.deserialise(stream, Dataset.class));
        }
        try(InputStream stream = Files.newInputStream(existingDataset)) {
            assertNotNull(ContentUtil.deserialise(stream, Dataset.class));
        }

    }


    /**
     * Brian is called using environment variables. Check the function is working and returning an expected result
     */
    @Test
    public void urlForBrian_whenEnvVariablesAreSet_givesURIBasedOnBrianURL() throws Exception {
        // Given
        // we set the env variable
        Map<String, String> envNew = new HashMap<>();
        envNew.put("brian_url", "/csdbURIShouldComeFromEnvVariable");
        DataPublisher.env = envNew;

        // When
        // we get the service csdbURI
        URI uri = DataPublisher.csdbURI();

        // Then
        // we expect a standard response
        assertEquals("/csdbURIShouldComeFromEnvVariable/Services/ConvertCSDB", uri.toString());
    }
    @Test
    public void contentUtil_givenFileReturnedByBrian_shouldDeserialiseTimeSeries() throws IOException {
        // Given
        // a file returned by brian
        String brianPath = "/csdb/csdb_no_extension/brian.json";


        // When
        // we deserialise it
        TimeSerieses serieses = null;
        try (InputStream inputStream = getClass().getResourceAsStream(brianPath)) {
            serieses = ContentUtil.deserialise(inputStream, TimeSerieses.class);
        }

        // Then
        // we expect it to be non null and to have data
        assertNotNull(serieses);
        assertTrue(serieses.size() > 0);


    }

    /**
     * The process starts by hooking into publish and pulling out pages that require being treated as CSDB datasets
     *
     * Check the hooks identify data.json pages that are published alongside .csdb files or blank files with csdb content
     *
     * @throws Exception
     */
    @Test
    public void functionCsdbDatasetsInCollection_givenCollectionsSetupInBefore_shouldIdentifyCSDBdatasets() throws Exception {
        // Given
        // our pre setup collection with csdb extensions
        Collection collection = zebedee.collections.list().getCollection("collection");

        // When
        // we search for csdb datasets with a publisher
        List<HashMap<String, Path>> datasetsInCollection = DataPublisher.csdbDatasetsInCollection(collection, publisher);

        // Then
        // we expect two results
        assertEquals(2, datasetsInCollection.size());
    }

    /**
     * Dataset id is identified from the ?.csdb portion of the filename
     */
    @Test
    public void datasetIdFromDatafilePath_withTypicalFilepaths_givesExpectedIds() throws IOException {
        // Given
        // our pre setup collection with csdb extensions
        Collection collection = zebedee.collections.list().getCollection("collection");
        Path datasetPath = collection.reviewed.toPath("/themea/landinga/producta/datasets/another_dataset/PPI.csdb");

        // When
        // we get a dataset Id from this
        String datasetIdFromDatafilePath = DataPublisher.datasetIdFromDatafilePath(datasetPath);

        // Then
        // we expect the result PPI
        assertEquals("PPI", datasetIdFromDatafilePath);
    }

    @Test
    public void functionUriForSeriesInDataset_givenDatasetsSetupInBefore_ShouldBeCorrect() throws IOException {
        // Given
        // a file returned by brian
        String datasetId = "/theme/product/datasets/1234";
        TimeSeries series = new TimeSeries();
        series.setCdid("ABCD");

        // When
        // we use these to generate a timeseries uri
        String uriForSeriesInDataset = DataPublisher.uriForSeriesInDataset(datasetId, series);

        // Then
        // we expect a uri in the corresponding timeseries folder with CDID as a subfolder
        assertEquals("/theme/product/timeseries/abcd", uriForSeriesInDataset);
    }

    @Test
    public void startPage_GivenFreshTimeSeries_ShouldBeTheSame() throws IOException {

    }
    @Test
    public void startPage_givenExistingTimeSeries_shouldBePopulatedByExistingData() throws IOException {

    }

    @Test
    public void populatePage_withoutExistingTimeSeries_shouldFillEmptySeries() throws IOException {

    }
    @Test
    public void populatePage_overExistingTimeSeries_shouldAddNewPoints() {
        // Given

        // When

        // Then
    }
    @Test
    public void populatePage_overExistingTimeSeries_shouldOverwriteExistingPoints() {

    }

    // Quick
    @Test
    public void constructTimeseriesFromComponents_withDatasetValues_doesCopyToTimeseries() throws URISyntaxException {
        // Given
        // a test dataset

        // When
        // we populate a series

        // Then
        // we expect the data from the dataset to have transferred

    }
    @Test
    public void constructTimeseriesFromComponents_withTimeseriesValues_doesUpdateTimeseriesName() throws URISyntaxException {
        // Given
        // a test dataset with an existing timeseries

        // When
        // we populate a series

        // Then
        // we expect the data from the dataset to have transferred

    }
    @Test
    public void constructTimeseriesFromComponents_withTimeseriesValues_doesNotOverwriteManualUpdates() throws URISyntaxException {
        // Given
        // a test dataset with an existing timeseries

        // When
        // we populate a series

        // Then
        // we expect the data from the dataset to have transferred

    }

}
