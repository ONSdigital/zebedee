package com.github.onsdigital.zebedee.search.fastText.requests;

import com.github.onsdigital.zebedee.search.fastText.FastTextClient;
import com.github.onsdigital.zebedee.search.fastText.response.BatchSentenceVectorResponse;
import com.github.onsdigital.zebedee.search.fastText.response.SentenceVectorResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.http.HttpScheme;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BatchSentenceVectorRequest extends FastTextRequest<BatchSentenceVectorResponse> {

    private static final String PATH = "/supervised/sentence/vector/batch";

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

    public static void main(String[] args) throws Exception {
        final Map<String, String> query = new HashMap<String, String>() {{
            put(UUID.randomUUID().toString(), "rpi");
            put(UUID.randomUUID().toString(), "cpi");
            put(UUID.randomUUID().toString(), "murder");
            put(UUID.randomUUID().toString(), "homicide");
            put(UUID.randomUUID().toString(), "inflation");
        }};

        String requestId = UUID.randomUUID().toString();
        System.out.println(requestId);
        BatchSentenceVectorRequest request = new BatchSentenceVectorRequest(requestId, query);
        BatchSentenceVectorResponse response = request.call();

        Map<String, SentenceVectorResponse> results = response.getResults();
        for (String key : results.keySet()) {
            System.out.println(key);
            System.out.println(MAPPER.writeValueAsString(results.get(key)));
        }

        FastTextClient.getInstance().close();
    }
}
