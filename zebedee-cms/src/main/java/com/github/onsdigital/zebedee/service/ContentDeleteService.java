package com.github.onsdigital.zebedee.service;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.DeleteContentRequestDeniedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.DeleteMarkerJson;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.ContentDetailDescription;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.DeleteMarker;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.onsdigital.zebedee.exceptions.DeleteContentRequestDeniedException.BlockReason.ALREADY_MARKED_BY_THIS_COLLECTION;
import static com.github.onsdigital.zebedee.exceptions.DeleteContentRequestDeniedException.BlockReason.BEING_EDITED_BY_ANOTHER_COLLECTION;
import static com.github.onsdigital.zebedee.exceptions.DeleteContentRequestDeniedException.BlockReason.MARKED_BY_ANOTHER_COLLECTION;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Created by dave on 7/22/16.
 */
public class ContentDeleteService {

    private static final String JSON_FILE_EXT = ".json";
    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();
    public static ContentDeleteService instance = null;

    public static ContentDeleteService getInstance() {
        if (instance == null) {
            instance = new ContentDeleteService();
        }
        return instance;
    }

    private ContentDeleteService() {
        // Use get instance method.
    }

    public List<ContentDetail> getDeleteItemsByCollection(Collection collection, Session session) throws ZebedeeException,
            IOException {
        List<Page> pagesToDelete = new ArrayList<>();
        ZebedeeReader reader = new ZebedeeReader();

        for (DeleteMarker marker : collection.description.getDeleteMarkers()) {
            DeleteMarkerJson json = DeleteMarker.markerToJson(marker);
            Page page;
            try {
                pagesToDelete.add(reader.getPublishedContent(json.getUri()));
            } catch (ZebedeeException | IOException ex) {
                pagesToDelete.add((Page) reader.getCollectionContent(collection.description.id, session.id,
                        json.getUri()));
            }
        }

        List<ContentDetail> result = new ArrayList<>();
        pagesToDelete.stream()
                .forEach(page -> {
                    ContentDetailDescription desc = new ContentDetailDescription(page.getDescription().getTitle())
                            .setEdition(page.getDescription().getEdition())
                            .setLanguage(page.getDescription().getLanguage());
                    result.add(new ContentDetail(desc, page.getUri().toString(), page.getType().name()));
                });
        return result;
    }

    public void addDeleteMarkerToCollection(Collection collection, DeleteMarker marker)
            throws ZebedeeException, IOException {
        if (collection.description.getDeleteMarkers().contains(marker)) {
            throw new DeleteContentRequestDeniedException(collection, ALREADY_MARKED_BY_THIS_COLLECTION);
        }

        Optional<Collection> blockingCollection = zebedeeCmsService.getZebedee()
                .checkAllCollectionsForDeleteMarker(marker.getUri());

        if (blockingCollection.isPresent()) {
            throw new DeleteContentRequestDeniedException(blockingCollection.get(), MARKED_BY_ANOTHER_COLLECTION);
        }

        blockingCollection = zebedeeCmsService.getZebedee().isBeingEditedInAnotherCollection(marker.getUri());

        if (blockingCollection.isPresent()) {
            throw new DeleteContentRequestDeniedException(blockingCollection.get(), BEING_EDITED_BY_ANOTHER_COLLECTION);
        }

        collection.description.getDeleteMarkers().add(marker);
        saveManifest(collection);
        logDebug("Content marked for delete").addParameter("target", marker).log();
    }

    public boolean removeMarker(Collection collection, String contentUri) throws ZebedeeException {
        DeleteMarker deleteMarker = new DeleteMarker().setUri(contentUri);
        Optional<DeleteMarker> deleteMarkerOptional = collection.description.getDeleteMarkers()
                .stream()
                .filter(existingMarker -> deleteMarker.getUri().equalsIgnoreCase(existingMarker.getUri()))
                .findFirst();

        if (deleteMarkerOptional.isPresent()) {
            collection.description.getDeleteMarkers().remove(deleteMarkerOptional.get());
            saveManifest(collection);
        }
        return deleteMarkerOptional.isPresent();
    }

    private void saveManifest(Collection collection) throws ZebedeeException {
        try (OutputStream output = Files.newOutputStream(manifestPath(collection))) {
            Serialiser.serialise(output, collection.description);
        } catch (IOException e) {
            // TODO probably want exception type for this.
            logError(e, "Error while serialising delete markers...").logAndThrow(BadRequestException.class);
        }
    }

    private Path manifestPath(com.github.onsdigital.zebedee.model.Collection collection) {
        return Paths.get(collection.path.toString() + JSON_FILE_EXT);
    }

}
