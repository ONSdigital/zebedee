package com.github.onsdigital.zebedee.kafka;

import com.github.onsdigital.zebedee.avro.ContentPublished;
import com.github.onsdigital.zebedee.kafka.avro.AvroSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.Future;

/**
 * This class represents a client that actually interfaces with Kafka and creates a producer that allows sending of
 * kafka messages. The class initiates the producer on initialisation.
  */
public class KafkaClientImpl implements KafkaClient {
    private final Producer<String, ContentPublished> producer;
    private String topic;

    public KafkaClientImpl(String kafkaAddr, String topic) {
        this.topic = topic;

        Properties props = new Properties();
        props.put("bootstrap.servers",kafkaAddr);
        AvroSerializer<ContentPublished> avroSerializer = new AvroSerializer<>(ContentPublished.class);
        this.producer = new KafkaProducer<>(props, new StringSerializer(), avroSerializer);

        Runtime.getRuntime().addShutdownHook(new Thread(producer::close));
    }


    /**
     * Produce a content-published kafka message and return a future for that message result
     */
    @Override
    public Future<RecordMetadata> produceContentPublished(String url, String dataType, String collectionID) {

        String uri = url;
        if (url.endsWith("/data.json")) {
            uri = url.substring(0, url.length() - "/data.json".length());
        }
        ContentPublished value = new ContentPublished(uri,dataType,collectionID);
        return producer.send(new ProducerRecord<>(topic,value));
    }

}
