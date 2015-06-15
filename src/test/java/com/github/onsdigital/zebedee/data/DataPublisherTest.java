package com.github.onsdigital.zebedee.data;

import com.github.onsdigital.content.page.statistics.Dataset;
import com.github.onsdigital.content.page.statistics.data.TimeSeries;
import com.github.onsdigital.content.partial.TimeseriesValue;
import com.github.onsdigital.content.util.ContentUtil;
import com.github.onsdigital.zebedee.Builder;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.model.Sessions;
import org.apache.commons.io.FileUtils;
import org.apache.poi.util.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Time;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by thomasridd on 11/06/15.
 */
public class DataPublisherTest {
    Zebedee zebedee;
    Collection collection1;
    Collection collection2;
    Builder builder;
    String publisher1Email;
    String publisher2Email;
    Session session;

    // Data for after we have dealt with Brian
    TimeSerieses serieses;
    Dataset dataset;

    Map<String, String> envVariables;

    @Before
    public void setUp() throws Exception {
        // Creates a zebedee with two collections, each of which contain a dataset
        //
        // Note the collection event history will not be properly defined

        builder = new Builder(this.getClass());
        zebedee = new Zebedee(builder.zebedee);
        collection1 = new Collection(builder.collections.get(0), zebedee);
        collection2 = new Collection(builder.collections.get(1), zebedee);
        publisher1Email = builder.publisher1.email;
        publisher2Email = builder.publisher2.email;
        session = zebedee.sessions.create(publisher1Email);


        envVariables = DataPublisher.env;


        Path toPath = collection1.reviewed.toPath("/datapublishertest/csdb_with_extension/CXNV.csdb");
        if (!Files.exists(toPath.getParent())) {
            Files.createDirectories(toPath.getParent());
        }

        String outPath = "/csdb/csdb_with_extension/CXNV.csdb";
        try(InputStream inputStream = getClass().getResource(outPath).openStream();
            OutputStream outputStream = FileUtils.openOutputStream(toPath.toFile())) {
            IOUtils.copy(inputStream, outputStream);
        }

        toPath = collection1.reviewed.toPath("/datapublishertest/csdb_with_extension/data.json");
        outPath = "/csdb/csdb_with_extension/data.json";
        try(InputStream inputStream = getClass().getResource(outPath).openStream();
            OutputStream outputStream = FileUtils.openOutputStream(toPath.toFile())) {
            IOUtils.copy(inputStream, outputStream);
        }

        toPath = collection2.reviewed.toPath("/datapublishertest/csdb_no_extension/OTT");
        if (!Files.exists(toPath.getParent())) {
            Files.createDirectories(toPath.getParent());
        }
        outPath = "/csdb/csdb_no_extension/OTT";
        try(InputStream inputStream = getClass().getResource(outPath).openStream();
            OutputStream outputStream = FileUtils.openOutputStream(toPath.toFile())) {
            IOUtils.copy(inputStream, outputStream);
        }

        toPath = collection2.reviewed.toPath("/datapublishertest/csdb_no_extension/data.json");
        outPath = "/csdb/csdb_no_extension/data.json";
        try(InputStream inputStream = getClass().getResource(outPath).openStream();
            OutputStream outputStream = FileUtils.openOutputStream(toPath.toFile())) {
            IOUtils.copy(inputStream, outputStream);
        }

        String brianPath = "/csdb/csdb_no_extension/brian.json";
        try(InputStream inputStream = getClass().getResourceAsStream(brianPath)) {
            serieses = ContentUtil.deserialise(inputStream, TimeSerieses.class);
        }
        String datasetPath = "/csdb/csdb_no_extension/data.json";
        try(InputStream inputStream = getClass().getResourceAsStream(datasetPath)) {
            dataset = ContentUtil.deserialise(inputStream, Dataset.class);
        }


    }

    @After
    public void tearDown() throws Exception {
        builder.delete();
        DataPublisher.env = envVariables;
    }

