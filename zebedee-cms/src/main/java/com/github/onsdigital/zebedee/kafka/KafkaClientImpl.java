package com.github.onsdigital.zebedee.kafka;

import com.github.onsdigital.zebedee.avro.ContentDeleted;
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

import java.util.Properties;
import java.util.concurrent.Future;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaSecClientKey;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaSecClientCert;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaSecProtocol;

/**
 * This class represents a client that actually interfaces with Kafka and
 * creates a producer that allows sending of
 * kafka messages. The class initiates the producer on initialisation.
 */
public class KafkaClientImpl implements KafkaClient {

    private final Producer<String, ContentUpdated> updateProducer;
    private final Producer<String, ContentDeleted> deletedProducer;
    private String updateTopic;
    private String deletedTopic;

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

        // Producer for ContentUpdated
        AvroSerializer<ContentUpdated> updatedSerializer = new AvroSerializer<>(ContentUpdated.class);
        this.updateProducer = new KafkaProducer<>(props, new StringSerializer(), updatedSerializer);

        // Producer for ContentDeleted
        AvroSerializer<ContentDeleted> deletedSerializer = new AvroSerializer<>(ContentDeleted.class);
        this.deletedProducer = new KafkaProducer<>(props, new StringSerializer(), deletedSerializer);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            updateProducer.close();
            deletedProducer.close();
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
        ContentDeleted value = new ContentDeleted(uri, collectionID, searchIndex, traceID);
        return deletedProducer.send(new ProducerRecord<>(deletedTopic, value));
    }
}
