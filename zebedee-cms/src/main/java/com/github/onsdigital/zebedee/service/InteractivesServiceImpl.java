package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dp.interactives.api.InteractivesAPIClient;
import com.github.onsdigital.dp.interactives.api.exceptions.NoInteractivesInCollectionException;
import com.github.onsdigital.dp.interactives.api.exceptions.ForbiddenException;
import com.github.onsdigital.dp.interactives.api.models.Interactive;
import com.github.onsdigital.dp.interactives.api.models.InteractiveHTMLFile;
import com.github.onsdigital.dp.interactives.api.models.InteractiveMetadata;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionInteractive;
import com.github.onsdigital.zebedee.json.CollectionInteractiveFile;
import com.github.onsdigital.zebedee.json.ContentStatus;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.Arrays;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;

/**
 * Interactives related services
 */
public class InteractivesServiceImpl implements InteractivesService {

    private InteractivesAPIClient interactivesClient;

    public InteractivesServiceImpl(InteractivesAPIClient interactivesClient) {
        this.interactivesClient = interactivesClient;
    }

    @Override
    public CollectionInteractive updateInteractiveInCollection(Collection collection, String id, CollectionInteractive updatedInteractive, String user) throws ZebedeeException, IOException {

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
        collectionInteractive.setLastEditedAt(new Date());

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
                throw new InternalServerError("The interactive URL has not been set on the interactive response");
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
    public void removeInteractiveFromCollection(Collection collection, String interactiveID) throws ZebedeeException, IOException {

        // if its not in the collection then just return.
        Optional<CollectionInteractive> existingInteractive = collection.getDescription().getInteractive(interactiveID);
        if (!existingInteractive.isPresent()) {
            return;
        }

        try {
            interactivesClient.deleteInteractive(interactiveID);
        } catch (NoInteractivesInCollectionException e) {
            warn().data("collectionId", collection.getDescription().getId())
                .data("interactiveId", interactiveID)
                .log("Interactives api could not find interactive. Deleting it from collection.");
        } catch (ForbiddenException e) {
                    warn().data("collectionId", collection.getDescription().getId())
                        .data("interactiveId", interactiveID)
                        .log("Interactives api, cannot delete a published interactive. Removing it from collection.");
        } catch (Exception e) {
            error().data("collectionId", collection.getDescription().getId())
                .data("interactiveId", interactiveID)
                .log(e.getMessage());
            throw new InternalServerError(e.getMessage(), e);
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
