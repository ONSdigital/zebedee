package com.github.onsdigital.zebedee.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.onsdigital.zebedee.kafka.model.ContentDeleted;
import com.github.onsdigital.zebedee.avro.ContentUpdated;
import com.github.onsdigital.zebedee.kafka.avro.AvroSerializer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.Future;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaSecClientKey;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaSecClientCert;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaSecProtocol;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;

/**
 * This class represents a client that actually interfaces with Kafka and
 * creates a producer that allows sending of
 * kafka messages. The class initiates the producer on initialisation.
 */
public class KafkaClientImpl implements KafkaClient {

    private final Producer<String, ContentUpdated> updateProducer;
    private final Producer<String, String> deletedProducer;
    private String updateTopic;
    private String deletedTopic;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);


    KafkaClientImpl(Producer<String, ContentUpdated> updateProducer,
                    Producer<String, String> deletedProducer,
                    String updateTopic,
                    String deletedTopic) {
        this.updateProducer = updateProducer;
        this.deletedProducer = deletedProducer;
        this.updateTopic = updateTopic;
        this.deletedTopic = deletedTopic;
    }

    public KafkaClientImpl(String kafkaAddr, String updateTopic, String deletedTopic) {
        this.updateTopic = updateTopic;
        this.deletedTopic = deletedTopic;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaAddr);

        if (getKafkaSecProtocol().equals("TLS")) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
            if (!getKafkaSecClientKey().isEmpty()) {
                info().log("key info KAFKA_SEC_CLIENT_KEY used");
                props.put(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, getKafkaSecClientKey());
                props.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PEM");
                props.put(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, getKafkaSecClientCert());
            }
        }

        this.updateProducer  = new KafkaProducer<>(props, new StringSerializer(), new AvroSerializer<>(ContentUpdated.class));
        this.deletedProducer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { updateProducer.close(); }  catch (Exception ignored) {}
            try { deletedProducer.close(); } catch (Exception ignored) {}
        }));
    }

    /**
     * Produce a content-updated kafka message and return a future for that message
     * result
     */
    @Override
    public Future<RecordMetadata> produceContentUpdated(String uri, String dataType, String collectionID,
            String jobId, String searchIndex, String traceID) {
        ContentUpdated value = new ContentUpdated(uri, dataType, collectionID, jobId, searchIndex, traceID);
        return updateProducer.send(new ProducerRecord<>(updateTopic, value));
    }

    @Override
    public Future<RecordMetadata> produceContentDeleted(String uri, String searchIndex, String collectionID, String traceID) {
        ContentDeleted payload = new ContentDeleted(uri, collectionID, searchIndex, traceID);

        try {
            String json = MAPPER.writeValueAsString(payload);

            ProducerRecord<String, String> record =
                    new ProducerRecord<>(deletedTopic, uri, json);

            record.headers().add("content-type", "application/json".getBytes(StandardCharsets.UTF_8));

            return deletedProducer.send(record);
        } catch (JsonProcessingException e) {
            error().data("uri", uri)
                    .data("index", searchIndex)
                    .data("collectionId", collectionID)
                    .data("traceId", traceID)
                    .exception(e)
                    .log("failed to serialize payload to JSON");
            throw new RuntimeException("JSON marshal failed for topic " + deletedTopic, e);
        }
    }
}
