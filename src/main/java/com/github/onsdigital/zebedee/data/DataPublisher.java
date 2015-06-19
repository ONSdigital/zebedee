package com.github.onsdigital.zebedee.data;

import com.github.onsdigital.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.content.page.statistics.data.timeseries.TimeseriesDescription;
import com.github.onsdigital.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.content.page.statistics.dataset.DatasetDescription;
import com.github.onsdigital.content.partial.TimeseriesValue;
import com.github.onsdigital.content.util.ContentUtil;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by thomasridd on 04/06/15.
 */
public class DataPublisher {
    static final String BRIAN_KEY = "brian_url";
    public static int insertions = 0;
    public static int corrections = 0;
    public static Map<String, String> env = System.getenv();

    public static void preprocessCollection(Collection collection, Session session) throws IOException, BadRequestException, UnauthorizedException {

        if (env.get(BRIAN_KEY) == null || env.get(BRIAN_KEY).length() == 0) {
            System.out.println("Environment variable brian_url not set. Preprocessing step for " + collection.description.name + " skipped");
            return;
        }

        preprocessCSDB(collection, session);

        System.out.println(collection.description.name + " processed. Insertions: " + insertions + "      Corrections: " + corrections);
    }

    /**
     * The T5 timeseries objects are made by
     * 1. Searching for all .csdb files in a collection
     * 2. Getting Brian to break the files down to their component stats and basic metadata.
     * 3. Combining the stats with metadata entered with the dataset and existing data
     *
     * @param collection the collection to search for dataset objects
     * @param session
     * @throws IOException
     * @throws BadRequestException
     * @throws UnauthorizedException
     */
    static void preprocessCSDB(Collection collection, Session session) throws IOException, BadRequestException, UnauthorizedException {

        // First find all csdb files in the collection
        List<HashMap<String, Path>> csdbDatasetPages = csdbDatasetsInCollection(collection, session);

        // For each file in this collection
        for (HashMap<String, Path> csdbDataset : csdbDatasetPages) {

            // Download the dataset page (for metadata)
            Dataset dataset = ContentUtil.deserialise(FileUtils.openInputStream(csdbDataset.get("json").toFile()), Dataset.class);
            if (dataset.getDescription().getDatasetId() == null) {
                dataset.getDescription().setDatasetId(datasetIdFromDatafilePath(csdbDataset.get("file")));
            }

            // Break down the csdb file to timeseries (part-built by extracting csdb files)
            TimeSerieses serieses = callBrianToProcessCSDB(csdbDataset.get("file"));

            // Process each time series
            for (TimeSeries series : serieses) {

                // Work out the correct timeseries path by working back from the dataset uri
                String uri = uriForSeriesInDataset(dataset, series);
                Path path = collection.find("", uri);

                // Construct the new page
                TimeSeries newPage = constructTimeSeriesPageFromComponents(path, uri, dataset, series);

                // Save the new page to reviewed
                Path savePath = collection.autocreateReviewedPath(newPage.getUri() + "/data.json");
                IOUtils.write(ContentUtil.serialise(newPage), FileUtils.openOutputStream(savePath.toFile()));

                // Write csv and other files:
                // ...
            }
        }
    }

    static String datasetIdFromDatafilePath(Path path) {
        String[] sections = path.toString().split("/");
        sections = sections[sections.length - 1].split("\\.");
        return sections[0];
    }

    static String uriForSeriesInDataset(Dataset dataset, TimeSeries series) {
        String[] split = StringUtils.split(dataset.getUri().toString(), "/");
        split = (String[]) ArrayUtils.subarray(split, 0, split.length - 2);

        String uri = StringUtils.join(split, "/");
        uri = "/" + uri + "/timeseries/" + series.getCdid();

        return uri;
    }


    static TimeSeries constructTimeSeriesPageFromComponents(Path existingSeries, String uri, Dataset dataset, TimeSeries series) throws IOException {

        // Begin with existing data
        TimeSeries page = startPageForSeriesWithPublishedPath(uri, series);

//        if ((existingSeries != null) && Files.exists(existingSeries.resolve("data.json"))) {
//            System.out.println("Deserialising for " + existingSeries);
//            page = ContentUtil.deserialise(FileUtils.openInputStream(existingSeries.resolve("data.json").toFile()), TimeSeries.class);
//        } else {
//            page = new TimeSeries();
//            page.cdid = series.cdid;
//            page.uri = URI.create(uri);
//        }

        // Add stats data from the time series (as returned by Brian)
        // NOTE: This will log any corrections as it goes
        populatePageFromTimeSeries(page, series, dataset);

        // Add metadata from the dataset
        populatePageFromDataSetPage(page, dataset);

        return page;
    }

