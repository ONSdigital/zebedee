package com.github.onsdigital.zebedee.model.publishing.client;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
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
import static org.mockito.Matchers.anyString;
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
        MockitoAnnotations.initMocks(this);

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
        StatusLine statusLine = mock(StatusLine.class);

        when(requestBuilder.createGetContentHashRequest("host", "transactionId", "uri"))
                .thenReturn(request);

        when(httpClient.execute(request))
                .thenReturn(response);

        when(response.getStatusLine())
                .thenReturn(statusLine);

        when(statusLine.getStatusCode())
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

            StatusLine statusLine = mock(StatusLine.class);
            when(response.getStatusLine())
                    .thenReturn(statusLine);

            when(statusLine.getStatusCode())
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
