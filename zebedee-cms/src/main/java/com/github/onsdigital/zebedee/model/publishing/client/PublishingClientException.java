package com.github.onsdigital.zebedee.model.publishing.client;

import java.text.MessageFormat;

/**
 * Exception for errors occurring while sending requests and receiving responses to/from the publishing API.
 */
public class PublishingClientException extends RuntimeException {

    private String host;
    private String transactionId;
    private String uri;
    private int httpStatus;

    /**
     * Construct a new PublishingClientException.
     *
     * @param host          the host address of the publishing API the {@link PublishingClient} was
     *                      attempting to communticate with.
     * @param transactionId the publishing transaction ID.
     * @param uri           the uri of transaction content the error pertains too.
     * @param httpStatus    the http status code returned by the publishing API.
     */
    public PublishingClientException(String host, String transactionId, String uri, int httpStatus) {
        super("publishing client received an unexpected error");
        this.host = host;
        this.transactionId = transactionId;
        this.uri = uri;
        this.httpStatus = httpStatus;
    }

    public String getHost() {
        return this.host;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public String getUri() {
        return this.uri;
    }

    public int getHttpStatus() {
        return this.httpStatus;
    }

    @Override
    public String getMessage() {
        return MessageFormat.format("{0}, host: {1}, transactionId {2}, uri: {3}", super.getMessage(), host, transactionId, uri);
    }
}