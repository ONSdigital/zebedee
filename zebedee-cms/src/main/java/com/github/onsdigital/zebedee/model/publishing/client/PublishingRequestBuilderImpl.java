package com.github.onsdigital.zebedee.model.publishing.client;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.MDC;

import java.net.URISyntaxException;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * Provides functionality for building HTTP requests to the publishing API.
 */
public class PublishingRequestBuilderImpl implements PublishingRequestBuilder {

    private static final String TRANSACTION_ID_PARAM = "transactionId";

    private static final String URI_PARAM = "uri";

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final String TRACE_ID_HEADER = "trace_id";

    private static final String GET_CONTENT_HASH_URI = "/contentHash";

    @Override
    public HttpUriRequest createGetContentHashRequest(String host, String transactionId, String uri) throws URISyntaxException {
        validateGetContentHashRequestParams(host, transactionId, uri);

        HttpGet httpGet = new HttpGet(host + GET_CONTENT_HASH_URI);

        httpGet.setUri(new URIBuilder(httpGet.getUri())
                .setParameter(TRANSACTION_ID_PARAM, transactionId)
                .addParameter(URI_PARAM, uri)
                .build());

        httpGet.setHeader(getTraceIDHeader());

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

    private Header getTraceIDHeader() {
        return new BasicHeader(REQUEST_ID_HEADER, getTraceID());
    }

    /**
     * Get the trace ID to use as for the request header. If a value exists in the {@link MDC} map use that otherwise
     * generate a new random value.
     *
     * @return the trace ID to use for the request header.
     */
    private String getTraceID() {
        return defaultIfBlank(MDC.get(TRACE_ID_HEADER), UUID.randomUUID().toString());
    }
}
