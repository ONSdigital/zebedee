package com.github.onsdigital.zebedee.util.publish.pipeline;


import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.model.publishing.Publisher;
import com.google.gson.Gson;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PublishCollection {

    private final Properties kafkaProducer = new Properties();
    private final Properties kafkaConsumer = new Properties();

    // CollectionId is different between the pipeline and zebedee.
    private static final ConcurrentHashMap<String, String> workaround = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PublishCollection() {
        kafkaProducer.put("bootstrap.servers", "localhost:9092");
        kafkaProducer.put("acks", "all");
        kafkaProducer.put("retries", 0);
        kafkaProducer.put("batch.size", 16384);
        kafkaProducer.put("linger.ms", 1);
        kafkaProducer.put("buffer.memory", 33554432);
        kafkaProducer.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducer.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaConsumer.put("bootstrap.servers", "localhost:9092");
        kafkaConsumer.put("group.id", "uk.gov.ons.dp.web.zebedee.cms");
        kafkaConsumer.put("enable.auto.commit", "true");
        kafkaConsumer.put("auto.commit.interval.ms", "1000");
        kafkaConsumer.put("session.timeout.ms", "30000");
        kafkaConsumer.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaConsumer.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        executor.submit(this::pollForCompleteMessages);
    }

    public void schedule(Collection collection, Zebedee zebedee) {
        final String collectionId = collection.path.getFileName().toString();
        String epoch = "0";
        if (collection.description.publishDate != null) {
            epoch = Long.toString(collection.description.publishDate.getTime());
        }

        final SecretKey key = zebedee.getKeyringCache().schedulerCache.get(collection.description.id);
        final String encrytionKey = Base64.getEncoder().encodeToString(key.getEncoded());

        final String kafkaMessage = SchedulerMessage.createSchedulerMessage(collectionId, epoch, encrytionKey);
        workaround.putIfAbsent(collectionId, collection.description.id);
        System.out.println("Sending kafka message : " + kafkaMessage);
        collection.description.publishStartDate = new Date(Instant.now().getEpochSecond());
        try (Producer<String, String> producer = new KafkaProducer<>(kafkaProducer)) {
            producer.send(new ProducerRecord<>("uk.gov.ons.dp.web.schedule", kafkaMessage));
        }
    }

    private void pollForCompleteMessages() {
        final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaConsumer);
        consumer.subscribe(Arrays.asList("uk.gov.ons.dp.web.complete"));
        while (true) {
            final ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("offset = %d, key = %s, value = %s", record.offset(), record.key(), record.value());
                final Gson gson = new Gson();
                final CompleteMessage message = gson.fromJson(record.value(), CompleteMessage.class);
                System.out.println("message.getCollectionId()");
                onCompleteCollection(message.getCollectionId());
            }
        }
    }

    private void onCompleteCollection(String collectionId) {
        System.out.println("onCompleteCollection");
        Zebedee zebedee = Root.zebedee;
        System.out.println("String realCollectionId = workaround.get(collectionId);");
        String realCollectionId = workaround.get(collectionId);
        if (realCollectionId == null) return;
        workaround.remove(collectionId);
        try {
            System.out.println("Collection collection = Root.zebedee.getCollections()");
            Collection collection = Root.zebedee.getCollections().getCollection(realCollectionId);
            new PublishNotification(collection).sendNotification(EventType.PUBLISHED);
            collection.description.publishEndDate = new Date(Instant.now().getEpochSecond());
            ZebedeeCollectionReader collectionReader = new ZebedeeCollectionReader(collection, zebedee.getKeyringCache().schedulerCache.get(realCollectionId));
            Publisher.postPublish(zebedee, collection, true, collectionReader);
            System.out.println("Complete publishing for : " + collectionId);

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

}
