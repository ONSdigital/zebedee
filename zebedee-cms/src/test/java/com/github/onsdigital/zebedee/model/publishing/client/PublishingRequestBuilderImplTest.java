package com.github.onsdigital.zebedee.model.publishing.client;

import org.apache.http.client.methods.HttpUriRequest;
import org.hamcrest.core.IsNull;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PublishingRequestBuilderImplTest {

    private PublishingRequestBuilder requestBuilder;

    @Before
    public void setUp() throws Exception {
        this.requestBuilder = new PublishingRequestBuilderImpl();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateGetContentHashRequest_hostNull() throws Exception {
        try {
            requestBuilder.createGetContentHashRequest(null, null, null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("host required for createGetContentHashRequest but none provided"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateGetContentHashRequest_hostEmpty() throws Exception {
        try {
            requestBuilder.createGetContentHashRequest("", null, null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("host required for createGetContentHashRequest but none provided"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateGetContentHashRequest_transactionIdNull() throws Exception {
        try {
            requestBuilder.createGetContentHashRequest("host", null, null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("transaction required for createGetContentHashRequest but none provided"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateGetContentHashRequest_transactionIdEmpty() throws Exception {
        try {
            requestBuilder.createGetContentHashRequest("host", "", null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("transaction required for createGetContentHashRequest but none provided"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateGetContentHashRequest_uriNull() throws Exception {
        try {
            requestBuilder.createGetContentHashRequest("host", "transactionId", null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("uri required for createGetContentHashRequest but none provided"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateGetContentHashRequest_uriEmpty() throws Exception {
        try {
            requestBuilder.createGetContentHashRequest("host", "transactionId", "");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("uri required for createGetContentHashRequest but none provided"));
            throw ex;
        }
    }

    @Test
    public void testCreateGetContentHashRequest_success() throws Exception {
        HttpUriRequest getRequest = requestBuilder.createGetContentHashRequest("http://localhost:8080",
                "transactionId", "uri");

        assertThat(getRequest.getURI().getHost(), equalTo("localhost"));
        assertThat(getRequest.getURI().getPath(), equalTo("/contentHash"));
        assertThat(getRequest.getURI().getQuery(), equalTo("transactionId=transactionId&uri=uri"));
        assertThat(getRequest.getFirstHeader("trace_id"), is(IsNull.notNullValue()));
    }
}
