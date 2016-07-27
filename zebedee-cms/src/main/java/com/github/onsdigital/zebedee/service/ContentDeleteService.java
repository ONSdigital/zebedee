package com.github.onsdigital.zebedee.service;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.ContentDetailDescription;
import com.github.onsdigital.zebedee.json.DeleteMarkerJson;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.DeleteMarker;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;
import com.github.onsdigital.zebedee.service.content.navigation.ContentDetailModifier;
import com.github.onsdigital.zebedee.service.content.navigation.ContentTreeNavigator;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.content.page.base.PageType.home_page;
import static com.github.onsdigital.zebedee.content.page.base.PageType.product_page;
import static com.github.onsdigital.zebedee.content.page.base.PageType.taxonomy_landing_page;
import static com.github.onsdigital.zebedee.exceptions.DeleteContentRequestDeniedException.alreadyMarkedDeleteInCurrentCollectionError;
import static com.github.onsdigital.zebedee.exceptions.DeleteContentRequestDeniedException.beingEditedByAnotherCollectionError;
import static com.github.onsdigital.zebedee.exceptions.DeleteContentRequestDeniedException.deleteForbiddenForPageTypeError;
import static com.github.onsdigital.zebedee.exceptions.DeleteContentRequestDeniedException.markedDeleteInAnotherCollectionError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Created by dave on 7/22/16.
 */
public class ContentDeleteService {

    private static final String JSON_FILE_EXT = ".json";
    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();
    private static ContentTreeNavigator contentTreeNavigator = ContentTreeNavigator.getInstance();

    // Page types that cannot be deleted.
    private static final List<PageType> PAGE_TYPE_DELETE_BLACKLIST =
            Arrays.asList(new PageType[]{home_page, taxonomy_landing_page, product_page});

    public static ContentDeleteService instance = null;

    static Path DATA_JSON = Paths.get("data.json");

    private Function<Collection, List<Path>> collectionDeletedPaths =
            (collection) -> collection.description.getDeleteMarkedContentUris()
                    .stream()
                    .map(item -> Paths.get(item))
                    .collect(Collectors.toList());

    public static ContentDeleteService getInstance() {
        if (instance == null) {
            instance = new ContentDeleteService();
        }
        return instance;
    }

    private static ContentDetailModifier nodeDeleterMarker = (node) -> {
        node.setDeleteMarker(true);
        logDebug("Marking node as deleted").path(node.contentPath).log();
    };

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
                    result.add(new ContentDetail(desc, page.getUri().toString(), page.getType().name(),
                            page.getUri().toString()));
                });
        return result;
    }

    public void addDeleteMarkerToCollection(Collection collection, DeleteMarker marker)
            throws ZebedeeException, IOException {

        if (PAGE_TYPE_DELETE_BLACKLIST.contains(marker.getType())) {
            throw deleteForbiddenForPageTypeError(marker.getType());
        }

        if (collection.description.getDeleteMarkers().contains(marker)) {
            throw alreadyMarkedDeleteInCurrentCollectionError(collection);
        }

        Optional<Collection> blockingCollection = zebedeeCmsService.getZebedee()
                .checkAllCollectionsForDeleteMarker(marker.getUri());

        if (blockingCollection.isPresent()) {
            throw markedDeleteInAnotherCollectionError(blockingCollection.get());
        }

        blockingCollection = zebedeeCmsService.getZebedee().isBeingEditedInAnotherCollection(marker.getUri());

        if (blockingCollection.isPresent()) {
            throw beingEditedByAnotherCollectionError(blockingCollection.get());
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

    public void overlayDeletedNodesInBrowseTree(ContentDetail browseTree) throws IOException {
        getAllDeleteMarkerUris()
                .stream()
                .forEach(deletedUri ->
                        contentTreeNavigator.updateNodeAndDescendants(browseTree, deletedUri, nodeDeleterMarker));
    }

    public List<Path> getAllDeleteMarkerUris() throws IOException {
        List<Path> allDeleteMarkers = new ArrayList<>();
        zebedeeCmsService.getZebedee().collections.list()
                .stream()
                .forEach(collection -> allDeleteMarkers.addAll(collectionDeletedPaths.apply(collection)));
        return allDeleteMarkers;
    }

    public boolean hasDeleteMarker(Path uri) throws IOException {
        final Path targetPath = !uri.getFileName().equals(DATA_JSON) ? uri.resolve(DATA_JSON) : uri;

        Path publishedContentPath = zebedeeCmsService.getZebedee().publishedContentPath;
        return getAllDeleteMarkerUris().stream().filter(deletedUri ->
                publishedContentPath.resolve(deletedUri).equals(targetPath)
        ).findFirst().isPresent();
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
