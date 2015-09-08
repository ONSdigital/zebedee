package com.github.onsdigital.zebedee.data;

import com.github.davidcarboni.ResourceUtils;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.partial.Contact;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;

/**
 * Created by thomasridd on 11/06/15.
 */
public class DataPublisherTest {
    Zebedee zebedee;
    Builder bob;
    Session publisher;
    Collection collection;

    String publishedTimeSeriesPath;
    String publishedDatasetPath;

    String unpublishedTimeSeriesPath;
    String unpublishedDatasetPath;

    TimeSerieses serieses = null;
    TimeSeries publishedTimeSeries = null;
    TimeSeries unpublishedTimeSeries = null;
    Dataset publishedDataset = null;
    Dataset unpublishedDataset = null;

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

        collection = zebedee.collections.list().getCollection("collection");

        publishedTimeSeriesPath = "/themea/landinga/producta/timeseries/a4fk";
        publishedDatasetPath = "/themea/landinga/producta/datasets/a4fk_dataset";

        unpublishedTimeSeriesPath = "/themea/landinga/producta/timeseries/ju5c";
        unpublishedDatasetPath = "/themea/landinga/producta/datasets/another_dataset";

        String brianPath = "/csdb/csdb_no_extension/brian.json";
        try (InputStream inputStream = getClass().getResourceAsStream(brianPath)) {
            serieses = ContentUtil.deserialise(inputStream, TimeSerieses.class);
        }
        publishedTimeSeries = serieses.get(0); // AF4K
        unpublishedTimeSeries = serieses.get(1); // JU5C

        assertEquals("AF4K", publishedTimeSeries.getCdid());
        assertEquals("JU5C", unpublishedTimeSeries.getCdid());

        try (InputStream inputStream = Files.newInputStream(collection.reviewed.get(publishedDatasetPath).resolve("data.json"))) {
            publishedDataset = ContentUtil.deserialise(inputStream, Dataset.class);
        }
        try (InputStream inputStream = Files.newInputStream(collection.reviewed.get(unpublishedDatasetPath).resolve("data.json"))) {
            unpublishedDataset = ContentUtil.deserialise(inputStream, Dataset.class);
        }

        assertNotNull(publishedDataset);
        assertNotNull(unpublishedDataset);
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
    public void startPage_givenFreshTimeSeries_shouldBeMinimal() throws IOException {
        // Given
        // the time series that hadn't previously been published

        // When
        // we generate a start page
        TimeSeries startPage = DataPublisher.startPageForSeriesWithPublishedPath(zebedee, unpublishedTimeSeriesPath, unpublishedTimeSeries);

        // Then
        // we expect the startPage to be identical to the unpublishedTimeSeries
        assertEquals(0, startPage.years.size() + startPage.months.size() + startPage.quarters.size());
    }


    @Test
    public void startPage_givenExistingTimeSeries_shouldBePopulatedByExistingData() throws IOException {
        // Given
        // the time series that hadn't previously been published
        TimeSeries alreadyPublished = null;
        try (InputStream inputStream = Files.newInputStream(zebedee.published.get(publishedTimeSeriesPath).resolve("data.json"))) {
            alreadyPublished = ContentUtil.deserialise(inputStream, TimeSeries.class);
        }
        assertNotNull(alreadyPublished);

        // When
        // we generate a start page to build
        TimeSeries startPage = DataPublisher.startPageForSeriesWithPublishedPath(zebedee, publishedTimeSeriesPath, publishedTimeSeries);

        // Then
        // we expect the startPage to be based on the alreadyPublished timeseries
        assertEquals(ContentUtil.serialise(alreadyPublished), ContentUtil.serialise(startPage));
    }