    /**
     * Test the setup has worked
     */
    @Test
    public void prebuiltCollectionShouldContainFiles() {
        // Given
        // the prebuilt collection and expected files
        String file1 = "/datapublishertest/csdb_with_extension/CXNV.csdb";
        String json1 = "/datapublishertest/csdb_with_extension/data.json";

        String file2 = "/datapublishertest/csdb_no_extension/OTT";
        String json2 = "/datapublishertest/csdb_no_extension/data.json";

        // Then
        // we expect these files to exist
        assertTrue(Files.exists(collection1.reviewed.toPath(file1)));
        assertTrue(Files.exists(collection1.reviewed.toPath(json1)));
        assertTrue(Files.exists(collection2.reviewed.toPath(file2)));
        assertTrue(Files.exists(collection2.reviewed.toPath(json2)));
    }

    @Test
    public void datasetIdFromDatafilePathShouldGiveFilenameWithoutExtension() throws Exception {
        // Given
        // a couple of file names
        String file1 = "/datapublishertest/csdb_with_extension/CXNV.csdb";
        String file2 = "/datapublishertest/csdb_with_extension/CXNV";

        // When
        // we turn these into paths
        Path path1 = collection1.reviewed.toPath(file1);
        Path path2 = collection1.reviewed.toPath(file2);

        // Then
        // we expect datasetIds that are the filename without extension
        assertEquals("CXNV", DataPublisher.datasetIdFromDatafilePath(path1));
        assertEquals("CXNV", DataPublisher.datasetIdFromDatafilePath(path2));
    }

    @Test
    public void csdbDatasetsInCollectionShouldContainFilesWithExtension() throws Exception {
        // Given
        // our pre setup collection with csdb extensions
        String file1 = collection1.reviewed.toPath("/datapublishertest/csdb_with_extension/CXNV.csdb").toString();
        String json1 = collection1.reviewed.toPath("/datapublishertest/csdb_with_extension/data.json").toString();


        // When
        // we search for csdb datasets
        List<HashMap<String, Path>> datasets = DataPublisher.csdbDatasetsInCollection(collection1, session);

        // Then
        // we expect a single result
        assertEquals(1, datasets.size());
        // with the correct paths
        HashMap<String, Path> dataset = datasets.get(0);
        assertEquals(file1, dataset.get("file").toString());
        assertEquals(json1, dataset.get("json").toString());
    }

    @Test
    public void csdbDatasetsInCollectionShouldContainFilesWithoutExtension() throws Exception {
        // Given
        // our pre setup collection with csdb extensions
        String file2 = collection2.reviewed.toPath("/datapublishertest/csdb_no_extension/OTT").toString();
        String json2 = collection2.reviewed.toPath("/datapublishertest/csdb_no_extension/data.json").toString();


        // When
        // we search for csdb datasets
        List<HashMap<String, Path>> datasets = DataPublisher.csdbDatasetsInCollection(collection2, session);

        // Then
        // we expect a single result
        assertEquals(1, datasets.size());
        // with the correct paths
        HashMap<String, Path> dataset = datasets.get(0);
        assertEquals(file2, dataset.get("file").toString());
        assertEquals(json2, dataset.get("json").toString());
    }

