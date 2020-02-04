package com.github.onsdigital.zebedee.model.publishing.client;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.function.Supplier;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

/**
 * Implementation of {@link PublishingClient}.
 */
public class PublishingClientImpl implements PublishingClient {

    private PublishingRequestBuilder requestBuilder;
    private Supplier<CloseableHttpClient> httpClientSupplier;

    /**
     * Constuct a new PublishingClientImpl instance using the default values.
     */
    public PublishingClientImpl() {
        this.httpClientSupplier = () -> HttpClients.createDefault();
        this.requestBuilder = new PublishingRequestBuilderImpl();
    }

    /**
     * Construct a new PublishingClientImpl instance.
     *
     * @param httpClientSupplier a {@link Supplier} returning a {@link CloseableHttpClient}.
     * @param requestBuilder     a {@link PublishingRequestBuilder} instance for creating HTTP requests to the publishing
     *                           API.
     */
    public PublishingClientImpl(Supplier<CloseableHttpClient> httpClientSupplier,
                                PublishingRequestBuilder requestBuilder) {
        this.httpClientSupplier = httpClientSupplier;
        this.requestBuilder = requestBuilder;
    }

    @Override
    public GetContentHashEntity getContentHash(String host, String transactionId, String uri) throws IOException,
            URISyntaxException {
        HttpUriRequest request = requestBuilder.createGetContentHashRequest(host, transactionId, uri);

        try (
                CloseableHttpClient client = httpClientSupplier.get();
                CloseableHttpResponse response = client.execute(request)
        ) {
/*            info().be(request.getR)
                    .response(response)
                    .uri(uri)
                    .host(host)
                    .transactionId(transactionId)
                    .log("response received from publishing API for get content SHA-1 hash");*/

            if (response.getStatusLine().getStatusCode() != 200) {
                throw non200ResponseStatusException(host, transactionId, uri, response.getStatusLine().getStatusCode());
            }
            return getResponseEntity(response.getEntity(), GetContentHashEntity.class);
        }
    }

    private <T> T getResponseEntity(HttpEntity entity, Class<T> tClass) throws IOException {
        try (
                InputStream inputStream = entity.getContent();
                InputStreamReader reader = new InputStreamReader(inputStream)
        ) {
            return new Gson().fromJson(reader, tClass);
        }
    }

    private PublishingClientException non200ResponseStatusException(String host, String transactionId, String uri,
                                                                    int status) {
        return new PublishingClientException("publishing API returned a non 200 status for get content hash request",
                host, transactionId, uri, status);
    }
}