    @Test
    public void populatePageValues_withoutExistingTimeSeries_shouldFillEmptySeries() throws IOException {
        // Given
        // the page we have started building that didn't previously exist
        TimeSeries startPage = DataPublisher.startPageForSeriesWithPublishedPath(zebedee, unpublishedTimeSeriesPath, unpublishedTimeSeries);

        // When
        // we generate a start page
        DataPublisher.populatePageFromSetOfValues(startPage, startPage.years, unpublishedTimeSeries.years, unpublishedDataset);

        // Then
        // we expect the startPage to be
        assertEquals(unpublishedTimeSeries.years.size(), startPage.years.size());
    }

    @Test
    public void populatePageValues_overExistingTimeSeries_shouldAddNewPoints() throws IOException {
        // Given
        // the page we have started building that didn't previously exist
        TimeSeries startPage = DataPublisher.startPageForSeriesWithPublishedPath(zebedee, publishedTimeSeriesPath, publishedTimeSeries);
        simplifyTimeSeries(startPage, publishedTimeSeries);

        int originalYears = startPage.years.size();
        assertTrue(originalYears > 0);

        // When
        // we generate a start page
        DataPublisher.populatePageFromSetOfValues(startPage, startPage.years, publishedTimeSeries.years, publishedDataset);

        // Then
        // we expect the startPage to be added to
        assertTrue(originalYears < startPage.years.size());
    }
    @Test
    public void populatePageValues_overExistingTimeSeries_shouldOverwriteExistingPoints() throws IOException {
        // Given
        // the time series we are using as examples cleared with some simple data
        TimeSeries startPage = DataPublisher.startPageForSeriesWithPublishedPath(zebedee, publishedTimeSeriesPath, publishedTimeSeries);
        TimeSeries publishThisPage = publishedTimeSeries;
        Dataset publishThisDataset = publishedDataset;

        simplifyTimeSeries(startPage, publishThisPage);

        // When
        // we combine the two series
        DataPublisher.populatePageFromSetOfValues(startPage, startPage.years, publishThisPage.years, publishThisDataset);

        // Then
        // we expect equal values for
        assertEquals(4, startPage.years.size());
        assertEquals("4", valueForTime("2002", startPage).value);
    }
    @Test
    public void populatePageValues_overExistingTimeSeries_doesNotRemoveExistingPoints() throws IOException {
        // Given
        // the time series we are using as examples cleared with some simple data
        TimeSeries startPage = DataPublisher.startPageForSeriesWithPublishedPath(zebedee, publishedTimeSeriesPath, publishedTimeSeries);
        TimeSeries publishThisPage = publishedTimeSeries;
        Dataset publishThisDataset = publishedDataset;

        simplifyTimeSeries(startPage, publishThisPage);

        // When
        // we combine the two series
        DataPublisher.populatePageFromSetOfValues(startPage, startPage.years, publishThisPage.years, publishThisDataset);

        // Then
        // we expect the value for "2001" (not present in publishedTimeSeries)
        assertEquals(4, startPage.years.size());
        assertEquals("2", valueForTime("2001", startPage).value);
    }

