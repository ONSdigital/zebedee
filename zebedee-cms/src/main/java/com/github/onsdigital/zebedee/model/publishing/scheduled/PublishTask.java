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

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;

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
        logInfo("Running scheduled job for collection")
                .addParameter("collectionId", collectionId).log();
        Collection collection = null;
        boolean publishComplete = false;

        try {
            collection = zebedee.getCollections().getCollection(this.collectionId);

            if (collection.description.approvalStatus != ApprovalStatus.COMPLETE) {
                logInfo("Scheduled collection has not been approved - switching to manual")
                        .addParameter("collectionId", collectionId).log();

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

                ZebedeeCollectionReader collectionReader = new ZebedeeCollectionReader(collection, zebedee.getKeyringCache().schedulerCache.get(collectionId));
                long publishStart = System.currentTimeMillis();
                publishComplete = Publisher.Publish(collection, "System", collectionReader);

                if (publishComplete) {
                    long onPublishCompleteStart = System.currentTimeMillis();
                    new PublishNotification(collection).sendNotification(EventType.PUBLISHED);
                    PostPublisher.postPublish(zebedee, collection, skipVerification, collectionReader);

                    logInfo("Collection postPublish process finished")
                            .collectionName(collection)
                            .collectionId(collectionId)
                            .timeTaken((System.currentTimeMillis() - onPublishCompleteStart))
                            .log();
                    logInfo("Collection publish complete")
                            .collectionName(collection)
                            .collectionId(collectionId)
                            .timeTaken((System.currentTimeMillis() - publishStart))
                            .log();
                } else {
                    logWarn("scheduled collection publish did not complete successfully")
                            .collectionName(collection)
                            .collectionId(collection)
                            .log();

                    SlackNotification.scheduledPublishFailire(collection);
                }
            }
        } catch (Exception e) {
            logError(e, "Exception publishing scheduled collection")
                    .collectionId(collection)
                    .collectionName(collection)
                    .log();

            SlackNotification.scheduledPublishFailire(collection);
        }
    }
}
