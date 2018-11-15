package com.github.onsdigital.zebedee.search.fastText;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.IOException;

public class FastTextClient implements AutoCloseable {

    private static FastTextClient INSTANCE;

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

    public CloseableHttpResponse execute(HttpRequestBase requestBase) throws IOException {
        return this.httpClient.execute(requestBase);
    }

    private void configure(HttpClientBuilder customClientBuilder, ClientConfiguration configuration) {
        Integer connectionNumber = configuration.getMaxTotalConnection();
        if (connectionNumber != null) {
            connectionManager.setMaxTotal(connectionNumber);
            connectionManager.setDefaultMaxPerRoute(connectionNumber);
        }
        if (configuration.isDisableRedirectHandling()) {
            customClientBuilder.disableRedirectHandling();
        }
    }

    @Override
    public void close() throws Exception {
        httpClient.close();
    }

    private static ClientConfiguration createConfiguration() {
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setMaxTotalConnection(8);
        configuration.setDisableRedirectHandling(true);
        return configuration;
    }

    private class ShutdownHook extends Thread {
        @Override
        public void run() {
            try {
                if (httpClient != null) {
                    close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