    //
    static TimeSeries startPageForSeriesWithPublishedPath(String uri, TimeSeries series) throws IOException {
        return startPageForSeriesWithPublishedPath(Root.zebedee, uri, series);
    }

    static TimeSeries startPageForSeriesWithPublishedPath(Zebedee zebedee, String uri, TimeSeries series) throws IOException {
        TimeSeries page;
        Path path = zebedee.published.toPath(uri);

        if ((path != null) && Files.exists(path.resolve("data.json"))) {
            page = ContentUtil.deserialise(FileUtils.openInputStream(path.resolve("data.json").toFile()), TimeSeries.class);
        } else {
            page = new TimeSeries();
            page.setDescription(new TimeseriesDescription());
            page.setCdid(series.getCdid());
            page.setUri(URI.create(uri));
        }

        return page;
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
     * @param collection
     * @param session
     * @param uri
     * @return
     * @throws IOException
     */
    static boolean fileIsCsdbFile(Collection collection, Session session, String uri) throws IOException {
        Path path = collection.find(session.email, uri);
        boolean confirmed = false;

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
     * Posts a csdb file to the brian Services/ConvertCSDB endpoint and deserialises the result
     * as a collection of file series objects
     *
     * @param path
     * @return a series of timeseries objects
     * @throws IOException
     */
    static TimeSerieses callBrianToProcessCSDB(Path path) throws IOException {
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
     * The URI for the Brian ConvertCSDB endpoint
     *
     * @return
     */
    static URI csdbURI() {
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
     * Takes a part-built TimeSeries page and populates the {@link TimeseriesValue} list using data gathered from a csdb file
     *
     * @param page    a part-built time series page
     * @param series  a time series returned by Brian by parsing a csdb file
     * @param dataset the source dataset page
     * @return
     */
    static TimeSeries populatePageFromTimeSeries(TimeSeries page, TimeSeries series, Dataset dataset) {

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
        if (page.getDescription().getTitle() == null) { page.getDescription().setTitle(series.getDescription().getTitle()); }

        return page;
    }

    // Support function for above
    static void populatePageFromSetOfValues(TimeSeries page, Set<TimeseriesValue> currentValues, Set<TimeseriesValue> updateValues, Dataset dataset) {

        // Iterate through values
        for (TimeseriesValue value : updateValues) {
            // Find the current value of the data point
            TimeseriesValue current = getCurrentValue(currentValues, value);

            if (current != null) { // A point already exists for this data

                if (!current.value.equalsIgnoreCase(value.value)) { // A point already exists for this data
                    // Take a copy
                    TimeseriesValue old = ContentUtil.deserialise(ContentUtil.serialise(current), TimeseriesValue.class);

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
     * If a {@link TimeseriesValue} for value.time exists in currentValues returns that.
     * Otherwise null
     *
     * @param currentValues a set of {@link TimeseriesValue}
     * @param value         a {@link TimeseriesValue}
     * @return a {@link TimeseriesValue} from currentValues
     */
    static TimeseriesValue getCurrentValue(Set<TimeseriesValue> currentValues, TimeseriesValue value) {
        if (currentValues == null) {
            return null;
        }

        for (TimeseriesValue current : currentValues) {
            if (current.compareTo(value) == 0) {
                return current;
            }
        }
        return null;
    }

    /**
     * @param series
     * @param oldValue
     * @param newValue
     */
    private static void logCorrection(TimeSeries series, TimeseriesValue oldValue, TimeseriesValue newValue) {
        // TODO: Important point to pick up when corrections have been made to timeseries values
        corrections += 1;
    }

    private static void logInsertion(TimeSeries series, TimeseriesValue newValue) {
        insertions += 1;
    }

    static TimeSeries populatePageFromDataSetPage(TimeSeries page, Dataset datasetPage) {
        TimeseriesDescription description = page.getDescription();
        if (description == null) {
            description = new TimeseriesDescription();
            page.setDescription(description);
        }
        description.setNextRelease(datasetPage.getDescription().getNextRelease());
        description.setReleaseDate(datasetPage.getDescription().getReleaseDate());

        // Add the dataset id if relevant
        boolean datasetIsNew = true;
        for (String datasetId : page.sourceDatasets) {
            if (datasetPage.getDescription().getDatasetId().equalsIgnoreCase(datasetId)) {
                datasetIsNew = false;
                break;
            }
        }
        if (datasetIsNew) {
            page.sourceDatasets.add(datasetPage.getDescription().getDatasetId());
        }

        return page;
    }

}