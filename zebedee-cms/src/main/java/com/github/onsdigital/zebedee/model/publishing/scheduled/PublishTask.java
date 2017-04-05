package com.github.onsdigital.zebedee.model.publishing.scheduled;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.PathUtils;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.publishing.PostPublisher;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.model.publishing.Publisher;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

public class PublishTask implements Runnable {

    private final String collectionId;
    private final Zebedee zebedee;

    public PublishTask(Zebedee zebedee, Collection collection) {
        this.collectionId = collection.description.id;
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

        try {
            Collection collection = zebedee.getCollections().getCollection(this.collectionId);

            if (collection.description.approvalStatus != ApprovalStatus.COMPLETE) {
                logInfo("Scheduled collection has not been approved - switching to manual")
                        .addParameter("collectionId", collectionId).log();

                // Switch to manual
                collection.description.type = CollectionType.manual;

                // TODO Alarm message

                // and save
                String filename = PathUtils.toFilename(collection.description.name) + ".json";
                Path collectionPath = zebedee.getCollections().path.resolve(filename);
                try (OutputStream output = Files.newOutputStream(collectionPath)) {
                    Serialiser.serialise(output, collection.description);
                }

            } else {

                // Publish the s
                boolean skipVerification = false;

                ZebedeeCollectionReader collectionReader = new ZebedeeCollectionReader(collection, zebedee.getKeyringCache().schedulerCache.get(collectionId));
                long publishStart = System.currentTimeMillis();
                boolean publishComplete = Publisher.Publish(collection, "System", collectionReader);

                if (publishComplete) {
                    long onPublishCompleteStart = System.currentTimeMillis();
                    new PublishNotification(collection).sendNotification(EventType.PUBLISHED);
                    PostPublisher.postPublish(zebedee, collection, skipVerification, collectionReader);

                    logInfo("Collection postPublish process finished").collectionName(collection)
                            .timeTaken((System.currentTimeMillis() - onPublishCompleteStart)).log();
                    logInfo("Collection publish complete").collectionName(collection)
                            .timeTaken((System.currentTimeMillis() - publishStart)).log();
                }
            }
        } catch (IOException | NotFoundException | BadRequestException | UnauthorizedException e) {
            logError(e, "Exception publishing collection").addParameter("collectionId", collectionId).log();
        }
    }
}
