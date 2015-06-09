package com.github.onsdigital.zebedee.data;

import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.data.json.DatasetPage;
import com.github.onsdigital.zebedee.data.json.TimeSeriesObject;
import com.github.onsdigital.zebedee.data.json.TimeSeriesObjects;
import com.github.onsdigital.zebedee.data.json.TimeseriesPage;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by thomasridd on 04/06/15.
 */
public class DataPublisher {
    public static Map<String, String> env = System.getenv();

    public static void preprocessCollection(Collection collection, Session session) throws IOException, BadRequestException, UnauthorizedException {
        System.out.println("Hello");

        preprocessCSDB(collection, session);
    }

    /**
     *
     * The T5 timeseries objects are made by
     *      1. Searching for all .csdb files in a collection
     *      2. Getting Brian to break the files down to their component stats and basic metadata.
     *      3. Combining the stats with metadata entered with the dataset and existing data
     *
     * @param collection the collection to search for dataset objects
     * @param session
     * @throws IOException
     * @throws BadRequestException
     * @throws UnauthorizedException
     */
    private static void preprocessCSDB(Collection collection, Session session) throws IOException, BadRequestException, UnauthorizedException {
        List<HashMap<String, Path>> datasets = csdbDatasetsInCollection(collection, session);
        for(HashMap<String, Path> dataset: datasets) {
            DatasetPage datasetPage = Serialiser.deserialise(FileUtils.openInputStream(dataset.get("json").toFile()), DatasetPage.class);

            TimeSeriesObjects serieses = callBrianToProcessCSDB(dataset.get("file"));
            for(TimeSeriesObject series: serieses) {

                String uri = datasetPage.uri + "/" + series.taxi;
                Path path = collection.reviewed.toPath(uri).resolve("data.json");

                // Begin with existing data if possible (this will preserve any manually entered data)
                TimeseriesPage page = new TimeseriesPage();
                if (Files.exists(path)) {
                    page = Serialiser.deserialise(FileUtils.openInputStream(path.toFile()), TimeseriesPage.class);
                }

                // Add stats data from the time series (as returned by Brian)
                populatePageFromTimeSeries(page, series);

                // Add metadata from the dataset
                populatePageFromDataSetPage(page, datasetPage);

                // We want to copy our new series file to the reviewed section for the uri
                Path savePath = collection.autocreatePath(uri).resolve("data.json");
                IOUtils.write(Serialiser.serialise(series), FileUtils.openOutputStream(savePath.toFile()));

                // Write csv and other files:
                // ...
            }
        }
    }
    private static List<HashMap<String, Path>> csdbDatasetsInCollection(Collection collection, Session session) throws IOException {
        List<String> csdbUris = new ArrayList<>();
        List<HashMap<String, Path>> results = new ArrayList<>();

        for(String uri: collection.reviewedUris()) {
            if(uri.endsWith(".csdb")) {
                csdbUris.add(uri);
            }
        }
        for (String csdbUri: csdbUris) {
            Path csdbPath = collection.find(session.email, csdbUri);
            if (Files.exists(csdbPath)) {
                Path jsonPath = csdbPath.getParent().resolve("data.json");
                if(Files.exists(jsonPath)) {
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
     *
     * Posts a csdb file to the brian Services/ConvertCSDB endpoint and deserialises the result
     * as a collection of file series objects
     *
     * @param path
     * @return
     * @throws IOException
     */
    private static TimeSeriesObjects callBrianToProcessCSDB(Path path) throws IOException {
        URI url = csdbURI();

        HttpPost post = new HttpPost(url);

        // Add csdb file as a binary
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        FileBody bin = new FileBody(path.toFile());
        multipartEntityBuilder.addPart("file", bin);

        post.setEntity(multipartEntityBuilder.build());

       // Post to the endpoint
        try (CloseableHttpResponse response = HttpClients.createDefault().execute(post)) {
            TimeSeriesObjects result = null;

            //
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                try (InputStream inputStream = entity.getContent()) {
                    try {
                        result = Serialiser.deserialise(inputStream, TimeSeriesObjects.class);
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
    private static URI csdbURI() {
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

//    /**
//     * Sends a POST request with a file and returns the response.
//     *
//     * @param endpoint      The endpoint to send the request to.
//     * @param file          The file to upload
//     * @param responseClass The class to deserialise the Json response to. Can be null if no response message is expected.
//     * @param fields        Any name-value pairs to serialise
//     * @param <T>           The type to deserialise the response to.
//     * @return A {@link Response} containing the deserialised body, if any.
//     * @throws IOException If an error occurs.
//     * @see MultipartEntityBuilder
//     */
//    public <T> Response<T> post(Endpoint endpoint, File file, Class<T> responseClass, NameValuePair... fields) throws IOException {
//        if (file == null) {
//            return post(endpoint, responseClass, fields);
//        } // deal with null case
//
//        // Create the request
//        HttpPost post = new HttpPost(endpoint.url());
//        post.setHeaders(combineHeaders());
//
//        // Add fields as text pairs
//        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
//        for (NameValuePair field : fields) {
//            multipartEntityBuilder.addTextBody(field.getName(), field.getValue());
//        }
//        // Add file as binary
//        FileBody bin = new FileBody(file);
//        multipartEntityBuilder.addPart("file", bin);
//
//        // Set the body
//        post.setEntity(multipartEntityBuilder.build());
//
//        // Send the request and process the response
//        try (CloseableHttpResponse response = httpClient().execute(post)) {
//            T body = deserialiseResponseMessage(response, responseClass);
//            return new Response<>(response.getStatusLine(), body);
//        }
//    }

    private static TimeseriesPage populatePageFromTimeSeries(TimeseriesPage page, TimeSeriesObject seriesObject) {
        return null;
    }
    private static TimeseriesPage populatePageFromDataSetPage(TimeseriesPage page, DatasetPage datasetPage) {
        return null;
    }

}