    @Test
    public void csdbURIShouldComeFromEnvVariable() throws Exception {
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
    public void datasetShouldDeserialise() throws IOException {
        // Given
        // one of our collections
        List<HashMap<String, Path>> datasets = DataPublisher.csdbDatasetsInCollection(collection2, session);

        // When
        // we deserialise the dataset
        Dataset dataset = ContentUtil.deserialise(FileUtils.openInputStream(datasets.get(0).get("json").toFile()), Dataset.class);

        // Then
        // we expect a dataset and check a couple of fields
        assertNotNull(dataset);
        assertEquals("Visits and Spending by UK residents abroad and overseas residents in the UK.", dataset.description);
        assertEquals("/businessindustryandtrade/tourismindustry/datasets/overseastravelandtourism", dataset.uri.toString());
    }

    @Test
    public void timeSeriesFromBrianShouldDeserialise() {
        // Given
        // the file returned by brian that corresponds to csdb_no_extension

        // When
        // we deserialise it (as we did in setup)

        // Then
        // we expect it to be non null and to have data
        assertNotNull(serieses);
        assertTrue(serieses.size() > 0);
    }

    @Test
    public void uriForSeriesInDatasetShouldBeCorrect() {
        // Given
        // the series and dataset we loaded at setup
        String datasetURI = "/businessindustryandtrade/tourismindustry/datasets/overseastravelandtourism";
        TimeSeries series = serieses.get(0);
        String seriesId = series.cdid;

        // When
        // we use these to generate a timeseries uri
        String seriesURI = DataPublisher.uriForSeriesInDataset(dataset, series);

        // Then
        // we expect a uri in the corresponding timeseries folder with CDID as a subfolder
        assertEquals("/businessindustryandtrade/tourismindustry/timeseries/" + seriesId,
                seriesURI);

    }

    @Test
    public void startPageForNewSeriesShouldBeVanilla() throws IOException {
        // Given
        // the series from the standard taxonomy
        TimeSeries noCurrentSeries = null;
        for (TimeSeries series: this.serieses) {
            if (series.cdid.equalsIgnoreCase("GMAA")) {
                noCurrentSeries = series;
            }
        }
        assertNotNull(noCurrentSeries);

        String uri = DataPublisher.uriForSeriesInDataset(dataset, noCurrentSeries);

        // When
        // we get a start page
        TimeSeries startPage = DataPublisher.startPageForSeriesWithPublishedPath(zebedee, uri, noCurrentSeries);

        // Then
        // we expect it to have nothing in terms of data
        assertEquals(0,startPage.months.size());
        assertEquals(0, startPage.years.size());
        assertEquals(0, startPage.quarters.size());
    }

    @Test
    public void startPageForExistingSeriesShouldBePopulated() throws IOException {
        // Given
        // the gmbb series which we upload to an existing point
        TimeSeries hasCurrentSeries = null;
        for (TimeSeries series: this.serieses) {
            if (series.cdid.equalsIgnoreCase("GMBB")) {
                hasCurrentSeries = series;
            }
        }
        String uri = DataPublisher.uriForSeriesInDataset(dataset, hasCurrentSeries);

        Path path = zebedee.published.toPath(uri);
        IOUtils.copy(getClass().getResourceAsStream("/csdb/csdb_no_extension/gmbb.json"),
                FileUtils.openOutputStream(path.resolve("data.json").toFile()));

        // When
        // we get a start page
        TimeSeries startPage = DataPublisher.startPageForSeriesWithPublishedPath(zebedee, uri, hasCurrentSeries);

        // Then
        // we expect it to have some data
        int size = startPage.months.size() + startPage.years.size() + startPage.quarters.size();
        assertNotEquals(0, size);
    }

    @Test
    public void populateTimeSeriesShouldFillEmptySeries() throws IOException {
        // Given
        // a page with null values
        TimeSeries page = new TimeSeries();
        // and some values
        Set<TimeseriesValue> timeseriesValues = new HashSet<>();
        TimeseriesValue add = new TimeseriesValue();
            add.year = "2010"; add.value = "1"; add.date = "2010";
           timeseriesValues.add(add);
        TimeseriesValue add2 = new TimeseriesValue();
            add2.year = "2011"; add2.value = "1"; add2.date = "2011";
            timeseriesValues.add(add2);

        // When
        // we add these values
        DataPublisher.populatePageFromSetOfValues(page, page.years, timeseriesValues, dataset);

        // Then
        // we expect years to be populated
        assertNotEquals(0, page.years.size());
        assertEquals(0, page.months.size());
        assertEquals(0, page.quarters.size());
    }

    @Test
    public void dataSetPageDoesMoveValuesToTimeseries() {
        // Given
        // a test dataset
        Dataset testSet = new Dataset();
        testSet.datasetId = "dataSetPageDoesMoveValuesToTimeseries";
        testSet.releaseDate = new GregorianCalendar(1877, 3, 15).getTime();
        testSet.nextReleaseDate = new GregorianCalendar(1877, 3, 31).getTime();

        // When
        // we populate a series
        TimeSeries page = new TimeSeries();
        DataPublisher.populatePageFromDataSetPage(page, testSet);

        // Then
        // we expect the data from the dataset to have transferred
        assertEquals(testSet.releaseDate.toString(), page.releaseDate.toString());
        assertEquals(testSet.nextReleaseDate.toString(), page.nextReleaseDate.toString());
        assertEquals(testSet.datasetId, page.sourceDatasets.get(0));

    }
}