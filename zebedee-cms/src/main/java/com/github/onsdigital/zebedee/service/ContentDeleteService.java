package com.github.onsdigital.zebedee.service;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDeleteMarker;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.ContentDetailDescription;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Created by dave on 7/22/16.
 */
public class ContentDeleteService {

    private static final String JSON_FILE_EXT = ".json";
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

        for (ContentDeleteMarker marker : collection.description.getDeleteMarkers()) {
            Page page;
            try {
                pagesToDelete.add(reader.getPublishedContent(marker.getUri()));
            } catch (ZebedeeException | IOException ex) {
                pagesToDelete.add((Page) reader.getCollectionContent(collection.description.id, session.id, marker
                        .getUri()));
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

    public void addDeleteMarkerToCollection(Collection collection, ContentDeleteMarker... markers) throws ZebedeeException {
        List<ContentDeleteMarker> markersToAdd = Arrays.asList(markers).stream()
                .filter(marker -> !collection.description.getDeleteMarkers().contains(marker))
                .collect(Collectors.toList());

        if (!markersToAdd.isEmpty()) {
            collection.description.getDeleteMarkers().addAll(markersToAdd);
            saveManifest(collection);
        }
    }

    public boolean removeMarker(Collection collection, String contentUri) throws ZebedeeException {
        Optional<ContentDeleteMarker> marker = collection.description.getDeleteMarkers()
                .stream()
                .filter(deleteMarker -> contentUri.equalsIgnoreCase(deleteMarker.getUri()))
                .findFirst();

        if (marker.isPresent()) {
            collection.description.getDeleteMarkers().remove(marker.get());
            saveManifest(collection);
        }
        return marker.isPresent();
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