    @Test
    public void populatePageFromTimeSeries_givenFreshTimeSeries_shouldTransferDetails() throws IOException {
        // Given
        // the time series that hadn't previously been published (
        TimeSeries startPage = DataPublisher.startPageForSeriesWithPublishedPath(zebedee, unpublishedTimeSeriesPath, unpublishedTimeSeries);
        TimeSeries publishThisPage = unpublishedTimeSeries;
        Dataset publishThisDataset = unpublishedDataset;

        // When
        // we populate the page
        TimeSeries newPage = DataPublisher.populatePageFromTimeSeries(startPage, publishThisPage, publishThisDataset);

        // Then
        // we expect the newPage to have copied details from the
        assertEquals(newPage.getCdid(), publishThisPage.getCdid());
        assertEquals(newPage.getDescription().getTitle(), publishThisPage.getDescription().getTitle());
        assertEquals(newPage.getDescription().getSeasonalAdjustment(), publishThisPage.getDescription().getSeasonalAdjustment());
    }
    @Test
    public void populatePageFromTimeseries_overExistingTimeSeries_shouldTransferNameFromTimeSeriesIfNameBlank() throws IOException {
        // Given
        // the time series that hadn't previously been published (
        TimeSeries startPage = DataPublisher.startPageForSeriesWithPublishedPath(zebedee, unpublishedTimeSeriesPath, unpublishedTimeSeries);
        TimeSeries publishThisPage = publishedTimeSeries;
        Dataset publishThisDataset = publishedDataset;

        publishThisPage.getDescription().setTitle("Title");
        startPage.getDescription().setTitle("");

        // When
        // we populate the page
        TimeSeries newPage = DataPublisher.populatePageFromTimeSeries(startPage, publishThisPage, publishThisDataset);

        // Then
        // we expect the newPage to copy across the name
        assertEquals("Title", newPage.getDescription().getTitle());
    }
    @Test
    public void populatePageFromTimeseries_overExistingTimeSeries_shouldNotTransferNameIfNameExists() throws IOException {
        // Given
        // the time series that hadn't previously been published (
        TimeSeries startPage = DataPublisher.startPageForSeriesWithPublishedPath(zebedee, unpublishedTimeSeriesPath, unpublishedTimeSeries);
        TimeSeries publishThisPage = publishedTimeSeries;
        Dataset publishThisDataset = publishedDataset;

        publishThisPage.getDescription().setTitle("New Title");
        startPage.getDescription().setTitle("Existing Title");

        // When
        // we populate the page
        TimeSeries newPage = DataPublisher.populatePageFromTimeSeries(startPage, publishThisPage, publishThisDataset);

        // Then
        // we expect the newPage to not copy over the name
        assertEquals("Existing Title", newPage.getDescription().getTitle());
    }
    @Test
    public void populatePageFromTimeseries_overExistingTimeSeries_shouldTransferDetails() throws IOException {
        // Given
        // the time series that hadn't previously been published (
        TimeSeries startPage = DataPublisher.startPageForSeriesWithPublishedPath(zebedee, unpublishedTimeSeriesPath, unpublishedTimeSeries);
        TimeSeries publishThisPage = publishedTimeSeries;
        Dataset publishThisDataset = publishedDataset;

        publishThisPage.getDescription().setSeasonalAdjustment("SA to publish");
        startPage.getDescription().setSeasonalAdjustment("SA existing");

        // When
        // we populate the page
        TimeSeries newPage = DataPublisher.populatePageFromTimeSeries(startPage, publishThisPage, publishThisDataset);

        // Then
        // we expect the newPage to have copied limited details from the published page
        assertEquals(publishThisPage.getDescription().getSeasonalAdjustment(), newPage.getDescription().getSeasonalAdjustment());
        assertEquals(publishThisPage.getCdid(), newPage.getCdid());
    }

    private void simplifyTimeSeries(TimeSeries series1, TimeSeries series2) {
        // Expected values for layering series2 over series1 are
        //[{"2000": "1"}, {"2001": "2"}, {"2002": "4"}, {"2003": "5"}]

        series1.years = new TreeSet<>();
        series1.years.add(quickTimeSeriesValue("2000", "1"));
        series1.years.add(quickTimeSeriesValue("2001", "2"));
        series1.years.add(quickTimeSeriesValue("2002", "3"));
        series1.getDescription().setSeasonalAdjustment("YES");
        series1.getDescription().setTitle("Series1");

        series2.years = new TreeSet<>();
        series2.years.add(quickTimeSeriesValue("2000", "1"));
        series2.years.add(quickTimeSeriesValue("2002", "4"));
        series2.years.add(quickTimeSeriesValue("2003", "5"));
        series2.getDescription().setSeasonalAdjustment("NO");
        series2.getDescription().setTitle("Series2");

    }
    private TimeSeriesValue quickTimeSeriesValue(String year, String value) {
        TimeSeriesValue timeseriesValue = new TimeSeriesValue(); timeseriesValue.year = year; timeseriesValue.value = value;
        timeseriesValue.date = year;
        return timeseriesValue;
    }
    private TimeSeriesValue valueForTime(String year, TimeSeries series) {
        for (TimeSeriesValue value: series.years) {
            if (value.year.equals(year)) {
                return value;
            }
        }
        return null;
    }

