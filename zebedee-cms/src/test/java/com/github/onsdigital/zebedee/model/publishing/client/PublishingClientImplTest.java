package com.github.onsdigital.zebedee.model.publishing.client;

import com.google.gson.Gson;
import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PublishingClientImplTest {

    @Mock
    private PublishingRequestBuilder requestBuilder;

    @Mock
    private CloseableHttpClient httpClient;

    private PublishingClient client;
    private Supplier<CloseableHttpClient> clientSupplier;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        clientSupplier = () -> httpClient;
        this.client = new PublishingClientImpl(clientSupplier, requestBuilder);
    }

    @Test(expected = URISyntaxException.class)
    public void testGetContentHash_requestBuilderException() throws Exception {
        when(requestBuilder.createGetContentHashRequest(anyString(), anyString(), anyString()))
                .thenThrow(new URISyntaxException("Ka", "Booom"));

        client.getContentHash("", "", "");
    }

    @Test(expected = IOException.class)
    public void testGetContentHash_clientIOEx() throws Exception {
        HttpUriRequest request = mock(HttpUriRequest.class);

        when(requestBuilder.createGetContentHashRequest("host", "transactionId", "uri"))
                .thenReturn(request);

        when(httpClient.execute(request))
                .thenThrow(new IOException());

        client.getContentHash("host", "transactionId", "uri");
    }

    @Test(expected = ClientProtocolException.class)
    public void testGetContentHash_ClientProtocolException() throws Exception {
        HttpUriRequest request = mock(HttpUriRequest.class);

        when(requestBuilder.createGetContentHashRequest("host", "transactionId", "uri"))
                .thenReturn(request);

        when(httpClient.execute(request))
                .thenThrow(new ClientProtocolException());

        client.getContentHash("host", "transactionId", "uri");
    }

    @Test(expected = PublishingClientException.class)
    public void testGetContentHash_non200Status() throws Exception {
        HttpUriRequest request = mock(HttpUriRequest.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);

        when(requestBuilder.createGetContentHashRequest("host", "transactionId", "uri"))
                .thenReturn(request);

        when(httpClient.execute(request))
                .thenReturn(response);

        when(response.getCode())
                .thenReturn(400);


        try {
            client.getContentHash("host", "transactionId", "uri");
        } catch (PublishingClientException ex) {
            assertThat(ex.getHost(), equalTo("host"));
            assertThat(ex.getTransactionId(), equalTo("transactionId"));
            assertThat(ex.getUri(), equalTo("uri"));
            assertThat(ex.getHttpStatus(), equalTo(400));
            throw ex;
        }
    }

    @Test
    public void testGetContentHash_success() throws Exception {
        GetContentHashEntity expected = new GetContentHashEntity("uri", "transactionId", "1234567890");
        String expectedJson = new Gson().toJson(expected);

        try (InputStream responseBody = new ByteArrayInputStream(expectedJson.getBytes())) {
            HttpUriRequest request = mock(HttpUriRequest.class);
            when(requestBuilder.createGetContentHashRequest("host", "transactionId", "uri"))
                    .thenReturn(request);

            CloseableHttpResponse response = mock(CloseableHttpResponse.class);
            when(httpClient.execute(request))
                    .thenReturn(response);

            when(response.getCode())
                    .thenReturn(200);

            HttpEntity entity = mock(HttpEntity.class);
            when(response.getEntity())
                    .thenReturn(entity);

            when(entity.getContent())
                    .thenReturn(responseBody);

            GetContentHashEntity actual = client.getContentHash("host", "transactionId", "uri");

            assertThat(actual, equalTo(expected));
        }
    }
}
