package com.github.onsdigital.zebedee.data;

import au.com.bytecode.opencsv.CSVWriter;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.content.partial.Contact;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.Log;
import com.github.onsdigital.zebedee.util.ZipUtils;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by thomasridd on 04/06/15.
 *
 * Runs the logic to process dataset files into timeseries and downloadable spreadsheets
 *
 * Presently these are CSDB files only
 */
public class DataPublisher {
    static final String BRIAN_KEY = "brian_url";

    static final String DEFAULT_FILE = "data";
    public static int insertions = 0;
    public static int corrections = 0;
    public static Map<String, String> env = System.getenv();

    /**
     * Run the preprocessing step that converts data to timeseries
     *
     * @param zebedee a zebedee instance
     * @param collection the collection being published
     * @param session a user session (required for some permissions)
     *
     * @throws IOException
     * @throws BadRequestException
     * @throws UnauthorizedException
     * @throws URISyntaxException
     */
    public void preprocessCollection(Zebedee zebedee, Collection collection, Session session) throws IOException, BadRequestException, UnauthorizedException, URISyntaxException {

        if (env.get(BRIAN_KEY) == null || env.get(BRIAN_KEY).length() == 0) {
            System.out.println("Environment variable brian_url not set. Preprocessing step for " + collection.description.name + " skipped");
            return;
        }

        insertions = 0; corrections = 0;

        preprocessCSDB(zebedee, collection, session);

        if(insertions + corrections > 0) {
            System.out.println(collection.description.name + " processed. Insertions: " + insertions + "      Corrections: " + corrections);
        }

        CompressTimeseries(collection);
    }

    /**
     * Zip up timeseries to be transferred by the train
     *
     * @param collection the collection being published
     * @throws IOException
     */
    private void CompressTimeseries(Collection collection) throws IOException {
        Log.print("Compressing time series directories...");
        List<Path> timeSeriesDirectories = collection.reviewed.listTimeSeriesDirectories();
        for (Path timeSeriesDirectory : timeSeriesDirectories) {
            Log.print("Compressing time series directory %s", timeSeriesDirectory.toString());
            ZipUtils.zipFolder(timeSeriesDirectory.toFile(), new File(timeSeriesDirectory.toString() + "-to-publish.zip"));

            Log.print("Deleting directory after compression %s", timeSeriesDirectory);
            FileUtils.deleteDirectory(timeSeriesDirectory.toFile());
        }
    }


    /************************************************************************************
     *
     * Section One: Process csdb files by populating timeseries
     *
     ***********************************************************************************/

