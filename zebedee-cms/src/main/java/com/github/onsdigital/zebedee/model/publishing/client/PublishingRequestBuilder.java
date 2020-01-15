package com.github.onsdigital.zebedee.model.publishing.client;

import org.apache.http.client.methods.HttpUriRequest;

import java.net.URISyntaxException;

/**
 * Defines the behaviour of a publishing API HTTP request builder.
 */
public interface PublishingRequestBuilder {

    /**
     * Create a new HTTP GET request to get the content hash for a given uri from the publishing API.
     *
     * @param host          the host address of the publishing API.
     * @param transactionId the ID of the publishing transaction the content belongs to.
     * @param uri           the uri of the content to get the file hash for
     * @return a {@link HttpUriRequest}.
     * @throws URISyntaxException       error creating the request.
     * @throws IllegalArgumentException thrown if input paramerters are invalid/null/empty.
     */
    HttpUriRequest createGetContentHashRequest(String host, String transactionId, String uri) throws URISyntaxException;
}
