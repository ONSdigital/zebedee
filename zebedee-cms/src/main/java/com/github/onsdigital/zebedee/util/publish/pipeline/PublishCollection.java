package com.github.onsdigital.zebedee.util.publish.pipeline;


import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.publishing.request.Manifest;
import com.github.onsdigital.zebedee.model.*;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.model.publishing.Publisher;
import com.github.onsdigital.zebedee.util.upstream.UpstreamContent;
import com.google.gson.Gson;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PublishCollection {

    private final Properties kafkaProducer = new Properties();
    private final Properties kafkaConsumer = new Properties();
    private final String producerTopic;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    PublishCollection() {
        final String kafkaAddress = findEnv("KAFKA_ADDR", "localhost:9092");
        producerTopic = findEnv("PRODUCER_TOPIC", "uk.gov.ons.dp.web.schedule");

        kafkaProducer.put("bootstrap.servers", kafkaAddress);
        kafkaProducer.put("acks", "all");
        kafkaProducer.put("retries", 0);
        kafkaProducer.put("batch.size", 16384);
        kafkaProducer.put("linger.ms", 1);
        kafkaProducer.put("buffer.memory", 33554432);
        kafkaProducer.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProducer.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        kafkaConsumer.put("bootstrap.servers", kafkaAddress);
        kafkaConsumer.put("group.id", "uk.gov.ons.dp.web.zebedee.cms");
        kafkaConsumer.put("enable.auto.commit", "true");
        kafkaConsumer.put("auto.commit.interval.ms", "1000");
        kafkaConsumer.put("session.timeout.ms", "30000");
        kafkaConsumer.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaConsumer.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        executor.submit(this::pollForCompleteMessages);
    }

    public void schedule(Collection collection, Zebedee zebedee)  {
        final String collectionId = collection.description.id;
        final String collectionPath = collection.path.getFileName().toString();
        final Manifest manifest;
        try {
            manifest = Manifest.get(collection);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String epoch = "0";
        if (collection.description.publishDate != null) {
            epoch = Long.toString(collection.description.publishDate.getTime());
        }

        final Set<String> filesToDelete = manifest.urisToDelete;
        final SecretKey key = zebedee.getKeyringCache().schedulerCache.get(collection.description.id);
        final String encrytionKey = Base64.getEncoder().encodeToString(key.getEncoded());
        final String kafkaMessage = SchedulerMessage.createSchedulerMessage(collectionId, collectionPath,
                epoch, encrytionKey, filesToDelete, findAllFiles(collection));
        System.out.println("Sending kafka message : " + kafkaMessage);
        collection.description.publishStartDate = new Date(Instant.now().getEpochSecond());
        try (Producer<String, String> producer = new KafkaProducer<>(kafkaProducer)) {
            producer.send(new ProducerRecord<>(producerTopic, kafkaMessage));
        }
    }

    private void pollForCompleteMessages() {
        final String consumeTopic = findEnv("CONSUME_TOPIC", "uk.gov.ons.dp.web.complete1");
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaConsumer)) {
            consumer.subscribe(Collections.singleton(consumeTopic));
            while (true) {
                final ConsumerRecords<String, String> records = consumer.poll(100);
                for (ConsumerRecord<String, String> record : records) {
                    final Gson gson = new Gson();
                    final CompleteMessage message = gson.fromJson(record.value(), CompleteMessage.class);
                    onCompleteCollection(message);
                }
            }
        }
    }

    private void onCompleteCollection(final CompleteMessage completeMessage) {
        Zebedee zebedee = Root.zebedee;
        try {
            Collection collection = Root.zebedee.getCollections().getCollection(completeMessage.getCollectionId());
            new PublishNotification(collection).sendNotification(EventType.PUBLISHED);
            collection.description.publishEndDate = new Date(Instant.now().getEpochSecond());
            ZebedeeCollectionReader collectionReader = new ZebedeeCollectionReader(collection, zebedee.getKeyringCache().schedulerCache.get(completeMessage.getCollectionId()));
            Publisher.postPublish(zebedee, collection, true, collectionReader);
            System.out.println("Complete publishing collectionId : " + completeMessage.getCollectionId() + ", jobId : " + completeMessage.getScheduleID());

        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private String findEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null? defaultValue : value;
    }

    private Set<PublishedFile> findAllFiles(Collection collection) {
        final Path dir = collection.reviewed.getPath();
        final Set<PublishedFile> files = new HashSet<>();
        try {
            Files.walk(dir).forEach(file -> {
                if (!Files.isDirectory(file)) {
                    final String uri = file.toString().split("/reviewed")[1];
                    files.add(new PublishedFile(uri, UpstreamContent.buildS3Address(collection, uri)));
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return files;
    }

}