    /**
     * Run preprocess routine for CSDB datasets
     *
     * The T5 timeseries objects are made by
     * 1. Searching for all .csdb files in a collection
     * 2. Getting Brian to break the files down to their component stats and basic metadata.
     * 3. Combining the stats with metadata entered with the dataset and existing data
     *
     * @param collection the collection to search for dataset objects
     * @param session a user session (required for some permissions)
     * @throws IOException
     * @throws BadRequestException
     * @throws UnauthorizedException
     */
    void preprocessCSDB(Zebedee zebedee, Collection collection, Session session) throws IOException, BadRequestException, UnauthorizedException, URISyntaxException {

        // First find all csdb files in the collection
        List<HashMap<String, Path>> csdbDatasetPages = csdbDatasetsInCollection(collection, session);



        // For each file in this collection
        for (HashMap<String, Path> csdbDataset : csdbDatasetPages) {

            List<TimeSeries> newSeries = new ArrayList<>();

            // Download the dataset page (for metadata)
            Dataset dataset = ContentUtil.deserialise(FileUtils.openInputStream(csdbDataset.get("json").toFile()), Dataset.class);
            String datasetUri = zebedee.toUri(csdbDataset.get("json"));

            DownloadSection csdbSection = new DownloadSection();
            csdbSection.setTitle(dataset.getDescription().getTitle());
            csdbSection.setCdids(new ArrayList<String>());

            // Get a name for the xlsx/csv files to be generated
            dataset.getDescription().setDatasetId(datasetIdFromDatafilePath(csdbDataset.get("file")));

            String filePrefix = dataset.getDescription().getDatasetId();
            if (filePrefix.equalsIgnoreCase("")) { filePrefix = DEFAULT_FILE; }

            DownloadSection xlsxSection = new DownloadSection();
            xlsxSection.setTitle("xlsx download");
            xlsxSection.setFile(datasetUri + "/" + filePrefix.toLowerCase() + ".xlsx");

            DownloadSection csvSection = new DownloadSection();
            csvSection.setTitle("csv download");
            csvSection.setFile(datasetUri + "/" + filePrefix.toLowerCase() + ".csv");

            // Break down the csdb file to timeseries (part-built by extracting csdb files)
            TimeSerieses serieses = callBrianToProcessCSDB(csdbDataset.get("file"));


            for (TimeSeries series : serieses) {
                // Work out the correct timeseries path by working back from the dataset uri
                String uri = uriForSeriesInDataset(datasetUri, series);

                // Construct the new page
                TimeSeries newPage = constructTimeSeriesPageFromComponents(uri, dataset, series, datasetUri);

                // Add the cdid to the dataset page list of cdids
                csdbSection.getCdids().add(newPage.getDescription().getCdid());

                // Save the new page to reviewed
                Path savePath = collection.autocreateReviewedPath(uri + "/data.json");
                IOUtils.write(ContentUtil.serialise(newPage), FileUtils.openOutputStream(savePath.toFile()));

                // Write csv and other files:
                newSeries.add(newPage);
            }

            // Save the dataset to be reviewed
            List<DownloadSection> sections = new ArrayList<>();
            sections.add(csdbSection);
            sections.add(xlsxSection);
            sections.add(csvSection);
            dataset.setDownloads(sections);

            Path savePath = collection.autocreateReviewedPath(datasetUri + "/data.json");
            IOUtils.write(ContentUtil.serialise(dataset), FileUtils.openOutputStream(savePath.toFile()));

            // Save the files
            String filename = dataset.getDescription().getDatasetId();
            if (filename.equalsIgnoreCase("")) {filename = "data";}

            Path xlsPath = collection.autocreateReviewedPath(datasetUri + "/" + filename.toLowerCase() + ".xlsx");
            Path csvPath = collection.autocreateReviewedPath(datasetUri + "/" + filename.toLowerCase() + ".csv");
            List<List<String>> dataGrid = gridOfAllDataInTimeSeriesList(newSeries);
            writeDataGridToXlsx(xlsPath, dataGrid);
            writeDataGridToCsv(csvPath, dataGrid);

            System.out.println("Published " + newSeries.size() + " datasets for " + datasetUri);
        }
    }

    /**
     * Detects datasets appropriate to csdb style publication
     *
     * @param collection a collection ready to be published
     * @param session    a session for the publisher (necessary for easy access)
     * @return a list of hashmaps [{"json": Dataset-definition-data.json, "file": csdb-file.csdb}]
     * @throws IOException
     */
    static List<HashMap<String, Path>> csdbDatasetsInCollection(Collection collection, Session session) throws IOException {
        List<String> csdbUris = new ArrayList<>();
        List<HashMap<String, Path>> results = new ArrayList<>();

        // 1. Detect the uri's
        for (String uri : collection.reviewedUris()) {
            // Two conditions for a file being a csdb file
            if (uri.endsWith(".csdb")) { // 1. - it ends with .csdb
                csdbUris.add(uri);
            } else { // 2. - it has no extension and csdb content
                String[] sections = uri.split("/");
                if (!sections[sections.length - 1].contains(".")) {
                    if (fileIsCsdbFile(collection, session, uri)) {
                        csdbUris.add(uri);
                    }
                }
            }
        }

        // 2. Create a list
        for (String csdbUri : csdbUris) {
            Path csdbPath = collection.find(session.email, csdbUri);
            if (Files.exists(csdbPath)) {
                Path jsonPath = csdbPath.getParent().resolve("data.json");
                if (Files.exists(jsonPath)) {
                    HashMap<String, Path> csdbDataset = new HashMap<>();
                    csdbDataset.put("json", jsonPath);
                    csdbDataset.put("file", csdbPath);
                    results.add(csdbDataset);
                }

            }
        }
        return results;
    }

