package com.github.onsdigital.zebedee.model.publishing.verify;

import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.model.publishing.client.GetContentHashEntity;
import com.github.onsdigital.zebedee.model.publishing.client.PublishingClient;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.Resource;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HashVerifierImplTest {

    @Mock
    private PublishingClient publishingClient;

    @Mock
    private Collection collection;

    @Mock
    private CollectionDescription description;

    @Mock
    private CollectionReader reader;

    @Mock
    private Content reviewed;

    private HashVerifier hashVerifier;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        hashVerifier = new HashVerifierImpl(publishingClient);
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyTransactionContent_collectionIsNull_ExceptionThrown() throws Exception {
        try {
            hashVerifier.verifyTransactionContent(null, null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("collection required but was null"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyTransactionContent_collectionReaderIsNull_ExceptionThrown() throws Exception {
        when(collection.getDescription())
                .thenReturn(description);

        try {
            hashVerifier.verifyTransactionContent(collection, null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("collection reader required but was null"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyTransactionContent_collectionDescriptionIsNull_ExceptionThrown() throws Exception {
        when(collection.getDescription())
                .thenReturn(null);

        try {
            hashVerifier.verifyTransactionContent(collection, reader);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("collection.description required but was null"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyTransactionContent_collectionDescriptionTransactionsIdsNull_ExceptionThrown() throws Exception {
        when(collection.getDescription())
                .thenReturn(description);

        when(description.getPublishTransactionIds())
                .thenReturn(null);

        try {
            hashVerifier.verifyTransactionContent(collection, reader);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("description.publishingTransactionIds required but was null"));
            throw ex;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyTransactionContent_HostTransactionMapEmpty_ExceptionThrown() throws Exception {
        when(collection.getDescription())
                .thenReturn(description);

        when(description.getPublishTransactionIds())
                .thenReturn(new HashMap<>());

        try {
            hashVerifier.verifyTransactionContent(collection, reader);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), equalTo("error verifying transaction content: host-to-transactionId mapping expected but none found"));
            throw ex;
        }
    }

    @Test(expected = HashVerificationException.class)
    public void verifyTransactionContent_RewiewedGetUris_IOException() throws Exception {
        when(collection.getId())
                .thenReturn("666");

        when(collection.getDescription())
                .thenReturn(description);

        when(description.getPublishTransactionIds())
                .thenReturn(new HashMap<String, String>() {{
                    put("localhost", "666");
                }});

        when(collection.getReviewed())
                .thenReturn(reviewed);

        IOException expected = new IOException("bork");
        when(reviewed.uris())
                .thenThrow(expected);

        try {
            hashVerifier.verifyTransactionContent(collection, reader);
        } catch (HashVerificationException ex) {
            assertThat(ex.getMessage(), equalTo("error getting collection reviewed uris"));
            assertThat(ex.getCollectionId(), equalTo("666"));
            assertThat(ex.getCause(), equalTo(expected));
            throw ex;
        }
    }

    @Test(expected = HashVerificationException.class)
    public void verifyTransactionContent_ContentHashVerificationTask_ExecutionException() throws Exception {
        when(collection.getId())
                .thenReturn("666");

        when(collection.getDescription())
                .thenReturn(description);

        when(description.getPublishTransactionIds())
                .thenReturn(new HashMap<String, String>() {{
                    put("localhost", "666");
                }});

        when(collection.getReviewed())
                .thenReturn(reviewed);

        when(reviewed.uris()).thenReturn(new ArrayList<String>() {{
            add("/a/b/c/data.json");
        }});

        RuntimeException expected = new RuntimeException("wibble");
        when(publishingClient.getContentHash(anyString(), anyString(), anyString()))
                .thenThrow(expected);

        try {
            hashVerifier.verifyTransactionContent(collection, reader);
        } catch (HashVerificationException ex) {
            assertThat(ex.getMessage(), equalTo("http request to publishing API /getContentHash returned an error"));
            assertThat(ex.getHost(), equalTo("localhost"));
            assertThat(ex.getTransactionId(), equalTo("666"));
            assertThat(ex.getUri(), equalTo("/a/b/c/data.json"));
            assertThat(ex.getCause(), equalTo(expected));
            throw ex;
        }
    }

    @Test(expected = HashVerificationException.class)
    public void verifyTransactionContent_HashValueMismatch_ExecutionException() throws Exception {
        String uri = "/a/b/c/data.json";
        String host = "localhost";
        String transactionId = "666";
        String collectionId = "666";

        when(collection.getId())
                .thenReturn(collectionId);

        when(collection.getDescription())
                .thenReturn(description);

        when(description.getPublishTransactionIds())
                .thenReturn(new HashMap<String, String>() {{
                    put(host, transactionId);
                }});

        when(collection.getReviewed())
                .thenReturn(reviewed);

        when(reviewed.uris()).thenReturn(new ArrayList<String>() {{
            add(uri);
        }});

        Resource resource = mock(Resource.class);
        when(reader.getResource(uri))
                .thenReturn(resource);

        InputStream in = new ByteArrayInputStream("qwAESDRFTGYHUJIKOL;".getBytes());
        when(resource.getData())
                .thenReturn(in);

        GetContentHashEntity expected = new GetContentHashEntity(uri, transactionId, "abcdefg");

        when(publishingClient.getContentHash(host, transactionId, uri))
                .thenReturn(expected);

        try {
            hashVerifier.verifyTransactionContent(collection, reader);
        } catch (HashVerificationException ex) {
            assertTrue(ex.getMessage().contains("file content hash from remote server did not match the expected value "));
            assertThat(ex.getHost(), equalTo("localhost"));
            assertThat(ex.getTransactionId(), equalTo("666"));
            assertThat(ex.getUri(), equalTo("/a/b/c/data.json"));
            throw ex;
        }
    }

    @Test
    public void verifyTransactionContent_allTasksSuccessfulNoException() throws Exception {
        String uri = "/a/b/c/data.json";
        String host = "localhost";
        String transactionId = "666";
        String collectionId = "666";

        when(collection.getId())
                .thenReturn(collectionId);

        when(collection.getDescription())
                .thenReturn(description);

        when(description.getPublishTransactionIds())
                .thenReturn(new HashMap<String, String>() {{
                    put(host, transactionId);
                }});

        when(collection.getReviewed())
                .thenReturn(reviewed);

        when(reviewed.uris()).thenReturn(new ArrayList<String>() {{
            add(uri);
        }});

        Resource resource = mock(Resource.class);
        when(reader.getResource(uri))
                .thenReturn(resource);

        String value = "hello world";
        String expectedHash = null;
        try (InputStream in = new ByteArrayInputStream(value.getBytes())) {
            expectedHash = DigestUtils.sha1Hex(in);
        }

        when(resource.getData())
                .thenReturn(new ByteArrayInputStream(value.getBytes()));

        GetContentHashEntity expected = new GetContentHashEntity(uri, transactionId, expectedHash);

        when(publishingClient.getContentHash(host, transactionId, uri))
                .thenReturn(expected);

        hashVerifier.verifyTransactionContent(collection, reader);
    }
}
