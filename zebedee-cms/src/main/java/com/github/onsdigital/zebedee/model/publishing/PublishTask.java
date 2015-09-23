package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.PathUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
        System.out.println("Running scheduled job for collection id: " + collectionId);

        try {
            Collection collection = zebedee.collections.list().getCollection(this.collectionId);

            if (collection.description.approvedStatus == false) {
                System.out.println("Scheduled collection has not been approved - switching to manual");

                // Switch to manual
                collection.description.type = CollectionType.manual;

                // TODO Alarm message

                // and save
                String filename = PathUtils.toFilename(collection.description.name) + ".json";
                Path collectionPath = zebedee.collections.path.resolve(filename);
                System.out.println(collectionPath);
                try (OutputStream output = Files.newOutputStream(collectionPath)) {
                    Serialiser.serialise(output, collection.description);
                }

            } else {

                // Publish the s
                Publisher.Publish(zebedee, collection, "System");
            }
        } catch (IOException e) {
            System.out.println("Exception publishing collection for ID" + collectionId + " exception:" + e.getMessage());
        }
    }
}