    /**
     * Advanced checking for csdb files
     *
     * @param collection the collection to process
     * @param session a user session (required for some permissions)
     * @param uri the uri for a file
     * @return whether the file contents is in csdb format
     * @throws IOException
     */
    static boolean fileIsCsdbFile(Collection collection, Session session, String uri) throws IOException {
        Path path = collection.find(session.email, uri);
        boolean confirmed;

        if (!Files.exists(path)) {
            return false;
        } // trivial cases
        if (Files.isDirectory(path)) {
            return false;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(path.toFile()))) { // read the top four lines and compare to .csdb pattern
            br.readLine();
            String firstLine = br.readLine();
            String secondLine = br.readLine();
            String thirdLine = br.readLine();
            confirmed = firstLine.contains("IDENTIFIER") && secondLine.contains("PERIODICITY") && thirdLine.contains("SEASONAL ADJUSTMENT");
        }

        return confirmed;
    }

    /**
     * Get a datasetId by using it's file name
     *
     * @param path a path to a dataset file
     * @return a short id to use as dataset id
     */
    static String datasetIdFromDatafilePath(Path path) {
        String[] sections = path.toString().split("/");
        sections = sections[sections.length - 1].split("\\.");
        return sections[0].toUpperCase();
    }

    /**
     * Derive the correct timeseries uri for a dataset at a specific URI
     *
     * @param datasetUri the dataset uri
     * @param series the timeseries
     * @return the timeseries uri achieved by adding cdid to a dataset's taxonomy node
     */
    static String uriForSeriesInDataset(String datasetUri, TimeSeries series) {
        String[] split = StringUtils.split(datasetUri, "/");
        split = (String[]) ArrayUtils.subarray(split, 0, split.length - 2);

        String uri = StringUtils.join(split, "/");
        uri = ("/" + uri + "/timeseries/" + series.getCdid()).toLowerCase();

        if (uri.startsWith("//")) {
            uri = uri.substring(1);
        } // edge case handler
        return uri;
    }

    /**
     * Post a csdb file to the brian Services/ConvertCSDB endpoint
     *
     * @param path
     * @return a series of timeseries objects
     * @throws IOException
     */
    TimeSerieses callBrianToProcessCSDB(Path path) throws IOException {
        URI url = csdbURI();

        HttpPost post = new HttpPost(url);

        // Add csdb file as a binary
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        FileBody bin = new FileBody(path.toFile());
        multipartEntityBuilder.addPart("file", bin);

        post.setEntity(multipartEntityBuilder.build());

        // Post to the endpoint
        try (CloseableHttpResponse response = HttpClients.createDefault().execute(post)) {
            TimeSerieses result = null;

            //
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                try (InputStream inputStream = entity.getContent()) {
                    try {
                        result = ContentUtil.deserialise(inputStream, TimeSerieses.class);
                    } catch (JsonSyntaxException e) {
                        // This can happen if an error HTTP code is received and the
                        // body of the response doesn't contain the expected object:
                        result = null;
                    }
                }
            } else {
                EntityUtils.consume(entity);
            }
            return result;
        }
    }

    /**
     * Get the URL for the Brian ConvertCSDB endpoint
     *
     * @return
     */
    URI csdbURI() {
        String cdsbURL = env.get("brian_url") + "/Services/ConvertCSDB";
        URI url = null;
        try {
            URIBuilder uriBuilder = new URIBuilder(cdsbURL);
            url = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Data services URL not found: " + cdsbURL);
        }
        return url;
    }

    /**
     * Combine sections of the new timeseries, existing metadata, and metadata from the dataset file
     *
     * @param destinationUri
     * @param dataset
     * @param series
     * @return
     * @throws IOException
     */
    TimeSeries constructTimeSeriesPageFromComponents(String destinationUri, Dataset dataset, TimeSeries series, String datasetURI) throws IOException, URISyntaxException {

        // Attempts to open an existing time series or creates a new one
        TimeSeries page = startPageForSeriesWithPublishedPath(destinationUri, series);

        // Add stats data from the time series (as returned by Brian)
        // NOTE: This will log any corrections as it goes
        populatePageFromTimeSeries(page, series, dataset);

        // Add metadata from the dataset
        populatePageFromDataSetPage(page, dataset, datasetURI);

        return page;
    }

    /**
     * Get a starting point by opening the existing time series page with uri
     *
     * If it doesn't exist create a new one
     *
     * @param zebedee
     * @param uri
     * @param series
     * @return
     * @throws IOException
     */
    static TimeSeries startPageForSeriesWithPublishedPath(Zebedee zebedee, String uri, TimeSeries series) throws IOException {
        TimeSeries page;
        Path path = zebedee.published.toPath(uri);

        if ((path != null) && Files.exists(path.resolve("data.json"))) {
            page = ContentUtil.deserialise(FileUtils.openInputStream(path.resolve("data.json").toFile()), TimeSeries.class);
        } else {
            page = new TimeSeries();
            page.setDescription(new PageDescription());
            page.setCdid(series.getCdid());
            // page.setUri(URI.create(uri));
        }

        return page;
    }

    /**
     * Get a starting point by opening the existing time series page with uri
     *
     * @param uri
     * @param series
     * @return
     * @throws IOException
     */
    static TimeSeries startPageForSeriesWithPublishedPath(String uri, TimeSeries series) throws IOException {
        return startPageForSeriesWithPublishedPath(Root.zebedee, uri, series);
    }

    /**
     * Takes a part-built TimeSeries page and populates the {@link com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeriesValue} list using data gathered from a csdb file
     *
     * @param page    a part-built time series page
     * @param series  a time series returned by Brian by parsing a csdb file
     * @param dataset the source dataset page
     * @return
     */
    TimeSeries populatePageFromTimeSeries(TimeSeries page, TimeSeries series, Dataset dataset) {

        // Time series is a bit of an inelegant beast in that it splits data storage by time period
        // We deal with this by
        populatePageFromSetOfValues(page, page.years, series.years, dataset);
        populatePageFromSetOfValues(page, page.quarters, series.quarters, dataset);
        populatePageFromSetOfValues(page, page.months, series.months, dataset);

        if (page.getDescription() == null || series.getDescription() == null) {
            System.out.println("Problem");
        }
        page.getDescription().setSeasonalAdjustment(series.getDescription().getSeasonalAdjustment());
        page.getDescription().setCdid(series.getDescription().getCdid());

        // Copy across the title if it is currently blank (equates to equalling Cdid)
        if (page.getDescription().getTitle() == null || page.getDescription().getTitle().equalsIgnoreCase("")) {
            page.getDescription().setTitle(series.getDescription().getTitle());
        } else if (page.getDescription().getTitle().equalsIgnoreCase(page.getCdid())) {
            page.getDescription().setTitle(series.getDescription().getTitle());
        }

        page.getDescription().setDate(series.getDescription().getDate());
        page.getDescription().setNumber(series.getDescription().getNumber());

        return page;
    }

    // Support function for above
    void populatePageFromSetOfValues(TimeSeries page, Set<TimeSeriesValue> currentValues, Set<TimeSeriesValue> updateValues, Dataset dataset) {

        // Iterate through values
        for (TimeSeriesValue value : updateValues) {
            // Find the current value of the data point
            TimeSeriesValue current = getCurrentValue(currentValues, value);

            if (current != null) { // A point already exists for this data

                if (!current.value.equalsIgnoreCase(value.value)) { // A point already exists for this data
                    // Take a copy
                    TimeSeriesValue old = ContentUtil.deserialise(ContentUtil.serialise(current), TimeSeriesValue.class);

                    // Update the point
                    current.value = value.value;
                    current.sourceDataset = dataset.getDescription().getDatasetId();

                    current.updateDate = new Date();

                    // Log the correction
                    logCorrection(page, old, current);
                }
            } else {
                value.sourceDataset = dataset.getDescription().getDatasetId();
                value.updateDate = new Date();

                page.add(value);
                //
                logInsertion(page, value);
            }
        }
    }

    /**
     * If a {@link TimeSeriesValue} for value.time exists in currentValues returns that.
     * Otherwise null
     *
     * @param currentValues a set of {@link TimeSeriesValue}
     * @param value         a {@link TimeSeriesValue}
     * @return a {@link TimeSeriesValue} from currentValues
     */
    static TimeSeriesValue getCurrentValue(Set<TimeSeriesValue> currentValues, TimeSeriesValue value) {
        if (currentValues == null) {
            return null;
        }

        for (TimeSeriesValue current : currentValues) {
            if (current.compareTo(value) == 0) {
                return current;
            }
        }
        return null;
    }

    /**
     * Add release dates, contacts, and other details to a time series
     *
     * @param page
     * @param datasetPage
     * @param datasetURI
     * @return
     * @throws URISyntaxException
     */
    static TimeSeries populatePageFromDataSetPage(TimeSeries page, Dataset datasetPage, String datasetURI) throws URISyntaxException {
        PageDescription description = page.getDescription();
        if (description == null) {
            description = new PageDescription();
            page.setDescription(description);
        }
        description.setNextRelease(datasetPage.getDescription().getNextRelease());
        description.setReleaseDate(datasetPage.getDescription().getReleaseDate());

        // Set some details
        if (datasetPage.getDescription().getContact() != null) {
            Contact contact = new Contact();
            if (datasetPage.getDescription().getContact().getName() != null) {
                contact.setName(datasetPage.getDescription().getContact().getName());
            }
            if (datasetPage.getDescription().getContact().getTelephone() != null) {
                contact.setTelephone(datasetPage.getDescription().getContact().getTelephone());
            }
            if (datasetPage.getDescription().getContact().getEmail() != null) {
                contact.setEmail(datasetPage.getDescription().getContact().getEmail());
            }

            page.getDescription().setContact(contact);
        }

        // Ensure dataset is part of related datasets
        List<Link> relatedDatasets = page.getRelatedDatasets();
        if (relatedDatasets == null) {
            relatedDatasets = new ArrayList<>();
        }
        boolean datasetNotLinked = true;
        for (Link relatedDataset : relatedDatasets) {
            if (relatedDataset.getUri().toString().equalsIgnoreCase(datasetURI)) {
                datasetNotLinked = false;
                break;
            }
        }
        if (datasetNotLinked) {
            relatedDatasets.add(new Link(new URI(datasetURI)));
            page.setRelatedDatasets(relatedDatasets);
        }

        // Add stats bulletins
        if (datasetPage.getRelatedDocuments() != null) {
            page.setRelatedDocuments(datasetPage.getRelatedDocuments());
        }

        // Add the dataset id if relevant
        boolean datasetIsNew = true;
        for (String datasetId : page.sourceDatasets) {
            if (datasetPage.getDescription().getDatasetId().equalsIgnoreCase(datasetId)) {
                datasetIsNew = false;
                break;
            }
        }
        if (datasetIsNew) {
            page.sourceDatasets.add(datasetPage.getDescription().getDatasetId().toUpperCase());
        }

        return page;
    }

    /**
     * @param series
     * @param oldValue
     * @param newValue
     */
    private void logCorrection(TimeSeries series, TimeSeriesValue oldValue, TimeSeriesValue newValue) {
        corrections += 1;
    }

    private void logInsertion(TimeSeries series, TimeSeriesValue newValue) {
        insertions += 1;
    }




    /************************************************************************************
     *
     * Section Two: Pregenerate csv and xlsx files for timeseries
     *
     * This works by grabbing all data and storing it in a hashmap of hashmaps
     * It then works out column headings (timeSeriesIdList) and rows
     * It then turns this into a grid of data that can be outputted to csv or xlsx
     *
     ***********************************************************************************/

    /**
     * Load an array of TimeSeries objects from an array of paths
     *
     * @param pathsToTimeSeries
     * @return
     */
    static List<TimeSeries> timeSeriesesFromPathList(List<Path> pathsToTimeSeries) {
        List<TimeSeries> serieses = new ArrayList<>();

        for (Path path : pathsToTimeSeries) {
            try (InputStream stream = Files.newInputStream(path)) {
                TimeSeries series = (TimeSeries) ContentUtil.deserialiseContent(stream);
                serieses.add(series);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return serieses;
    }

    /**
     * A map of maps containing all data so that map.get(CDID).get(TIME) gives
     * the value
     *
     * @param serieses
     * @return a Map of Maps as described above
     */
    static Map<String, Map<String, String>> mapOfAllDataInTimeSeriesList(List<TimeSeries> serieses) {
        HashMap<String, Map<String, String>> map = new HashMap<>();

        for (TimeSeries series : serieses) {
            // add basic details
            addTimeSeriesMetadataToMap(map, series);

            // Add the data rows
            mapTimeRange(map, series.getCdid(), series.years);

            mapTimeRange(map, series.getCdid(), series.quarters);

            mapTimeRange(map, series.getCdid(), series.months);

        }

        return map;
    }

    /**
     * Add required metadata as header rows to the map
     *
     * @param map
     * @param series
     */
    private static void addTimeSeriesMetadataToMap(HashMap<String, Map<String, String>> map, TimeSeries series) {
        putCombination(series.getCdid(), "Title", series.getDescription().getTitle(), map);
        putCombination(series.getCdid(), "CDID", series.getDescription().getCdid(), map);
        putCombination(series.getCdid(), "National Statistic", (series.getDescription().isNationalStatistic() ? "Y" : "N"), map);
        putCombination(series.getCdid(), "Seasonally Adjusted", (series.getDescription().getSeasonalAdjustment()), map);
        putCombination(series.getCdid(), "PreUnit", series.getDescription().getPreUnit(), map);
        putCombination(series.getCdid(), "Unit", series.getDescription().getUnit(), map);

        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        if (series.getDescription().getReleaseDate() == null) {
            putCombination(series.getCdid(), "Release date", "", map);
        } else {
            putCombination(series.getCdid(), "Release date", format.format(series.getDescription().getReleaseDate()), map);
        }
        putCombination(series.getCdid(), "Next release", series.getDescription().getNextRelease(), map);
        putCombination(series.getCdid(), "Important notes", StringUtils.join(series.getNotes(), ", "), map);
    }

    /**
     * Add a set of TimeSeriesValues to the map
     *
     * @param map
     * @param cdid
     * @param timeRange
     */
    private static void mapTimeRange(HashMap<String, Map<String, String>> map, String cdid, Set<TimeSeriesValue> timeRange) {
        if (timeRange != null) {
            for (TimeSeriesValue value : timeRange) {
                putCombination(cdid, value.date, value.value, map);
            }
        }
    }

    /**
     * Update a map of maps with a value for a cdid with a specified row heading
     *
     * @param cdid
     * @param row
     * @param value
     * @param map
     */
    private static void putCombination(String cdid, String row, String value, Map<String, Map<String, String>> map) {
        Map<String, String> submap = new HashMap<>();
        if (map.containsKey(row)) {
            submap = map.get(row);
        }

        submap.put(cdid, value);
        map.put(row, submap);
    }


    /**
     * Turn an array of TimeSeries into a grid of strings suitable for writing to xls/csv
     *
     * @param serieses
     * @return
     */
    static List<List<String>> gridOfAllDataInTimeSeriesList(List<TimeSeries> serieses) {
        // Create a grid
        List<List<String>> rows = new ArrayList<>();

        // Derive column headings
        List<String> orderedCDIDs = timeSeriesIdList(serieses);

        // Derive the data for the grid in map form
        Map<String, Map<String, String>> mapOfData = mapOfAllDataInTimeSeriesList(serieses);


        // Add header rows for each timeseries
        addDetailRows(rows, orderedCDIDs, mapOfData);

        // Add year data
        List<String> yearRange = yearRange(serieses);
        addTimeRangeRows(rows, orderedCDIDs, mapOfData, yearRange);

        // Add quarter data
        List<String> quarterRange = quarterRange(serieses);
        addTimeRangeRows(rows, orderedCDIDs, mapOfData, quarterRange);

        // Add month data
        List<String> monthRange = monthRange(serieses);
        addTimeRangeRows(rows, orderedCDIDs, mapOfData, monthRange);

        return rows;
    }

    /**
     * Get a list of cdids (these will be our column headings)
     *
     * @param serieses
     * @return
     */
    static List<String> timeSeriesIdList(List<TimeSeries> serieses) {
        List<String> ids = new ArrayList<>();
        for (TimeSeries series : serieses) {
            ids.add(series.getCdid());
        }
        return ids;
    }

    /**
     * Add the standard set of metadata at the top of each column
     *
     * @param rows
     * @param orderedCDIDs
     * @param mapOfData
     */
    private static void addDetailRows(List<List<String>> rows, List<String> orderedCDIDs, Map<String, Map<String, String>> mapOfData) {
        List<String> titleRow = new ArrayList<>();
        titleRow.add("Title");
        List<String> cdidRow = new ArrayList<>();
        cdidRow.add("CDID");
        List<String> nationalStatistic = new ArrayList<>();
        nationalStatistic.add("National Statistic");
        List<String> seasonallyAdjusted = new ArrayList<>();
        seasonallyAdjusted.add("Seasonally Adjusted");
        List<String> preunit = new ArrayList<>();
        preunit.add("PreUnit");
        List<String> unit = new ArrayList<>();
        unit.add("Unit");
        List<String> releaseDate = new ArrayList<>();
        releaseDate.add("Release date");
        List<String> nextRelease = new ArrayList<>();
        nextRelease.add("Next release");
        List<String> importantNotes = new ArrayList<>();
        importantNotes.add("Important notes");

        for (String cdid : orderedCDIDs) {
            titleRow.add(mapOfData.get("Title").get(cdid));
            cdidRow.add(cdid);
            nationalStatistic.add(mapOfData.get("National Statistic").get(cdid));
            seasonallyAdjusted.add(mapOfData.get("Seasonally Adjusted").get(cdid));
            preunit.add(mapOfData.get("PreUnit").get(cdid));
            unit.add(mapOfData.get("Unit").get(cdid));
            releaseDate.add(mapOfData.get("Release date").get(cdid));
            nextRelease.add(mapOfData.get("Next release").get(cdid));
            importantNotes.add(mapOfData.get("Important notes").get(cdid));
        }
        rows.add(titleRow);
        rows.add(cdidRow);
        rows.add(nationalStatistic);
        rows.add(seasonallyAdjusted);
        rows.add(preunit);
        rows.add(unit);
        rows.add(releaseDate);
        rows.add(nextRelease);
        rows.add(importantNotes);
    }

    /**
     * Get list of years that should appear in a spreadsheet for a given set of time series
     *
     * @param seriesList the timeseries
     * @return a List of
     */
    static List<String> yearRange(List<TimeSeries> seriesList) {
        TimeSeriesValue min = null;
        TimeSeriesValue max = null;
        for (TimeSeries series : seriesList) {
            for (TimeSeriesValue value : series.years) {
                if (min == null || min.compareTo(value) > 0) {
                    min = value;
                }
                if (max == null || max.compareTo(value) < 0) {
                    max = value;
                }
            }
        }

        if (min == null) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(min.toDate());
        int minYear = cal.get(Calendar.YEAR);
        cal.setTime(max.toDate());
        int maxYear = cal.get(Calendar.YEAR);

        List<String> yearLabels = new ArrayList<>();
        for (int i = minYear; i <= maxYear; i++) {
            yearLabels.add(i + "");
        }

        return yearLabels;
    }

    /**
     * Get list of quarters that should appear in a spreadsheet for a given set of time series
     *
     * @param seriesList the timeseries
     * @return a List of
     */
    static List<String> quarterRange(List<TimeSeries> seriesList) {
        TimeSeriesValue min = null;
        TimeSeriesValue max = null;
        for (TimeSeries series : seriesList) {
            for (TimeSeriesValue value : series.quarters) {
                if (min == null || min.compareTo(value) > 0) {
                    min = value;
                }
                if (max == null || max.compareTo(value) < 0) {
                    max = value;
                }
            }
        }

        if (min == null) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(min.toDate());
        int minYear = cal.get(Calendar.YEAR);
        int minQuarter = cal.get(Calendar.MONTH) / 3;

        cal.setTime(max.toDate());
        int maxYear = cal.get(Calendar.YEAR);
        int maxQuarter = cal.get(Calendar.MONTH) / 3;

        String[] quarters = "Q1,Q2,Q3,Q4".split(",");

        List<String> quarterLabels = new ArrayList<>();

        for (int i = minYear; i <= maxYear; i++) {
            for (int q = 0; q < 4; q++) {
                if (i == minYear) {
                    if (q >= minQuarter) {
                        quarterLabels.add(i + " " + quarters[q]);
                    }
                } else if (i == maxYear) {
                    if (q <= maxQuarter) {
                        quarterLabels.add(i + " " + quarters[q]);
                    }
                } else {
                    quarterLabels.add(i + " " + quarters[q]);
                }
            }
        }

        return quarterLabels;
    }

    /**
     * Get list of months that should appear in a spreadsheet for a given set of time series
     *
     * @param seriesList the timeseries
     * @return a List of
     */
    static List<String> monthRange(List<TimeSeries> seriesList) {
        TimeSeriesValue min = null;
        TimeSeriesValue max = null;
        for (TimeSeries series : seriesList) {
            for (TimeSeriesValue value : series.months) {
                if (min == null || min.compareTo(value) > 0) {
                    min = value;
                }
                if (max == null || max.compareTo(value) < 0) {
                    max = value;
                }
            }
        }

        if (min == null) {
            return null;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(min.toDate());
        int minYear = cal.get(Calendar.YEAR);
        int minMonth = cal.get(Calendar.MONTH);

        cal.setTime(max.toDate());
        int maxYear = cal.get(Calendar.YEAR);
        int maxMonth = cal.get(Calendar.MONTH);

        String[] months = "JAN,FEB,MAR,APR,MAY,JUN,JUL,AUG,SEP,OCT,NOV,DEC".split(",");

        List<String> monthLabels = new ArrayList<>();

        for (int i = minYear; i <= maxYear; i++) {
            for (int q = 0; q < 12; q++) {
                if (i == minYear) {
                    if (q >= minMonth) {
                        monthLabels.add(i + " " + months[q]);
                    }
                } else if (i == maxYear) {
                    if (q <= maxMonth) {
                        monthLabels.add(i + " " + months[q]);
                    }
                } else {
                    monthLabels.add(i + " " + months[q]);
                }
            }
        }

        return monthLabels;
    }

    /**
     * Add rows to a data grid for a given set of time periods
     *
     * @param rows a grid to add data to
     * @param orderedCDIDs the columns
     * @param mapOfData a map to source the data values
     * @param timeRange the rows
     */
    private static void addTimeRangeRows(List<List<String>> rows, List<String> orderedCDIDs, Map<String, Map<String, String>> mapOfData, List<String> timeRange) {
        if (timeRange != null) {
            for (String period : timeRange) {
                List<String> newRow = new ArrayList<>();
                newRow.add(period);
                for (String cdid : orderedCDIDs) {
                    newRow.add(mapOfData.get(period).get(cdid));
                }
                rows.add(newRow);
            }
        }
    }

    /**
     * Output a grid of strings to XLSX
     *
     * @param xlsPath the path to save the file
     * @param grid the data grid to save
     * @throws IOException
     */
    static void writeDataGridToXlsx(Path xlsPath, List<List<String>> grid) throws IOException {
        Workbook wb = new SXSSFWorkbook(30);
        Sheet sheet = wb.createSheet("data");

        int rownum = 0;
        for (List<String> gridRow : grid) {
            Row row = sheet.createRow(rownum++);

            int colnum = 0;
            for (String gridCell : gridRow) {
                row.createCell(colnum++).setCellValue(gridCell);
            }
        }

        try (OutputStream stream = Files.newOutputStream(xlsPath)) {
            wb.write(stream);
        }
    }

    /**
     * Output a grid of strings to CSV
     *
     * @param csvPath path to write to
     * @param grid    grid to output to
     * @throws IOException
     */
    static void writeDataGridToCsv(Path csvPath, List<List<String>> grid) throws IOException {
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(Files.newOutputStream(csvPath), Charset.forName("UTF8")), ',')) {
            for (List<String> gridRow : grid) {
                String[] row = new String[gridRow.size()];
                row = gridRow.toArray(row);
                writer.writeNext(row);
            }
        }
    }





}