package com.github.onsdigital.zebedee.model.publishing.verify;

/**
 * Exception type for errors occurring while attempting to verify collection content SHA-1 hash values.
 */
public class HashVerificationException extends RuntimeException {

    private String collectionId;
    private String host;
    private String transactionId;
    private String uri;

    /**
     * Construct a new instance.
     *
     * @param message       the context of the error.
     * @param collectionId  the unique ID of the {@link com.github.onsdigital.zebedee.model.Collection}.
     * @param host          the address of the publishing API the verification task was communicating with.
     * @param transactionId the unique publishing transaction ID for the publishing API.
     * @param uri           the uri of the content being verified.
     */
    public HashVerificationException(String message, String collectionId, String host, String transactionId,
                                     String uri) {
        super(message);
        this.collectionId = collectionId;
        this.host = host;
        this.transactionId = transactionId;
        this.uri = uri;
    }

    /**
     * Construct a new instance.
     *
     * @param message       the context of the error.
     * @param cause         the cause of the error.
     * @param collectionId  the unique ID of the {@link com.github.onsdigital.zebedee.model.Collection}.
     * @param host          the address of the publishing API the verification task was communicating with.
     * @param transactionId the unique publishing transaction ID for the publishing API.
     * @param uri           the uri of the content being verified.
     */
    public HashVerificationException(String message, Throwable cause, String collectionId, String host,
                                     String transactionId, String uri) {
        super(message, cause);
        this.collectionId = collectionId;
        this.host = host;
        this.transactionId = transactionId;
        this.uri = uri;
    }

    /**
     * Construct a new instance.
     *
     * @param message      the context of the error.
     * @param cause        the cause of the error.
     * @param collectionId the unique ID of the {@link com.github.onsdigital.zebedee.model.Collection}.
     */
    public HashVerificationException(String message, Throwable cause, String collectionId) {
        super(message, cause);
        this.collectionId = collectionId;
    }

    /**
     * Construct a new instance.
     *
     * @param message the context of the error.
     * @param cause   the cause of the error.
     */
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