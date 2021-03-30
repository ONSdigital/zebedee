package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.publishing.PostPublisher;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.model.publishing.Publisher;
import com.github.onsdigital.zebedee.util.SlackNotification;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;

public class PublishTask implements Runnable {

    private final String collectionId;
    private final Zebedee zebedee;

    public PublishTask(Zebedee zebedee, Collection collection) {
        this.collectionId = collection.getDescription().getId();
        this.zebedee = zebedee;
    }

    /**
     * Run task
     * Checks if the collection has been approved. If so publishes. If not creates a warning and switches the type to manual
     */
    @Override
    public void run() {
        info().data("collectionId", collectionId).log("Running scheduled job for collection");
        Collection collection = null;
        boolean publishComplete = false;

        try {
            collection = zebedee.getCollections().getCollection(this.collectionId);

            if (collection.getDescription().getApprovalStatus() != ApprovalStatus.COMPLETE) {
                info().data("collectionId", collectionId).log("Scheduled collection has not been approved - switching to manual");

                // Switch to manual
                collection.getDescription().setType(CollectionType.manual);

                // and save
                String filename = PathUtils.toFilename(collection.getDescription().getName()) + ".json";
                Path collectionPath = zebedee.getCollections().path.resolve(filename);
                try (OutputStream output = Files.newOutputStream(collectionPath)) {
                    Serialiser.serialise(output, collection.description);
                }

            } else {

                // Publish the s
                boolean skipVerification = false;

                ZebedeeCollectionReader collectionReader = new ZebedeeCollectionReader(collection,
                        zebedee.getLegacyKeyringCache().getSchedulerCache().get(collectionId));
                long publishStart = System.currentTimeMillis();
                publishComplete = Publisher.publish(collection, "System", collectionReader);

                if (publishComplete) {
                    long onPublishCompleteStart = System.currentTimeMillis();
                    new PublishNotification(collection).sendNotification(EventType.PUBLISHED);
                    PostPublisher.postPublish(zebedee, collection, skipVerification, collectionReader);

                    info().data("collectionId", collectionId).data("timeTaken", (System.currentTimeMillis() - onPublishCompleteStart))
                            .log("Collection postPublish process finished");

                    info().data("collectionId", collectionId).data("timeTaken", (System.currentTimeMillis() - onPublishCompleteStart))
                            .log("Collection publish complete");

                } else {
                    warn().data("collectionId", collectionId).log("scheduled collection publish did not complete successfully");
                    SlackNotification.scheduledPublishFailure(collection);
                }
            }
        } catch (Exception e) {
            error().data("collectionId", collectionId).logException(e, "Exception publishing scheduled collection");

            SlackNotification.scheduledPublishFailure(collection);
        }
    }
}
