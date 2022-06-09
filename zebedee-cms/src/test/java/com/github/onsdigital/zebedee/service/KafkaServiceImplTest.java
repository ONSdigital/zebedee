package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.kafka.KafkaClient;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class KafkaServiceImplTest {

    private static final String COLLECTION_ID = "col123";
    private static final String URI1 = "/moo";
    private static final String URI2 = "/quack";
    private static final String URI3 = "/oink";
    private static final String TEST_DATATYPE = "testDataType";
    private static final String TEST_TRACE_ID = "10110011100010000";
    private static final String TEST_JOBID = "";
    private static final String TEST_SEARCHINDEX = "testONS";

    KafkaClient mockKafkaClient = mock(KafkaClient.class);

    @Before
    public void setup() throws Exception {
        Future<RecordMetadata> mockFuture = mock(Future.class);
        when(mockFuture.get()).thenReturn(new RecordMetadata(null, 0, 0, 0, null, 0, 0));
        when(mockKafkaClient.produceContentUpdated(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString()))
                        .thenReturn(mockFuture);
    }

    @Test
    public void testProduceContentUpdated_threeURIs() throws Exception {
        // Given a kafka service with a mocked kafka client
        KafkaService kafkaService = new KafkaServiceImpl(mockKafkaClient);

        List<String> uris = Arrays.asList(URI1, URI2, URI3);

        // When publish is called on the collection
        kafkaService.produceContentUpdated(COLLECTION_ID, uris, TEST_DATATYPE, TEST_JOBID, TEST_SEARCHINDEX,
                TEST_TRACE_ID);

        // Then publishImage should be called on the API for each image.
        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dataTypeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> colIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> jobIdCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> searchIndexCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> traceIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockKafkaClient, times(3)).produceContentUpdated(uriCaptor.capture(), dataTypeCaptor.capture(),
                colIdCaptor.capture(), jobIdCapture.capture(), searchIndexCapture.capture(), traceIdCaptor.capture());

        Set<String> urisCalled = new HashSet<>(uriCaptor.getAllValues());

        assertEquals(3, uriCaptor.getAllValues().size());
        assertTrue(urisCalled.contains(URI1));
        assertTrue(urisCalled.contains(URI2));
        assertTrue(urisCalled.contains(URI3));

        assertEquals(TEST_DATATYPE, dataTypeCaptor.getAllValues().get(0));
        assertEquals(COLLECTION_ID, colIdCaptor.getAllValues().get(0));
        assertEquals(TEST_DATATYPE, dataTypeCaptor.getAllValues().get(1));
        assertEquals(COLLECTION_ID, colIdCaptor.getAllValues().get(1));
        assertEquals(TEST_DATATYPE, dataTypeCaptor.getAllValues().get(2));
        assertEquals(COLLECTION_ID, colIdCaptor.getAllValues().get(2));
    }

    @Test
    public void testProduceContentUpdated_zeroURIs() throws Exception {
        // Given a kafka service with a mocked kafka client
        KafkaService kafkaService = new KafkaServiceImpl(mockKafkaClient);

        List<String> uris = new ArrayList<>();

        // When publish is called on the collection
        kafkaService.produceContentUpdated(COLLECTION_ID, uris, TEST_DATATYPE, TEST_JOBID, TEST_SEARCHINDEX,
                TEST_TRACE_ID);

        // Then publishImage should be called on the API for each image.
        verify(mockKafkaClient, never()).produceContentUpdated(anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString());
    }

    @Test
    public void testProduceContentUpdated_clientException() throws Exception {
        final Future future = mock(Future.class);
        when(future.get()).thenThrow(new ExecutionException(new IOException()));
        when(mockKafkaClient.produceContentUpdated(anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString()))
                        .thenReturn(future);
        KafkaService kafkaService = new KafkaServiceImpl(mockKafkaClient);
        List<String> uris = Arrays.asList(URI1, URI2, URI3);
        // When produceContentUpdated is called on the collection
        Throwable thrown = assertThrows(IOException.class, () -> kafkaService.produceContentUpdated(COLLECTION_ID,
                uris, TEST_DATATYPE, TEST_JOBID, TEST_SEARCHINDEX, TEST_TRACE_ID));
        // Then a timeout exception is thrown.
        assertEquals(thrown.getMessage(), "unable to process kafka message");
    }
}