package com.github.onsdigital.zebedee.kafka;

import com.github.onsdigital.zebedee.avro.ContentPublished;
import com.github.onsdigital.zebedee.kafka.avro.AvroSerializer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.Future;

import static com.amazonaws.util.Base64.decode;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaSecClientKeyP12;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaSecClientKey;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaSecProtocol;
/**
 * This class represents a client that actually interfaces with Kafka and creates a producer that allows sending of
 * kafka messages. The class initiates the producer on initialisation.
  */
public class KafkaClientImpl implements KafkaClient {

    private final Producer<String, ContentPublished> producer;
    private String topic;
    private static final String KEY_FILE_PREFIX = "client-key";

    public KafkaClientImpl(String kafkaAddr, String topic) {
        this.topic = topic;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,kafkaAddr);

        if (getKafkaSecProtocol().equals("TLS")) {
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
            if (!getKafkaSecClientKeyP12().isEmpty()) {
                info().log("key info not used if KAFKA_SEC_CLIENT_KEY_P12 set)");
                byte[] kafkaSecClientKeyBytes  = decode(getKafkaSecClientKeyP12());
                props.put(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, kafkaSecClientKeyBytes);
            } else {
                // key already in file
                props.put(SslConfigs.SSL_KEYSTORE_LOCATION_DOC, getKafkaSecClientKey());
            }
            props.put("SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG", "");
            props.put("SslConfigs.SSL_KEYSTORE_TYPE_CONFIG", "PKCS12");
        }

        AvroSerializer<ContentPublished> avroSerializer = new AvroSerializer<>(ContentPublished.class);
        this.producer = new KafkaProducer<>(props, new StringSerializer(), avroSerializer);

        Runtime.getRuntime().addShutdownHook(new Thread(producer::close));
    }


    /**
     * Produce a content-published kafka message and return a future for that message result
     */
    @Override
    public Future<RecordMetadata> produceContentPublished(String uri, String dataType, String collectionID) {
        ContentPublished value = new ContentPublished(uri,dataType,collectionID);
        return producer.send(new ProducerRecord<>(topic,value));
    }

}
