package com.github.onsdigital.zebedee.model.publishing.client;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Defines the interface for HTTP client for the website publising API.
 */
public interface PublishingClient {

    /**
     * Get the file hash for the content at the specified uri from the publishing API.
     *
     * @param host          the host address of the publishing API.
     * @param transactionId the publishing transaction ID which the content belongs to.
     * @param uri           the uri of the content to get the hash for.
     * @return {@link GetContentHashEntity} with the hash value.
     * @throws IOException        problem executing the request.
     * @throws URISyntaxException problem creating the request.
     */
    GetContentHashEntity getContentHash(String host, String transactionId, String uri) throws IOException,
            URISyntaxException;
}
