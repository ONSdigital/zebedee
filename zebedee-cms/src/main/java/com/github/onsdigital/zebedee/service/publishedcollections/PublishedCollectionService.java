package com.github.onsdigital.zebedee.service.publishedcollections;

import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class PublishedCollectionService {

    private static final String REPORT_HOST = System.getenv().getOrDefault("PUBLISH_COLLECTION_HOST", "localhost");

    private static final short REPORT_PORT = Short.parseShort(System.getenv().getOrDefault("PUBLISH_COLLECTION_PORT", "9090"));

    private static final String PATH = "/publishedcollection";

    private final Gson gson = new Gson();

    public PublishedCollection[] getList() throws PublishedCollectionException {
        byte[] responseBytes;
        try {
            final HttpGet httpget = new HttpGet(buildUri(null));
            try (final CloseableHttpResponse execute = HttpClients.createDefault().execute(httpget)) {
                final HttpEntity entity = execute.getEntity();
                try(final InputStream content = entity.getContent()) {
                    responseBytes = IOUtils.toByteArray(content);
                }
            } catch (IOException e) {
                throw new PublishedCollectionException(e);
            }
        } catch (URISyntaxException e) {
            throw new PublishedCollectionException(e);
        }
         return gson.fromJson(new String(responseBytes), PublishedCollection[].class);
    }

    public PublishedCollection[] getCollection(final String collectionId) throws PublishedCollectionException {
        byte[] responseBytes;
        try {
            final HttpGet httpget = new HttpGet(buildUri(collectionId));
            try (final CloseableHttpResponse execute = HttpClients.createDefault().execute(httpget)) {
                final HttpEntity entity = execute.getEntity();
                try(final InputStream content = entity.getContent()) {
                    responseBytes = IOUtils.toByteArray(content);
                }
            } catch (IOException e) {
                throw new PublishedCollectionException(e);
            }
        } catch (URISyntaxException e) {
            throw new PublishedCollectionException(e);
        }
        final PublishedCollection pubCollection =  gson.fromJson(new String(responseBytes), PublishedCollection.class);
        final PublishedCollection[] results = new PublishedCollection[1];
        results[0] = pubCollection;
        return results;
    }

    private URI buildUri(final String collectionId) throws URISyntaxException {
        final URIBuilder uriBuilder = new URIBuilder()
                .setScheme("http")
                .setHost(REPORT_HOST)
                .setPort(REPORT_PORT);
        if (collectionId != null) {
            uriBuilder.setPath(PATH + "/" + collectionId);
        } else {
            uriBuilder.setPath(PATH);;
        }
        return uriBuilder.build();
    }
}
