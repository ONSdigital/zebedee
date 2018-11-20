package com.github.onsdigital.zebedee.search.fastText;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.search.fastText.requests.BatchSentenceVectorRequest;
import com.github.onsdigital.zebedee.search.fastText.requests.InfoRequest;
import com.github.onsdigital.zebedee.search.fastText.response.BatchSentenceVectorResponse;
import com.github.onsdigital.zebedee.search.fastText.response.InfoResponse;
import com.github.onsdigital.zebedee.search.fastText.response.SentenceVectorResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;

public class FastTextClient implements AutoCloseable {

    private static FastTextClient INSTANCE;

    private static final float CONFIDENCE_THRESHOLD = 0.5f;

    private final CloseableHttpClient httpClient;
    private final PoolingHttpClientConnectionManager connectionManager;

    public static FastTextClient getInstance() {
        if (INSTANCE == null) {
            synchronized (FastTextClient.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FastTextClient(createConfiguration());
                }
            }
        }
        return INSTANCE;
    }

    private FastTextClient(ClientConfiguration configuration) {
        HttpClientBuilder customClientBuilder = HttpClients.custom();
        this.connectionManager = new PoolingHttpClientConnectionManager();
        configure(customClientBuilder, configuration);
        this.httpClient = customClientBuilder.setConnectionManager(this.connectionManager)
                .build();
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    private Map<String, Future<BatchSentenceVectorResponse>> submitVectorQueries(Map<String, String> queries,
                                                                                 FastTextExecutorService executorService) {
        Map<String, Future<BatchSentenceVectorResponse>> futureMap = new HashMap<>();

        String requestId = UUID.randomUUID().toString();
        BatchSentenceVectorRequest request = new BatchSentenceVectorRequest(requestId, queries);

        Future<BatchSentenceVectorResponse> future = executorService.submit(request);
        // Add to futureMap
        futureMap.put(requestId, future);

        return futureMap;
    }

    /**
     * Builds queries for dp-fasttext
     * @return
     */
    private Map<String, Future<BatchSentenceVectorResponse>> buildQueries(List<Page> pages, int batchThreshold) throws Exception {
        // Init map to hold batch queries
        Map<String, String> queries = new HashMap<>();

        // Init map to hold futures
        Map<String, Future<BatchSentenceVectorResponse>> futureMap = new ConcurrentHashMap<>();

        // Prepare empty embedding vector
        int dimensions = this.getFastTextInfo().getDimensions();
        double[] emptyVector = new double[dimensions];
        String emptyEncodedVector = FastTextHelper.convertArrayToBase64(emptyVector);

        // Build and submit queries
        try (FastTextExecutorService executorService = FastTextExecutorService.getInstance()) {
            // Loop over pages
            for (Page page : pages) {
                String pageSentence = page.getPageSentence();
                if (null != pageSentence && !pageSentence.isEmpty() && page.getType() != PageType.timeseries) {
                    // Submit request
                    queries.put(page.getUri().toString(), pageSentence);
                } else {
                    // Set as empty vector
                    page.setEncodedEmbeddingVector(emptyEncodedVector);
                }

                if (queries.size() == batchThreshold) {
                    // Submit
                    futureMap.putAll(this.submitVectorQueries(queries, executorService));
                    queries = new HashMap<>();
                }
            }
            // Submit remaining queries
            if (!queries.isEmpty()) {
                futureMap.putAll(this.submitVectorQueries(queries, executorService));
            }
        }

        return futureMap;
    }

    /**
     * Sets the embedding vector field for a list of pages
     * @param pages
     * @param batchThreshold
     */
    public List<Page> generateEmbeddingVectors(List<Page> pages, int batchThreshold) throws Exception {
        // Init a map of uri to page
        Map<String, Page> pageMap = pages.stream()
                .collect(Collectors.toMap(x -> x.getUri().toString(), x -> x));

        Map<String, Future<BatchSentenceVectorResponse>> futureMap = this.buildQueries(pages, batchThreshold);

        // Collect results
        for (String requestId : futureMap.keySet()) {
            logDebug("Processing dp-fasttext request")
                    .addParameter("context", requestId)
                    .log();

            Future<BatchSentenceVectorResponse> future = futureMap.get(requestId);
            BatchSentenceVectorResponse response = future.get();

            // Loop through keys and set page vectors
            Map<String, SentenceVectorResponse> results = response.getResults();
            for (String uri : results.keySet()) {
                SentenceVectorResponse sentenceVectorResponse = results.get(uri);
                String encodedVector = sentenceVectorResponse.getVector();
                Map<String, Float> generatedKeywordsMap = sentenceVectorResponse.getKeywords();

                List<String> generatedKeywords = generatedKeywordsMap.entrySet().stream()
                        .filter(x -> x.getValue() >= CONFIDENCE_THRESHOLD)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

                // Set on page
                pageMap.get(uri).setEncodedEmbeddingVector(encodedVector);
                pageMap.get(uri).setGeneratedKeywords(generatedKeywords);
            }
        }

        // Return pages from map
        return new ArrayList<>(pageMap.values());
    }

    /**
     * Query dp-fasttext for model info
     * @return
     */
    public InfoResponse getFastTextInfo() throws Exception {
        String requestId = UUID.randomUUID().toString();
        InfoRequest request = new InfoRequest(requestId);
        InfoResponse response = request.call();

        return response;
    }

    public CloseableHttpResponse execute(HttpRequestBase requestBase) throws IOException {
        return this.httpClient.execute(requestBase);
    }

    private void configure(HttpClientBuilder customClientBuilder, ClientConfiguration configuration) {
        int connectionNumber = configuration.getMaxTotalConnection();
        this.connectionManager.setMaxTotal(connectionNumber);
        this.connectionManager.setDefaultMaxPerRoute(connectionNumber);
        if (configuration.isDisableRedirectHandling()) {
            customClientBuilder.disableRedirectHandling();
        }
    }

    @Override
    public void close() throws Exception {
        httpClient.close();
    }

    private static ClientConfiguration createConfiguration() {
        return new ClientConfiguration(8, true);
    }

    private class ShutdownHook extends Thread {
        @Override
        public void run() {
            try {
                if (httpClient != null) {
                    close();
                }
            } catch (Exception e) {
                logError(e)
                        .addMessage("Caught exception closing HTTP client")
                        .log();
            }
        }
    }

}
