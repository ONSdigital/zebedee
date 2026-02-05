package com.github.onsdigital.zebedee.verification.http;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.HttpResponseException;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.pool.ConnPoolControl;
import org.apache.hc.core5.util.TimeValue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

/**
 * Created by bren on 22/07/15.
 * <p>
 * http client to a single host with connection pool and  cache functionality.
 */
//Add post,put,etc. functionality if needed
public class PooledHttpClient {

    private final PoolingHttpClientConnectionManager connectionManager;
    private final CloseableHttpClient httpClient;
    private final IdleConnectionMonitorThread monitorThread;
    private final URI HOST;

    public PooledHttpClient(String host, ClientConfiguration configuration) {
        HOST = resolveHostUri(host);
        this.connectionManager = new PoolingHttpClientConnectionManager();
        HttpClientBuilder customClientBuilder = HttpClients.custom();
        configure(customClientBuilder, configuration);
        httpClient = customClientBuilder.setConnectionManager(connectionManager)
                .build();
        this.monitorThread = new IdleConnectionMonitorThread(connectionManager);
        this.monitorThread.start();
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
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

    private URI resolveHostUri(String host) {
        URI givenHost = URI.create(host);
        URIBuilder builder = new URIBuilder();
        if (StringUtils.startsWithIgnoreCase(host, "http")) {
            builder.setScheme(givenHost.getScheme());
            builder.setHost(givenHost.getHost());
            builder.setPort(givenHost.getPort());
            builder.setPath(givenHost.getPath());
            builder.setUserInfo(givenHost.getUserInfo());
        } else {
            builder.setScheme("http");
            builder.setHost(host);
        }
        try {
            return builder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @param path            path, should not contain any query string, only path info
     * @param headers         key-value map to to be added to request as headers
     * @param queryParameters query parameters to be sent as get query string
     * @return
     * @throws IOException             All exceptions thrown are IOException implementations
     * @throws ClientProtocolException for protocol related exceptions, HttpResponseExceptions are a subclass of this exception type
     * @throws HttpResponseException   exception for http status code > 300, HttpResponseException is a subclass of IOException
     *                                 catch HttpResponseException for  status code
     */
    public CloseableHttpResponse sendGet(String path, Map<String, String> headers, List<NameValuePair> queryParameters) throws IOException {
        URI uri = buildGetUri(path, queryParameters);
        HttpGet request = new HttpGet(uri);
        addHeaders(headers, request);
        return validate(httpClient.execute(request));
    }


    /**
     * @param path           path, should not contain any query string, only path info
     * @param headers        key-value map to to be added to request as headers
     * @param postParameters query parameters to be sent as get query string
     * @return
     * @throws IOException             All exceptions thrown are IOException implementations
     * @throws ClientProtocolException for protocol related exceptions, HttpResponseExceptions are a subclass of this exception type
     * @throws HttpResponseException   exception for http status code > 300, HttpResponseException is a subclass of IOException
     *                                 catch HttpResponseException for  status code
     */
    public CloseableHttpResponse sendPost(String path, Map<String, String> headers, List<NameValuePair> postParameters) throws IOException {
        URI uri = buildPath(path);
        HttpPost request = new HttpPost(uri);
        addHeaders(headers, request);
        if (postParameters != null) {
            request.setEntity(new UrlEncodedFormEntity(postParameters));
        }
        return validate(httpClient.execute(request));
    }

    public CloseableHttpResponse sendPost(String path, Map<String, String> headers, String content) throws IOException {
        URI uri = buildPath(path);
        HttpPost request = new HttpPost(uri);
        addHeaders(headers, request);

        request.setEntity(new StringEntity(content));
        return validate(httpClient.execute(request));
    }

    private void addHeaders(Map<String, String> headers, HttpUriRequestBase request) {
        if (headers != null) {
            Iterator<Map.Entry<String, String>> headerIterator = headers.entrySet().iterator();
            while (headerIterator.hasNext()) {
                Map.Entry<String, String> next = headerIterator.next();
                request.addHeader(next.getKey(), next.getValue());
            }

        }
    }

    public void shutdown() throws IOException {
        info().data("host", HOST).log("Shutting down connection pool");
        httpClient.close();
        info().log("Successfully shut down connection pool");
        monitorThread.shutdown();
    }

    private URI buildPath(String path) {
        URIBuilder uriBuilder = newUriBuilder(path);
        try {
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid uri! " + HOST + path);
        }
    }

    private URIBuilder newUriBuilder(String path) {
        URIBuilder uriBuilder = new URIBuilder(HOST);
        String uri = StringUtils.defaultIfEmpty(uriBuilder.getPath(), "");
        uriBuilder.setPath((uri + "/" + path).replaceAll("//+", "/"));
        return uriBuilder;
    }


    private URI buildGetUri(String path, List<NameValuePair> queryParameters) {
        try {
            URIBuilder uriBuilder = newUriBuilder(path);
            if (queryParameters != null) {
                uriBuilder.setParameters(queryParameters);
            }
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid uri! " + HOST + path);
        }
    }

    /**
     * Throws appropriate errors if response is not successful
     */
    private CloseableHttpResponse validate(CloseableHttpResponse response) throws ClientProtocolException {
        int statusCode = response.getCode();
        HttpEntity entity = response.getEntity();
        if (statusCode > 302) {
            String errorMessage = getErrorMessage(entity);
            throw new HttpResponseException(
                    statusCode,
                    errorMessage == null ? response.getReasonPhrase() : errorMessage);
        }
        if (entity == null) {
            throw new ClientProtocolException("Response contains no content");
        }

        return response;
    }

    private String getErrorMessage(HttpEntity entity) {
        try {
            String s = EntityUtils.toString(entity);
            return s;
        } catch (Exception e) {
            error().logException(e, "Failed reading content service");
        }
        return null;
    }




    //Based on tuorial code on https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d5e393

    //Http client already tests connection to see if it is stale before making a request, but documents suggests using monitor thread since it is 100% reliable
    private class IdleConnectionMonitorThread extends Thread {

        private boolean shutdown;
        private ConnPoolControl<?> connMgr;

        public IdleConnectionMonitorThread(ConnPoolControl<?> connMgr) {
            super();
            this.connMgr = connMgr;
        }

        @Override
        public void run() {
            info().log("Running connection pool monitor");
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(5000);
                        // Close expired connections every 5 seconds
                        connMgr.closeExpired();
                        // Close connections
                        // that have been idle longer than 30 sec
                        connMgr.closeIdle(TimeValue.ofSeconds(60));
                    }
                }
            } catch (InterruptedException ex) {
                error().logException(ex, "Connection pool monitor failed");
                ex.printStackTrace();
            }
        }

        public void shutdown() {
            info().log("Shutting down connection pool monitor");
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }

    }


    private class ShutdownHook extends Thread {
        @Override
        public void run() {
            try {
                if (httpClient != null) {
                    shutdown();
                }
            } catch (IOException e) {
                error().data("host", HOST).logException(e, "Falied shutting down http client");
                e.printStackTrace();
            }
        }
    }
}
