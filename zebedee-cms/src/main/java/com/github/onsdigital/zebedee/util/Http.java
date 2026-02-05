package com.github.onsdigital.zebedee.util;

import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;

public class Http extends com.github.davidcarboni.httpino.Http {

    protected CloseableHttpClient httpClient() {
        if (httpClient == null) {

            ConnectionConfig connectionConfig = ConnectionConfig.custom()
                    .setConnectTimeout(5000, TimeUnit.MILLISECONDS)
                    .setSocketTimeout(300000, TimeUnit.MILLISECONDS)
                    .build();

            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(5000, TimeUnit.MILLISECONDS)
                    .build();

            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setDefaultConnectionConfig(connectionConfig);

            CloseableHttpClient client = HttpClients
                    .custom()
                    .setDefaultRequestConfig(requestConfig)
                    .setConnectionManager(connectionManager)
                    .build();

            httpClient = client;
        }
        return httpClient;
    }
}
