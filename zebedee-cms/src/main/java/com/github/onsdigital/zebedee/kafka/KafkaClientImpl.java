package com.github.onsdigital.zebedee.kafka;

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

    private final Producer<String, ContentUpdated> producer;
    private String topic;

    public KafkaClientImpl(String kafkaAddr, String topic) {
        this.topic = topic;

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

        AvroSerializer<ContentUpdated> avroSerializer = new AvroSerializer<>(ContentUpdated.class);
        this.producer = new KafkaProducer<>(props, new StringSerializer(), avroSerializer);

        Runtime.getRuntime().addShutdownHook(new Thread(producer::close));
    }

    /**
     * Produce a content-updated kafka message and return a future for that message
     * result
     */
    @Override
    public Future<RecordMetadata> produceContentUpdated(String uri, String dataType, String collectionID,
            String jobId, String searchIndex, String traceID) {
        ContentUpdated value = new ContentUpdated(uri, dataType, collectionID, jobId, searchIndex, traceID);
        return producer.send(new ProducerRecord<>(topic, value));
    }

}
