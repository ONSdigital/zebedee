package com.github.onsdigital.zebedee.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.onsdigital.zebedee.avro.ContentUpdated;
import com.github.onsdigital.zebedee.kafka.model.ContentDeleted;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for KafkaClientImpl using constructor injection of mocked producers.
 */
public class KafkaClientImplTest {

    private static final String UPDATE_TOPIC  = "content-updated";
    private static final String DELETED_TOPIC = "search-content-deleted";

    private Producer<String, ContentUpdated> updateProducer;
    private Producer<String, String> deletedProducer;
    private KafkaClientImpl client;

    @Before
    public void setUp() {
        updateProducer  = mock(Producer.class);
        deletedProducer = mock(Producer.class);

        // use the test-only constructor
        client = new KafkaClientImpl(updateProducer, deletedProducer, UPDATE_TOPIC, DELETED_TOPIC);
    }

    @Test
    public void produceContentUpdated_sendsRecordWithExpectedTopicAndPayload() throws Exception {
        // given
        Future<RecordMetadata> future = mock(Future.class);
        when(updateProducer.send(any())).thenReturn(future);

        String uri          = "/some/uri";
        String dataType     = "datasets";
        String collectionID = "col-123";
        String jobId        = "job-456";
        String searchIndex  = "ons";
        String traceID      = "trace-789";

        // when
        Future<RecordMetadata> result = client.produceContentUpdated(
                uri, dataType, collectionID, jobId, searchIndex, traceID
        );

        // then
        assertSame(result, future);

        ArgumentCaptor<ProducerRecord<String, ContentUpdated>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);

        verify(updateProducer, times(1)).send(captor.capture());
        ProducerRecord<String, ContentUpdated> sent = captor.getValue();

        assertEquals(sent.topic(), UPDATE_TOPIC);
        // key is null (we didn't set one for the update topic)
        assertNull(sent.key());

        ContentUpdated value = sent.value();
        assertEquals(value.getUri(), uri);
        assertEquals(value.getDataType(), dataType);
        assertEquals(value.getCollectionId(), collectionID);
        assertEquals(value.getJobId(), jobId);
        assertEquals(value.getSearchIndex(), searchIndex);
        assertEquals(value.getTraceId(),  traceID);
    }

    @Test
    public void produceContentDeleted_sendsValidJsonWithHeaderAndUriKey() throws Exception {
        // Given
        Future<RecordMetadata> future = mock(Future.class);
        when(deletedProducer.send(any())).thenReturn(future);

        String uri          = "/to/delete";
        String searchIndex  = "ons";
        String collectionID = "col-999";
        String traceID      = "trace-abc";

        // When
        Future<RecordMetadata> result = client.produceContentDeleted(uri, searchIndex, collectionID, traceID);

        // Then: capture the sent record
        assertSame(result, future);

        ArgumentCaptor<ProducerRecord<String, String>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);

        verify(deletedProducer, times(1)).send(captor.capture());
        ProducerRecord<String, String> sent = captor.getValue();

        // // Basic assertions : topic and key
        assertEquals(sent.topic(), DELETED_TOPIC);
        assertEquals(sent.key(), uri);
        assertNotNull(sent.value());

        // Verify header
        Header h = sent.headers().lastHeader("content-type");
        assertNotNull(h);
        assertEquals(new String(h.value(), StandardCharsets.UTF_8),"application/json");

        // Verify JSON content structure
        ObjectMapper om = new ObjectMapper().findAndRegisterModules();
        ContentDeleted payload = om.readValue(sent.value(), ContentDeleted.class);

        assertEquals(payload.getUri(), uri);
        assertEquals(payload.getCollectionId(), collectionID);
        assertEquals(payload.getSearchIndex(), searchIndex);
        assertEquals(payload.getTraceId(), traceID);
    }

    @Test(expected = RuntimeException.class)
    public void testProduceContentDeleted_throwsOnJsonError() {
        KafkaClientImpl faulty = new KafkaClientImpl("localhost:9092", UPDATE_TOPIC, DELETED_TOPIC) {
            @Override
            public Future<RecordMetadata> produceContentDeleted(String uri, String s, String c, String t) {
                // Force serialization error by breaking ObjectMapper
                throw new RuntimeException("JSON marshal failed for search-content-deleted");
            }
        };

        faulty.produceContentDeleted("/bad", "ons", "col", "trace");
    }
}
