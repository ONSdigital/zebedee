package com.github.onsdigital.zebedee.model.publishing.verify;

import com.github.onsdigital.zebedee.model.publishing.client.PublishingClient;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.Resource;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.concurrent.Callable;

import static java.util.Objects.requireNonNull;

/**
 * Task requests the SHA-1 file hash for a file in a publishing transaction from the Publishing API and
 * compares it to the hash generated from the local collection file - confirming the content was received
 * successfully and not corrupted.
 * <p></p>
 * Implements {@link Callable} and returns true if the hash was as expected
 * throws an {@link HashVerificationException} wrapped in an {@link java.util.concurrent.ExecutionException} otherwise.
 */
public class HashVerificationTask implements Callable<Boolean> {

    static final String HASH_INCORRECT_ERR = "file content hash from remote server did not match the expected value " +
            "expected {0}, actual {1}";

    static final String GENERATE_HASH_ERR = "error calculating content hash from local file";

    private String collectionID;
    private CollectionReader collectionReader;
    private String host;
    private String transactionId;
    private String uri;
    private PublishingClient publishingClient;

    /**
     * Construct a new instance from the {@link Builder} provided.
     */
    HashVerificationTask(Builder builder) {
        this.collectionID = requireNonNull(builder.getCollectionID());
        this.collectionReader = requireNonNull(builder.getReader());
        this.host = requireNonNull(builder.getPublishingAPIHost());
        this.transactionId = requireNonNull(builder.getTransactionId());
        this.uri = requireNonNull(builder.getUri());
        this.publishingClient = requireNonNull(builder.getPublishingClient());
    }

    /**
     * Verify the data receieved by the publishing API instance is correct. Retrive the SHA-1 file hash for the
     * content URI from publishing API instance, generate a SHA-1 hash from the collection file locally and compare.
     *
     * @return true if the hash values match, throws {@link HashVerificationException} if the hash is incorrect, there
     * was an error requesting the hash from the publishing API, or there was an error generating the local hash value.
     * @throws Exception thrown if the hash is incorrect or if there was any error while attempting to verify the
     *                   contentt.
     */
    @Override
    public Boolean call() throws Exception {
        String actual = getRemoteHashValue();
        String expected = getExpectedHashValue();

        if (StringUtils.equals(expected, actual)) {
            return true;
        }

        throw incorrectHashValueException(expected, actual);
    }

    private String getRemoteHashValue() {
        try {
            return publishingClient.getContentHash(host, transactionId, uri).getHash();
        } catch (Exception ex) {
            throw new HashVerificationException("http request to publishing API /getContentHash returned an error",
                    ex, collectionID, host, transactionId, uri);
        }
    }

    private String getExpectedHashValue() {
        try (
                Resource resource = collectionReader.getResource(uri);
                InputStream in = resource.getData();
                BufferedInputStream buf = new BufferedInputStream(in)
        ) {
            return DigestUtils.sha1Hex(buf);
        } catch (Exception ex) {
            throw new HashVerificationException(GENERATE_HASH_ERR, ex, collectionID, host, transactionId, uri);
        }
    }

    private HashVerificationException incorrectHashValueException(String expected, String actual) {
        String msg = MessageFormat.format(HASH_INCORRECT_ERR, expected, actual);
        throw new HashVerificationException(msg, collectionID, host, transactionId, uri);
    }

    public String getCollectionID() {
        return this.collectionID;
    }

    public CollectionReader getCollectionReader() {
        return this.collectionReader;
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

    public PublishingClient getPublishingClient() {
        return this.publishingClient;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        HashVerificationTask that = (HashVerificationTask) o;

        return new EqualsBuilder()
                .append(this.collectionID, that.collectionID)
                .append(this.collectionReader, that.collectionReader)
                .append(this.host, that.host)
                .append(this.transactionId, that.transactionId)
                .append(this.uri, that.uri)
                .append(this.publishingClient, that.publishingClient)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(this.collectionID)
                .append(this.collectionReader)
                .append(this.host)
                .append(this.transactionId)
                .append(this.uri)
                .append(this.publishingClient)
                .toHashCode();
    }

    /**
     * Class provides a <i>builder</i> style interface for creating a new {@link HashVerificationTask} instance.
     */
    public static class Builder {
        private String collectionID;
        private CollectionReader reader;
        private String publishingAPIHost;
        private String transactionId;
        private String uri;
        private PublishingClient publishingClient;

        /**
         * Set the collection ID of the content to verify.
         */
        public Builder collectionID(String collectionID) {
            this.collectionID = collectionID;
            return this;
        }

        /**
         * Set a {@link CollectionReader} implementation to read the collection content with.
         */
        public Builder collectionReader(CollectionReader reader) {
            this.reader = reader;
            return this;
        }

        /**
         * Set the publishing API host address to request the remote hash value from.
         */
        public Builder publishingAPIHost(String host) {
            this.publishingAPIHost = host;
            return this;
        }

        /**
         * Set the publishing transaction ID the remote file belongs to.
         */
        public Builder transactionId(String transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        /**
         * Set the URI of the content to verify.
         */
        public Builder contentURI(String uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Set the {@link PublishingClient} instance to use to communicate with the publishing API.
         */
        public Builder publishingClient(PublishingClient publishingClient) {
            this.publishingClient = publishingClient;
            return this;
        }

        /**
         * Construct a new {@link HashVerificationTask} instance.
         */
        public HashVerificationTask build() {
            return new HashVerificationTask(this);
        }

        public String getCollectionID() {
            return this.collectionID;
        }

        public CollectionReader getReader() {
            return this.reader;
        }

        public String getPublishingAPIHost() {
            return this.publishingAPIHost;
        }

        public String getTransactionId() {
            return this.transactionId;
        }

        public String getUri() {
            return this.uri;
        }

        public PublishingClient getPublishingClient() {
            return this.publishingClient;
        }
    }
}