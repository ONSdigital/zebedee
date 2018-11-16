package com.github.onsdigital.zebedee.search.fastText.requests;

import com.github.onsdigital.zebedee.Configuration;
import com.github.onsdigital.zebedee.search.fastText.FastTextClient;
import com.github.onsdigital.zebedee.search.fastText.exceptions.FastTextServerError;
import com.github.onsdigital.zebedee.search.fastText.headers.JsonContentTypeHeader;
import com.github.onsdigital.zebedee.search.fastText.headers.RequestIdHeader;
import org.apache.commons.io.Charsets;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logInfo;

public abstract class FastTextRequest<T> implements Callable<T> {

    protected static final String HOST = Configuration.FastTextConfiguration.DP_FASTTEXT_HOST;
    static final ObjectMapper MAPPER = new ObjectMapper();

    private static final JsonContentTypeHeader JSON_CONTENT_TYPE_HEADER = new JsonContentTypeHeader();

    private final RequestIdHeader requestIdHeader;

    private Class<T> returnClass;
    private TypeReference<T> typeReference;

    private FastTextRequest(String requestId) {
        this.requestIdHeader = new RequestIdHeader(requestId);
    }

    public FastTextRequest(String requestId, Class<T> returnClass) {
        this(requestId);
        this.returnClass = returnClass;
    }

    public FastTextRequest(String requestId, TypeReference<T> typeReference) {
        this(requestId);
        this.typeReference = typeReference;
    }

    protected abstract URIBuilder uriBuilder();

    protected HttpGet get() throws URISyntaxException {
        HttpGet get = new HttpGet(this.uriBuilder().build());
        get.addHeader(this.requestIdHeader);
        get.addHeader(JSON_CONTENT_TYPE_HEADER);
        return get;
    }

    protected HttpPost post(Map<String, Object> params) throws IOException, URISyntaxException {
        HttpPost post = new HttpPost(this.uriBuilder().build());
        post.addHeader(this.requestIdHeader);
        post.addHeader(JSON_CONTENT_TYPE_HEADER);

        if (null != params) {
            String postParams = buildPostParams(params);
            StringEntity stringEntity = new StringEntity(postParams, Charsets.UTF_8);
            post.setEntity(stringEntity);
        }

        return post;
    }

    public abstract HttpRequestBase getRequestBase() throws IOException, URISyntaxException;

    public final String getRequestId() {
        return requestIdHeader.getRequestId();
    }

    @Override
    public T call() throws Exception {
        HttpRequestBase requestBase = this.getRequestBase();

        logInfo("Executing fastText request")
                .addParameter("method", requestBase.getMethod())
                .addParameter("requestId", this.requestIdHeader.getRequestId())
                .addParameter("uri", requestBase.getURI().toString())
                .log();

        try (CloseableHttpResponse response = FastTextClient.getInstance().execute(requestBase)) {
            String jsonResponse = EntityUtils.toString(response.getEntity());
            int code = response.getStatusLine().getStatusCode();

            if (code != HttpStatus.SC_OK) {
                FastTextServerError e = new FastTextServerError(jsonResponse, code, this.getRequestId());
                logError(e)
                        .addMessage("dp-fasttext returned non-200 response")
                        .addParameter("status", code)
                        .log();
                throw e;

            }

            // Either typeReference or returnClass are guaranteed to not be null
            if (this.typeReference != null) {
                return MAPPER.readValue(jsonResponse, this.typeReference);
            }

            return MAPPER.readValue(jsonResponse, this.returnClass);
        }
    }

    private static String buildPostParams(Map<String, Object> params) throws IOException {
        return MAPPER.writeValueAsString(params);
    }

}
