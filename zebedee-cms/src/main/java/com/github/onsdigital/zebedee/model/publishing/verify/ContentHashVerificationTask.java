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

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static java.util.Objects.requireNonNull;

/**
 * Task requests the SHA-1 file hash for a file in a publishing transaction from the Publishing API and
 * compares it to the file hash generated from the local collection file - confirming the content was
 * received successfully and not corrupted.
 * <p></p>
 * Implements {@link Callable} and returns true if the hash was as expected
 * throws an {@link HashVerificationException} wrapped in an {@link java.util.concurrent.ExecutionException} otherwise.
 */
public class ContentHashVerificationTask implements Callable<Boolean> {

    static final String HASH_INCORRECT_ERR = "file content hash from remote server did not match the expected value " +
            "expected {0}, actual {1}";

    static final String GENERATE_HASH_ERR = "error calculating content hash from local file";

    private String collectionID;
    private CollectionReader collectionReader;
    private String host;
    private String transactionId;
    private String uri;
    private PublishingClient publishingClient;

    private ContentHashVerificationTask(Builder builder) {
        this.collectionID = requireNonNull(builder.getCollectionID());
        this.collectionReader = requireNonNull(builder.getReader());
        this.host = requireNonNull(builder.getPublishingAPIHost());
        this.transactionId = requireNonNull(builder.getTransactionId());
        this.uri = requireNonNull(builder.getUri());
        this.publishingClient = requireNonNull(builder.getPublishingClient());
    }

    @Override
    public Boolean call() throws Exception {
        String actual = publishingClient.getContentHash(host, transactionId, uri).getHash();
        String expected = getExpectedHashValue();

        if (StringUtils.equals(expected, actual)) {
            info().collectionID(collectionID)
                    .uri(uri)
                    .data("train_host", host)
                    .data("transaction_id", transactionId)
                    .log("content hash verified");
            return true;
        }

        throw incorrectHashValueException(expected, actual);
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

        ContentHashVerificationTask that = (ContentHashVerificationTask) o;

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
     * Class provides a <i>builder</i> style interface for creating a new {@link ContentHashVerificationTask} instance.
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
         * Construct a new {@link ContentHashVerificationTask} instance.
         */
        public ContentHashVerificationTask build() {
            return new ContentHashVerificationTask(this);
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