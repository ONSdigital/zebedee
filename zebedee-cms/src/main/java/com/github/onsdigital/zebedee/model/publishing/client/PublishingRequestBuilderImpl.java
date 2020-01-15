package com.github.onsdigital.zebedee.model.publishing.client;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;

/**
 * Provides functionality for building HTTP requests to the publishing API.
 */
public class PublishingRequestBuilderImpl implements PublishingRequestBuilder {

    private static final String TRANSACTION_ID_PARAM = "transactionId";

    private static final String URI_PARAM = "uri";

    private static final String GET_CONTENT_HASH_URI = "/contentHash";

    @Override
    public HttpUriRequest createGetContentHashRequest(String host, String transactionId, String uri) throws URISyntaxException {
        validateGetContentHashRequestParams(host, transactionId, uri);

        HttpGet httpGet = new HttpGet(host + GET_CONTENT_HASH_URI);

        httpGet.setURI(new URIBuilder(httpGet.getURI())
                .setParameter(TRANSACTION_ID_PARAM, transactionId)
                .addParameter(URI_PARAM, uri)
                .build());

        return httpGet;
    }

    private void validateGetContentHashRequestParams(String host, String transactionId, String uri) {
        if (StringUtils.isEmpty(host)) {
            throw new IllegalArgumentException("host required for createGetContentHashRequest but none provided");
        }

        if (StringUtils.isEmpty(transactionId)) {
            throw new IllegalArgumentException("transaction required for createGetContentHashRequest but none provided");
        }

        if (StringUtils.isEmpty(uri)) {
            throw new IllegalArgumentException("uri required for createGetContentHashRequest but none provided");
        }
    }
}
