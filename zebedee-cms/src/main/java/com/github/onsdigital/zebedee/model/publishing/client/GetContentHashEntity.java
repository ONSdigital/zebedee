package com.github.onsdigital.zebedee.model.publishing.client;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * GetContentHashEntity POJO representing the http response entity returned from the Publishing API get content hash
 * endpoint.
 */
public class GetContentHashEntity {

    private String uri;
    private String transactionId;
    private String hash;

    /**
     * Construct a new GetContentHashEntity
     *
     * @param uri           the uri of the content the hash belongs too.
     * @param transactionId the publishing transaction ID.
     * @param hash          the file hash value of the the content at the requested uri.
     */
    public GetContentHashEntity(final String uri, final String transactionId, final String hash) {
        this.uri = uri;
        this.transactionId = transactionId;
        this.hash = hash;
    }

    public String getUri() {
        return this.uri;
    }

    public String getTransactionId() {
        return this.transactionId;
    }

    public String getHash() {
        return this.hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        GetContentHashEntity entity = (GetContentHashEntity) o;

        return new EqualsBuilder()
                .append(getUri(), entity.getUri())
                .append(getTransactionId(), entity.getTransactionId())
                .append(getHash(), entity.getHash())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getUri())
                .append(getTransactionId())
                .append(getHash())
                .toHashCode();
    }
}
