package com.github.onsdigital.zebedee.util;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class Http extends com.github.davidcarboni.httpino.Http {

    protected CloseableHttpClient httpClient() {
        if (httpClient == null) {

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(300000)
                    .setConnectTimeout(5000)
                    .setConnectionRequestTimeout(5000)
                    .build();

            CloseableHttpClient client = HttpClients
                    .custom().setDefaultRequestConfig(requestConfig).build();

            httpClient = client;
        }
        return httpClient;
    }
}
