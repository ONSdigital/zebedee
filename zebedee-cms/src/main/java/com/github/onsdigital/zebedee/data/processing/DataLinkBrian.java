package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.data.json.ApiError;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.TimeSeriesConversionException;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.google.gson.JsonSyntaxException;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.InputStreamBody;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;

public class DataLinkBrian implements DataLink {
    // Store env (for tests)
    public Map<String, String> env = System.getenv();

    /**
     * Post a csdb file to the brian Services/ConvertCSDB endpoint
     *
     * @param uri              the path to the file that will be sent to brian
     * @param collectionReader a collectionReader
     * @return a list of TimeSeries objects found by Brian
     * @throws IOException
     */
    public TimeSerieses callCSDBProcessor(String uri, ContentReader collectionReader) throws IOException, ZebedeeException {

        // Get the brian CSDB processing uri
        URI url = csdbURI();
        return callBrian(uri, collectionReader, url);

    }

    /**
     * Post a csv file to the brian Services/ConvertCSV endpoint
     *
     * @param uri
     * @param collectionReader
     * @return
     * @throws IOException
     */
    public TimeSerieses callCSVProcessor(String uri, ContentReader collectionReader) throws IOException, ZebedeeException {

        // Get the brian CSV processing uri
        URI url = csvURI();

        // Call brian
        return callBrian(uri, collectionReader, url);
    }


    private TimeSerieses callBrian(String fileUri, ContentReader contentReader, URI endpointUri) throws ZebedeeException, IOException {
        try (
                Resource resource = contentReader.getResource(fileUri);
                InputStream input = resource.getData()
        ) {
            return getTimeSeries(endpointUri, input, resource.getName());
        }
    }

    public TimeSerieses getTimeSeries(URI endpointUri, InputStream input, String name) throws IOException, ZebedeeException {
        // Add csdb file as a binary
        HttpPost post = new HttpPost(endpointUri);
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        InputStreamBody body = new InputStreamBody(input, name);
        multipartEntityBuilder.addPart("file", body);

        post.setEntity(multipartEntityBuilder.build());

        // Post to the endpoint
        try (
                CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(post)
        ) {
            switch (response.getCode()) {
                case HttpStatus.SC_OK:
                    return decodeBodyFromResponse(response, TimeSerieses.class);
                case HttpStatus.SC_BAD_REQUEST:
                    error().data("api_error", decodeBodyFromResponse(response, ApiError.class))
                            .data("filename", name).log("error sending time series file to brian");
                    throw new TimeSeriesConversionException("error with time series file sent to brian");
                default:
                    error().data("api_error", decodeBodyFromResponse(response, ApiError.class))
                            .data("filename", name).log("unexpected error sending time series file to brian");
                    throw new UnexpectedErrorException("unexpected error sending time series file to brian", HttpStatus.SC_SERVICE_UNAVAILABLE);
            }
        }
    }

    private <T> T decodeBodyFromResponse(CloseableHttpResponse response, Class<T> type) throws IOException {
        T result = null;
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try (InputStream inputStream = entity.getContent()) {
                try {
                    result = ContentUtil.deserialise(inputStream, type);
                } catch (JsonSyntaxException e) {
                    // This can happen if the body of the response doesn't contain the expected object:
                    result = null;
                }
            }
        } else {
            EntityUtils.consume(entity);
        }
        return result;
    }

    /**
     * Get the URL for the Brian ConvertCSDB endpoint
     *
     * @return
     */
    URI csdbURI() {
        String endpoint = "/Services/ConvertCSDB";

        return getBrianUri(endpoint);
    }

    /**
     * Get the URL for the Brian ConvertCSV endpoint
     *
     * @return
     */
    URI csvURI() {
        String endpoint = "/Services/ConvertCSV";

        return getBrianUri(endpoint);
    }

    private URI getBrianUri(String endpoint) {
        String csdbURL = "";
        if (env.containsKey("brian_url")) {
            csdbURL = env.get("brian_url") + endpoint;
        } else {
            csdbURL = Configuration.getBrianUrl() + endpoint;
        }

        URI url = null;
        try {
            URIBuilder uriBuilder = new URIBuilder(csdbURL);
            url = uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Data services URL not found: " + csdbURL);
        }
        return url;
    }

}
