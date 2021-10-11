package com.github.onsdigital.zebedee.kafka;

import com.github.onsdigital.zebedee.avro.ContentPublished;
import com.github.onsdigital.zebedee.kafka.avro.AvroSerializer;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaSecProtocol;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaSecClientKeyP12;
import static com.github.onsdigital.zebedee.configuration.Configuration.getKafkaSecClientKey;
import static com.amazonaws.util.Base64.decode;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;
import java.util.concurrent.Future;

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
        props.put("bootstrap.servers",kafkaAddr);

        if (getKafkaSecProtocol().equals("TLS")) {
            props.put("security.protocol", "SSL");
            if (!getKafkaSecClientKeyP12().isEmpty()) {
                byte[] kafkaSecClientKeyBytes  = decode(getKafkaSecClientKeyP12());
                // Kafka versions before 2.7 needs the above to be in files
                File keyFile;
                try {
                    keyFile  = new File(Files.createTempFile(KEY_FILE_PREFIX, ".p12").toString());
                    FileUtils.writeByteArrayToFile(keyFile, kafkaSecClientKeyBytes);
                } catch (IOException e) {
                    error().logException(e, "failed to create file using AWS-MSK kafka protocol");
                    throw new RuntimeException(e);
                }
                props.put("ssl.keystore.location",   keyFile.toString());

            } else {
                // key already in file
                props.put("ssl.keystore.location", getKafkaSecClientKey());
            }
            props.put("ssl.keystore.password", "");
            props.put("ssl.keystore.type", "PKCS12");
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
