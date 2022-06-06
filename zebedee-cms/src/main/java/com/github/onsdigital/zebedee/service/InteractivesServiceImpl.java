package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dp.interactives.api.InteractivesAPIClient;
import com.github.onsdigital.dp.interactives.api.models.Interactive;
import com.github.onsdigital.dp.interactives.api.models.InteractiveHTMLFile;
import com.github.onsdigital.dp.interactives.api.models.InteractiveMetadata;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionInteractive;
import com.github.onsdigital.zebedee.json.CollectionInteractiveFile;
import com.github.onsdigital.zebedee.json.ContentStatus;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.Arrays;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

/**
 * Dataset related services
 */
public class InteractivesServiceImpl implements InteractivesService {

    private InteractivesAPIClient interactivesClient;

    public InteractivesServiceImpl(InteractivesAPIClient interactivesClient) {
        this.interactivesClient = interactivesClient;
    }

    /**
     * Add the dataset for the given datasetID to the collection for the collectionID.
     */
    @Override
    public CollectionInteractive updateInteractiveInCollection(Collection collection, String id, CollectionInteractive updatedInteractive, String user) throws ZebedeeException, IOException, RuntimeException {

        CollectionInteractive collectionInteractive;

        Optional<CollectionInteractive> existingInteractive = collection.getDescription().getInteractive(id);
        if (existingInteractive.isPresent()) {
            collectionInteractive = existingInteractive.get();
        } else {
            collectionInteractive = new CollectionInteractive();
            collectionInteractive.setId(id);
        }

        if (updatedInteractive != null && updatedInteractive.getState() != null) {
            if (updatedInteractive.getState().equals(ContentStatus.InProgress)) {
                //initial state is always Complete
                updatedInteractive.setState(ContentStatus.Complete);
            }
            collectionInteractive.setState(ContentStatusUtils.updatedStateInCollection(collectionInteractive.getState(), updatedInteractive.getState(), collectionInteractive.getLastEditedBy(), user));
        } else {
            collectionInteractive.setState(ContentStatus.Complete);
        }

        collectionInteractive.setLastEditedBy(user);

        Interactive interactive = interactivesClient.getInteractive(id);
        if (ObjectUtils.allNotNull(interactive, interactive.getMetadata())) {
            InteractiveMetadata metadata = interactive.getMetadata();
            boolean notAssociatedYet = StringUtils.isBlank(metadata.getCollectionId());
            
            if (!notAssociatedYet) {
                boolean associatedToAnotherCollection = !metadata.getCollectionId().equals(collection.getId());
                if (associatedToAnotherCollection) {
                    throw new ConflictException("cannot add interactive " + id
                            + " to collection " + collection.getId()
                            + " it is already in collection " + metadata.getCollectionId());
                }
            }

            if (StringUtils.isBlank(interactive.getURL())) {
                info().data("collectionId", collection.getDescription().getId())
                        .data("interactiveId", id)
                        .log("The interactive URL has not been set");
                throw new InvalidObjectException("The interactive URL has not been set on the dataset response");
            }

            if (notAssociatedYet) {
                interactivesClient.linkInteractiveToCollection(id, collection.getId());
            }

            if (! Arrays.isNullOrEmpty(interactive.getHTMLFiles())) {
                List<CollectionInteractiveFile> files = new ArrayList<>();
                for (InteractiveHTMLFile x : interactive.getHTMLFiles()) {
                    files.add(new CollectionInteractiveFile(x.getName(), x.getURI()));
                }
                collectionInteractive.setFiles(files.toArray(new CollectionInteractiveFile[0]));
            }
            collectionInteractive.setUri(interactive.getURI());
            collectionInteractive.setTitle(metadata.getTitle());
            collection.getDescription().addInteractive(collectionInteractive);
            collection.save();
        }

        return collectionInteractive;
    }

    @Override
    public void removeInteractiveFromCollection(Collection collection, String interactiveID) throws IOException, RuntimeException {

        // if its not in the collection then just return.
        Optional<CollectionInteractive> existingInteractive = collection.getDescription().getInteractive(interactiveID);
        if (!existingInteractive.isPresent()) {
            return;
        }

        try {
            interactivesClient.deleteInteractive(interactiveID);
        } catch (Exception e) {
            throw e;
        }

        collection.getDescription().removeInteractive(existingInteractive.get());
        collection.save();
    }

    @Override
    public void publishCollection(Collection collection) throws RuntimeException {
        String collectionId = collection.getDescription().getId();

        if (collectionId == null || collectionId.isEmpty()) {
            throw new IllegalArgumentException("a collectionId must be set in the collection being published");
        }

        interactivesClient.publishCollection(collectionId);
    }
}
