package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.httpino.Endpoint;
import com.github.davidcarboni.httpino.Response;
import com.github.davidcarboni.httpino.Serialiser;
import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Extends existing HTTP implementation to support input streams without saving to disk.
 * Todo: merge this code into Httpino and remove this class.
 */
public class Http extends com.github.davidcarboni.httpino.Http {

    private CloseableHttpClient httpClient;

    public <T> Response<T> post(Endpoint endpoint, InputStream inputStream, String filename, Class<T> responseClass, NameValuePair... fields) throws IOException {

        // Create the request
        HttpPost post = new HttpPost(endpoint.url());

        // Add fields as text pairs
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        for (NameValuePair field : fields) {
            multipartEntityBuilder.addTextBody(field.getName(), field.getValue());
        }

        InputStreamBody body = new InputStreamBody(inputStream, filename);
        multipartEntityBuilder.addPart("file", body);

        // Set the body
        post.setEntity(multipartEntityBuilder.build());

        // Send the request and process the response
        try (CloseableHttpResponse response = httpClient().execute(post)) {
            T responseBody = deserialiseResponseMessage(response, responseClass);
            return new Response<>(response.getStatusLine(), responseBody);
        }
    }

    private CloseableHttpClient httpClient() {
        if (httpClient == null) {
            httpClient = HttpClients.createDefault();
        }
        return httpClient;
    }

    /**
     * Deserialises the given {@link CloseableHttpResponse} to the specified type.
     *
     * @param response      The response.
     * @param responseClass The type to deserialise to. This can be null, in which case {@link EntityUtils#consume(HttpEntity)} will be used to consume the response body (if any).
     * @param <T>           The type to deserialise to.
     * @return The deserialised response, or null if the response does not contain an entity.
     * @throws IOException If an error occurs.
     */
    private <T> T deserialiseResponseMessage(CloseableHttpResponse response, Class<T> responseClass) throws IOException {
        T body = null;

        HttpEntity entity = response.getEntity();
        if (entity != null && responseClass != null) {
            try (InputStream inputStream = entity.getContent()) {
                try {
                    body = Serialiser.deserialise(inputStream, responseClass);
                } catch (JsonSyntaxException e) {
                    // This can happen if an error HTTP code is received and the
                    // body of the response doesn't contain the expected object:
                    body = null;
                }
            }
        } else {
            EntityUtils.consume(entity);
        }

        return body;
    }
}