    @Test
    public void populatePageFromDataset_withSampleDataset_doesCopyDatesAndContactToTimeseries() throws IOException, URISyntaxException, ParseException {
        // Given
        // a test dataset with some specified test values
        TimeSeries startPage = DataPublisher.startPageForSeriesWithPublishedPath(zebedee, unpublishedTimeSeriesPath, unpublishedTimeSeries);
        Dataset publishThisDataset = publishedDataset;
        String publishThisUri = publishedDatasetPath;

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");
        publishThisDataset.getDescription().setReleaseDate(formatter.parse("7-Jun-2013"));
        publishThisDataset.getDescription().setNextRelease("7-Jun-2014");
        Contact contact = new Contact();
        contact.setEmail("email@email.com");
        contact.setName("user");
        contact.setTelephone("(+44)20-8123-4567");
        publishThisDataset.getDescription().setContact(contact);

        // When
        // we populate a series
        TimeSeries newSeries = DataPublisher.populatePageFromDataSetPage(startPage, publishThisDataset, publishThisUri);

        // Then
        // we expect the data from the dataset to have transferred
        assertEquals(publishThisDataset.getDescription().getReleaseDate(), newSeries.getDescription().getReleaseDate());
        assertEquals(publishThisDataset.getDescription().getNextRelease(), newSeries.getDescription().getNextRelease());
        assertEquals(publishThisDataset.getDescription().getContact().getName(), newSeries.getDescription().getContact().getName());
        assertEquals(publishThisDataset.getDescription().getContact().getEmail(), newSeries.getDescription().getContact().getEmail());
        assertEquals(publishThisDataset.getDescription().getContact().getTelephone(), newSeries.getDescription().getContact().getTelephone());
    }

    @Test
    public void populatePageFromDataset_withSampleDataset_doesCopyDatasetIdToTimeseries() throws IOException, URISyntaxException, ParseException {
        // Given
        // a test dataset with some specified test values
        TimeSeries startPage = DataPublisher.startPageForSeriesWithPublishedPath(zebedee, unpublishedTimeSeriesPath, unpublishedTimeSeries);
        Dataset publishThisDataset = publishedDataset;
        String publishThisUri = publishedDatasetPath;
        publishThisDataset.getDescription().setDatasetId("Test DatasetID");

        // When
        // we populate a series
        TimeSeries newSeries = DataPublisher.populatePageFromDataSetPage(startPage, publishThisDataset, publishThisUri);

        // Then
        // we expect the id from the dataset to be contained in the timeseries source datasets
        boolean datasetIdFound = false;
        for (String datasetId: newSeries.sourceDatasets) {
            if (datasetId.equalsIgnoreCase("Test DatasetID")) {
                datasetIdFound = true;
                break;
            }
        }
        assertTrue(datasetIdFound);
    }


    @Test
    public void generateDataFiles_givenPaths_generatesListOfTimeSeries() {
        // Given
        // some time series
        List<Path> serieses = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            serieses.add(bob.randomWalkTimeSeries("Series " + i));
        }

