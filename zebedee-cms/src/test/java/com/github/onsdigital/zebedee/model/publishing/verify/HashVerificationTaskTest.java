package com.github.onsdigital.zebedee.model.publishing.verify;

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
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.concurrent.Callable;

import static com.github.onsdigital.zebedee.model.publishing.verify.HashVerificationTask.GENERATE_HASH_ERR;
import static com.github.onsdigital.zebedee.model.publishing.verify.HashVerificationTask.HASH_INCORRECT_ERR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class HashVerificationTaskTest {

    private static final String COLLECTION_ID = "666";
    private static final String HOST = "localhost";
    private static final String TRANSACTION_ID = "1";
    private static final String URI = "/helloworld";

    @Mock
    private CollectionReader collectionReader;

    @Mock
    private Resource resource;

    @Mock
    private PublishingClient publishingClient;

    private Callable<Boolean> task;

    private GetContentHashEntity entity;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        task = new HashVerificationTask.Builder()
                .collectionID(COLLECTION_ID)
                .collectionReader(collectionReader)
                .publishingAPIHost(HOST)
                .transactionId(TRANSACTION_ID)
                .contentURI(URI)
                .publishingClient(publishingClient)
                .build();

        entity = new GetContentHashEntity(URI, TRANSACTION_ID, "1234567890");
    }

    @Test(expected = NullPointerException.class)
    public void testBuilder_collectionIdNull() {
        new HashVerificationTask.Builder().build();
    }

    @Test(expected = NullPointerException.class)
    public void testBuilder_collectionReaderNull() {
        new HashVerificationTask.Builder()
                .collectionID(COLLECTION_ID)
                .collectionReader(null)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testBuilder_hostNull() {
        new HashVerificationTask.Builder()
                .collectionID(COLLECTION_ID)
                .collectionReader(collectionReader)
                .publishingAPIHost(null)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testBuilder_transactionIdNull() {
        new HashVerificationTask.Builder()
                .collectionID(COLLECTION_ID)
                .collectionReader(collectionReader)
                .publishingAPIHost(HOST)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testBuilder_uriNull() {
        new HashVerificationTask.Builder()
                .collectionID(COLLECTION_ID)
                .collectionReader(collectionReader)
                .publishingAPIHost(HOST)
                .transactionId(TRANSACTION_ID)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testBuilder_publishingClientNull() {
        new HashVerificationTask.Builder()
                .collectionID(COLLECTION_ID)
                .collectionReader(collectionReader)
                .publishingAPIHost(HOST)
                .transactionId(TRANSACTION_ID)
                .contentURI(URI)
                .build();
    }

    @Test
    public void testBuilder_success() {
        HashVerificationTask actual = new HashVerificationTask.Builder()
                .collectionID(COLLECTION_ID)
                .collectionReader(collectionReader)
                .publishingAPIHost(HOST)
                .transactionId(TRANSACTION_ID)
                .contentURI(URI)
                .publishingClient(publishingClient)
                .build();

        assertThat(actual.getCollectionID(), equalTo(COLLECTION_ID));
        assertThat(actual.getCollectionReader(), equalTo(collectionReader));
        assertThat(actual.getHost(), equalTo(HOST));
        assertThat(actual.getTransactionId(), equalTo(TRANSACTION_ID));
        assertThat(actual.getUri(), equalTo(URI));
        assertThat(actual.getPublishingClient(), equalTo(publishingClient));
    }

    @Test(expected = HashVerificationException.class)
    public void testCall_publishingClientIOException() throws Exception {
        when(publishingClient.getContentHash(HOST, TRANSACTION_ID, URI))
                .thenThrow(new IOException());

        try {
            task.call();
        } catch (HashVerificationException ex) {
            assertThat(ex.getTransactionId(), equalTo(TRANSACTION_ID));
            assertThat(ex.getCollectionId(), equalTo(COLLECTION_ID));
            assertThat(ex.getHost(), equalTo(HOST));
            assertThat(ex.getUri(), equalTo(URI));
            assertThat(ex.getMessage(), equalTo("http request to publishing API /getContentHash returned an error"));
            throw ex;
        }
    }

    @Test(expected = HashVerificationException.class)
    public void testCall_publishingClientURISyntaxException() throws Exception {
        when(publishingClient.getContentHash(HOST, TRANSACTION_ID, URI))
                .thenThrow(new URISyntaxException("", ""));

        try {
            task.call();
        } catch (HashVerificationException ex) {
            assertThat(ex.getTransactionId(), equalTo(TRANSACTION_ID));
            assertThat(ex.getCollectionId(), equalTo(COLLECTION_ID));
            assertThat(ex.getHost(), equalTo(HOST));
            assertThat(ex.getUri(), equalTo(URI));
            assertThat(ex.getMessage(), equalTo("http request to publishing API /getContentHash returned an error"));
            throw ex;
        }
    }

    @Test(expected = HashVerificationException.class)
    public void testCall_collectionReaderIOException() throws Exception {
        when(publishingClient.getContentHash(HOST, TRANSACTION_ID, URI))
                .thenReturn(entity);

        IOException cause = new IOException("collectioncReader get resource exeption");
        when(collectionReader.getResource(URI))
                .thenThrow(cause);

        try {
            task.call();
        } catch (HashVerificationException ex) {
            assertThat(ex.getMessage(), equalTo(GENERATE_HASH_ERR));
            assertThat(ex.getCause(), equalTo(cause));
            assertThat(ex.getHost(), equalTo(HOST));
            assertThat(ex.getUri(), equalTo(URI));
            assertThat(ex.getTransactionId(), equalTo(TRANSACTION_ID));
            throw ex;
        }
    }

    @Test(expected = HashVerificationException.class)
    public void testCall_hashValuesDoNotMatch() throws Exception {
        byte[] testData = "Hello world".getBytes();

        String expected;
        try (InputStream inputStream = new ByteArrayInputStream(testData)) {
            expected = DigestUtils.sha1Hex(inputStream);
        }

        when(publishingClient.getContentHash(HOST, TRANSACTION_ID, URI))
                .thenReturn(entity);

        when(collectionReader.getResource(URI))
                .thenReturn(resource);

        try (InputStream inputStream = new ByteArrayInputStream(testData)) {
            when(resource.getData()).thenReturn(inputStream);
        }

        try {
            task.call();
        } catch (HashVerificationException ex) {
            assertThat(ex.getMessage(), equalTo(MessageFormat.format(HASH_INCORRECT_ERR, expected, "1234567890")));
            assertThat(ex.getHost(), equalTo(HOST));
            assertThat(ex.getUri(), equalTo(URI));
            assertThat(ex.getTransactionId(), equalTo(TRANSACTION_ID));
            throw ex;
        }
    }

    @Test
    public void testCall_matchingHashValues() throws Exception {
        byte[] testData = "Hello world".getBytes();

        GetContentHashEntity entity;
        try (InputStream inputStream = new ByteArrayInputStream(testData)) {
            entity = new GetContentHashEntity(URI, TRANSACTION_ID, DigestUtils.sha1Hex(inputStream));
        }

        when(publishingClient.getContentHash(HOST, TRANSACTION_ID, URI))
                .thenReturn(entity);

        when(collectionReader.getResource(URI))
                .thenReturn(resource);

        try (InputStream inputStream = new ByteArrayInputStream(testData)) {
            when(resource.getData()).thenReturn(inputStream);
        }

        assertThat(task.call(), is(true));
    }
}
