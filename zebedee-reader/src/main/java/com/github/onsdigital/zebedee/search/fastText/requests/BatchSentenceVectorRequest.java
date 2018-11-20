package com.github.onsdigital.zebedee.search.fastText.requests;

import com.github.onsdigital.zebedee.search.fastText.response.BatchSentenceVectorResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.http.HttpScheme;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class BatchSentenceVectorRequest extends FastTextRequest<BatchSentenceVectorResponse> {

    private static final String PATH = "/supervised/vector/batch";

    private final Map<String, String> batchQueries;

    public BatchSentenceVectorRequest(String requestId, Map<String, String> batchQueries) {
        super(requestId, BatchSentenceVectorResponse.class);

        this.batchQueries = batchQueries;
    }

    private Map<String, Object> getPostParams() {
        return new HashMap<String, Object>() {{
            put("queries", batchQueries);
        }};
    }

    @Override
    protected URIBuilder uriBuilder() {
        return new URIBuilder()
                .setScheme(HttpScheme.HTTP.asString())
                .setHost(HOST)
                .setPath(PATH);
    }

    @Override
    public HttpRequestBase getRequestBase() throws IOException, URISyntaxException {
        // Build the HTTP post request

        return super.post(this.getPostParams());
    }
}