        // When
        // we get the time series
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // Then
        // We expect the timeSerieses to be populated
        assertNotNull(timeSerieses);
        for (TimeSeries timeSeries : timeSerieses) {
            assertNotNull(timeSeries);
        }
    }

    @Test
    public void generateDataFiles_givenPaths_generatesPopulatedTimeseries() {
        // Given
        // some time series
        List<Path> serieses = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            serieses.add(bob.randomWalkTimeSeries("Series " + i));
        }

        // When
        // we get the time series
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // Then
        // we expect time series values
        for (TimeSeries timeSeries: timeSerieses) {
            assertNotEquals(0, timeSeries.years.size());
            assertNotEquals(0, timeSeries.quarters.size());
            assertNotEquals(0, timeSeries.months.size());
        }
    }

    @Test
    public void timeSeriesFromPathList_givenPaths_retrievesCorrectSeries() {
        // Given
        // some time series
        List<Path> serieses = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            serieses.add(bob.randomWalkTimeSeries("Series " + i));
        }

        // When
        // we get the time series
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // Then
        // we expect time series values
        for (TimeSeries series: timeSerieses) {
            assertThat(series.getDescription().getTitle(), containsString("Series"));
        }
    }

    @Test
    public void timeSeriesIdList_givenMixedListOfPaths_returnsInOriginalOrder() {
        // Given
        // some time series we create and then retrieve
        List<Path> serieses = new ArrayList<>();
        serieses.add(bob.randomWalkTimeSeries("3"));
        serieses.add(bob.randomWalkTimeSeries("2"));
        serieses.add(bob.randomWalkTimeSeries("7"));
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // When
        // we use timeSeriesIdList to pull out Ids
        List<String> ids = DataPublisher.timeSeriesIdList(timeSerieses);

        // Then
        // we expect the ids we got originally in original order
        assertThat(ids, Matchers.contains("3", "2", "7"));
    }

    @Test
    public void mapOfAllDataInTimeSeries_givenListOfPaths_returnsExpectedResults() {
        // Given
        // some time series we create and then retrieve
        List<Path> serieses = new ArrayList<>();
        serieses.add(bob.randomWalkTimeSeries("3"));
        serieses.add(bob.randomWalkTimeSeries("2"));
        serieses.add(bob.randomWalkTimeSeries("7"));
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // When
        // we use timeSeriesIdList to pull out Ids
        Map<String, Map<String, String>> data = DataPublisher.mapOfAllDataInTimeSeriesList(timeSerieses);

        // Then
        // we expect the map to contain all information
        for (TimeSeries series : timeSerieses) {
            for (TimeSeriesValue value : series.years) {
                assertEquals(value.value, data.get(value.date).get(series.getCdid()));
            }
            for (TimeSeriesValue value : series.quarters) {
                assertEquals(value.value, data.get(value.date).get(series.getCdid()));
            }
            for (TimeSeriesValue value : series.months) {
                assertEquals(value.value, data.get(value.date).get(series.getCdid()));
            }
        }
    }

    @Test
    public void yearRange_givenSingleTimeSeries_doesReturnStartAndEndYears() {
        // Given
        // a timeseries
        List<Path> serieses = new ArrayList<>();
        serieses.add(bob.randomWalkTimeSeries("Random", true, false, false, 3, 2015));
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // When
        // we get the year range
        List<String> yearRange = DataPublisher.yearRange(timeSerieses);

        // Then
        // we expect a three year range
        assertThat(yearRange, Matchers.contains("2013", "2014", "2015"));
    }


    @Test
    public void yearRange_givenOverlappingTimeSeries_givesUnionRange() {
        // Given
        // two timeseries that overlap
        List<Path> serieses = new ArrayList<>();
        serieses.add(bob.randomWalkTimeSeries("Random", true, false, false, 3, 2015));
        serieses.add(bob.randomWalkTimeSeries("Random", true, false, false, 3, 2014));
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // When
        // we get the year range
        List<String> yearRange = DataPublisher.yearRange(timeSerieses);

        // Then
        // we expect a four year range
        assertThat(yearRange, Matchers.contains("2012", "2013", "2014", "2015"));
    }

    @Test
    public void yearRange_givenDisjointTimeSeries_fillsInTheGaps() {
        // Given
        // two timeseries that overlap
        List<Path> serieses = new ArrayList<>();
        serieses.add(bob.randomWalkTimeSeries("Random", true, false, false, 3, 2015));
        serieses.add(bob.randomWalkTimeSeries("Random", true, false, false, 3, 2011));
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // When
        // we get the year range
        List<String> yearRange = DataPublisher.yearRange(timeSerieses);

        // Then
        // we expect a four year range
        assertThat(yearRange, Matchers.contains("2009", "2010", "2011", "2012", "2013", "2014", "2015"));
    }

    @Test
    public void quarterRange_givenSingleTimeSeries_doesReturnFullQuarters() {
        // Given
        // a timeseries
        List<Path> serieses = new ArrayList<>();
        serieses.add(bob.randomWalkQuarters("quarters", 2014, 3, 3));
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // When
        // we get the quarter range
        List<String> quarterRange = DataPublisher.quarterRange(timeSerieses);

        // Then
        // we expect a three quarter range
        assertThat(quarterRange, Matchers.containsInAnyOrder("2014 Q4", "2015 Q1", "2015 Q2"));
    }


    @Test
    public void quarterRange_givenOverlappingTimeSeries_givesUnionRange() {
        // Given
        // two timeseries that overlap
        List<Path> serieses = new ArrayList<>();
        serieses.add(bob.randomWalkQuarters("quarters", 2014, 2, 3));
        serieses.add(bob.randomWalkQuarters("quarters", 2014, 3, 3));
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // When
        // we get the quarter range
        List<String> quarterRange = DataPublisher.quarterRange(timeSerieses);

        // Then
        // we expect a four quarter range
        assertThat(quarterRange, Matchers.contains("2014 Q3", "2014 Q4", "2015 Q1", "2015 Q2"));
    }

    @Test
    public void quarterRange_givenDisjointTimeSeries_fillsInTheGaps() {
        // Given
        // two timeseries that overlap
        List<Path> serieses = new ArrayList<>();
        serieses.add(bob.randomWalkQuarters("quarters", 2014, 3, 3));
        serieses.add(bob.randomWalkQuarters("quarters", 2013, 3, 3));
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // When
        // we get the quarter range
        List<String> quarterRange = DataPublisher.quarterRange(timeSerieses);

        // Then
        // we expect a four quarter range
        assertThat(quarterRange, Matchers.contains("2013 Q4", "2014 Q1", "2014 Q2", "2014 Q3", "2014 Q4", "2015 Q1", "2015 Q2"));
    }

    @Test
    public void monthRange_givenSingleTimeSeries_doesReturnFullMonths() {
        // Given
        // a timeseries
        List<Path> serieses = new ArrayList<>();
        serieses.add(bob.randomWalkMonths("months", 2014, 10, 3));
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // When
        // we get the quarter range
        List<String> monthRange = DataPublisher.monthRange(timeSerieses);

        // Then
        // we expect a three quarter range
        assertThat(monthRange, Matchers.containsInAnyOrder("2014 NOV", "2014 DEC", "2015 JAN"));
    }


    @Test
    public void monthRange_givenOverlappingTimeSeries_givesUnionRange() {
        // Given
        // two timeseries that overlap
        List<Path> serieses = new ArrayList<>();
        serieses.add(bob.randomWalkMonths("months", 2014, 10, 3));
        serieses.add(bob.randomWalkMonths("months", 2014, 9, 3));
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // When
        // we get the month range
        List<String> monthRange = DataPublisher.monthRange(timeSerieses);

        // Then
        // we expect a four month range
        assertThat(monthRange, Matchers.contains("2014 OCT", "2014 NOV", "2014 DEC", "2015 JAN"));
    }

    @Test
    public void monthRange_givenDisjointTimeSeries_fillsInTheGaps() {
        // Given
        // two timeseries that overlap
        List<Path> serieses = new ArrayList<>();
        serieses.add(bob.randomWalkMonths("months", 2014, 10, 3));
        serieses.add(bob.randomWalkMonths("months", 2014, 6, 3));
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // When
        // we get the month range
        List<String> monthRange = DataPublisher.monthRange(timeSerieses);

        // Then
        // we expect a four month range
        assertThat(monthRange, Matchers.contains("2014 JUL", "2014 AUG", "2014 SEP", "2014 OCT", "2014 NOV", "2014 DEC", "2015 JAN"));
    }

    @Test
    public void gridOfData_givenSetOfTimeSeries_returnsExpectedGrid() {
        // Given
        // some time series we create and then retrieve
        List<Path> serieses = new ArrayList<>();
        serieses.add(bob.randomWalkTimeSeries("a"));
        serieses.add(bob.randomWalkTimeSeries("b"));
        serieses.add(bob.randomWalkTimeSeries("c"));
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);

        // When
        // we use timeSeriesIdList to pull out Ids
        List<List<String>> grid = DataPublisher.gridOfAllDataInTimeSeriesList(timeSerieses);

        // Then
        // we expect the map to contain all information
        for (int i = 0; i < timeSerieses.size(); i++) {
            TimeSeries series = timeSerieses.get(i);
            assertEquals(series.getDescription().getTitle(), grid.get(0).get(i + 1));
            assertEquals(series.getDescription().getCdid(), grid.get(1).get(i + 1));

            for (TimeSeriesValue value : series.years) {
                assertEquals(value.value, gridCell(grid, series.getCdid(), value.date));
            }

            for (TimeSeriesValue value : series.quarters) {
                assertEquals(value.value, gridCell(grid, series.getCdid(), value.date));
            }

            for (TimeSeriesValue value : series.months) {
                assertEquals(value.value, gridCell(grid, series.getCdid(), value.date));
            }
        }
    }

    private String gridCell(List<List<String>> grid, String colHeader, String rowHeader) {
        int colNumber = -1;
        int rowNumber = -1;
        for (int i = 0; i < grid.get(0).size(); i++) {
            if (colHeader.equalsIgnoreCase(grid.get(0).get(i))) {
                colNumber = i;
                break;
            }
        }

        for (int i = 0; i < grid.size(); i++) {
            if (rowHeader.equalsIgnoreCase(grid.get(i).get(0))) {
                rowNumber = i;
                break;
            }
        }

        return grid.get(rowNumber).get(colNumber);
    }

    @Test
    public void writeXLSX_givenGridOfData_willCreateFile() throws IOException {
        // Given
        // random data
        List<Path> serieses = new ArrayList<>();
        serieses.add(bob.randomWalkTimeSeries("a"));
        serieses.add(bob.randomWalkTimeSeries("b"));
        serieses.add(bob.randomWalkTimeSeries("c"));
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);
        List<List<String>> grid = DataPublisher.gridOfAllDataInTimeSeriesList(timeSerieses);


        // When
        // we generate an XLSX as a temp file
        Path path = Files.createTempFile("temporary", ".xlsx");
        DataPublisher.writeDataGridToXlsx(path, grid);

        // Then
        // It will save a file
        assertTrue(path.toFile().length() > 12);
    }

    @Test
    public void writeCSV_givenGridOfData_willCreateFile() throws IOException {
        // Given
        // random data
        List<Path> serieses = new ArrayList<>();
        serieses.add(bob.randomWalkTimeSeries("a"));
        serieses.add(bob.randomWalkTimeSeries("b"));
        serieses.add(bob.randomWalkTimeSeries("c"));
        List<TimeSeries> timeSerieses = DataPublisher.timeSeriesesFromPathList(serieses);
        List<List<String>> grid = DataPublisher.gridOfAllDataInTimeSeriesList(timeSerieses);


        // When
        // we generate an XLSX as a temp file
        Path path = Files.createTempFile("temporary", ".csv");
        DataPublisher.writeDataGridToCsv(path, grid);

        // Then
        // It will save a file
        assertTrue(path.toFile().length() > 12);
    }
}
