package com.github.onsdigital.zebedee.model.publishing.verify;

public class HashVerificationException extends RuntimeException {

    private String collectionId;
    private String host;
    private String transactionId;
    private String uri;

    public HashVerificationException(String message, String collectionId, String host, String transactionId,
                                     String uri) {
        super(message);
        this.collectionId = collectionId;
        this.host = host;
        this.transactionId = transactionId;
        this.uri = uri;
    }

    public HashVerificationException(String message, Throwable cause, String collectionId, String host,
                                     String transactionId, String uri) {
        super(message, cause);
        this.collectionId = collectionId;
        this.host = host;
        this.transactionId = transactionId;
        this.uri = uri;
    }

    public HashVerificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getCollectionId() {
        return this.collectionId;
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
}