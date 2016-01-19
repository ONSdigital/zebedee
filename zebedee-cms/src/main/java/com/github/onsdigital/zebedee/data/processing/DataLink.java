package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.data.json.TimeSerieses;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.CollectionContentReader;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class DataLink {
    // Store env (for tests)
    public Map<String, String> env = System.getenv();

    /**
     * Post a csdb file to the brian Services/ConvertCSDB endpoint
     *
     * @param uri the path to the file that will be sent to brian
     * @param collectionReader a collectionReader
     *
     * @return a list of TimeSeries objects found by Brian
     * @throws IOException
     */
    public TimeSerieses callBrianToProcessCSDB(String uri, CollectionContentReader collectionReader) throws IOException, ZebedeeException {

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
    public TimeSerieses callBrianToProcessCSV(String uri, CollectionContentReader collectionReader) throws IOException, ZebedeeException {

        // Get the brian CSV processing uri
        URI url = csvURI();

        // Call brian
        return callBrian(uri, collectionReader, url);
    }


    private TimeSerieses callBrian(String fileUri, CollectionContentReader collectionContentReader, URI endpointUri) throws ZebedeeException, IOException {
        // Add csdb file as a binary
        HttpPost post = new HttpPost(endpointUri);
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();

        Resource resource = collectionContentReader.getResource(fileUri);
        InputStreamBody body = new InputStreamBody(resource.getData(), resource.getName());
        multipartEntityBuilder.addPart("file", body);

        post.setEntity(multipartEntityBuilder.build());

        // Post to the endpoint
        try (CloseableHttpResponse response = HttpClients.createDefault().execute(post)) {
            TimeSerieses result = null;
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
