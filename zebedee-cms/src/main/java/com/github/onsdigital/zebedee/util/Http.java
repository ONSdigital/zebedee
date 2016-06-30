package com.github.onsdigital.zebedee.util;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

public class Http extends com.github.davidcarboni.httpino.Http {

    public static void main(String[] args) {

        HttpPost post = new HttpPost("casded");

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(300000)
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .build();

        post.setConfig(requestConfig);

        CloseableHttpClient client = HttpClients
                .custom()
                .setDefaultRequestConfig(requestConfig).build();

        logDebug("testing").log();
    }

    protected CloseableHttpClient httpClient() {
        if (httpClient == null) {

            RequestConfig requestConfig = RequestConfig.custom()
                    .setSocketTimeout(5000)
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
