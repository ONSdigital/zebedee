package com.github.onsdigital.zebedee.service;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.Event;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.PendingDelete;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.DeleteMarker;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDao;
import com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory;
import com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData;
import com.github.onsdigital.zebedee.service.content.navigation.ContentTreeNavigator;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ContentTree;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.github.onsdigital.zebedee.content.page.base.PageType.home_page;
import static com.github.onsdigital.zebedee.content.page.base.PageType.product_page;
import static com.github.onsdigital.zebedee.content.page.base.PageType.taxonomy_landing_page;
import static com.github.onsdigital.zebedee.exceptions.DeleteContentRequestDeniedException.alreadyMarkedDeleteInCurrentCollectionError;
import static com.github.onsdigital.zebedee.exceptions.DeleteContentRequestDeniedException.deleteForbiddenForPageTypeError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.DELETE_MARKED_ADDED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.DELETE_MARKED_REMOVED;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.deleteMarkerAdded;
import static com.github.onsdigital.zebedee.persistence.model.CollectionEventMetaData.deleteMarkerRemoved;

// TODO THIS MUST HAVE decent audit / collection history logging.

public class ContentDeleteService {

    private static final String JSON_FILE_EXT = ".json";
    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();
    private static ContentTreeNavigator contentTreeNavigator = ContentTreeNavigator.getInstance();
    private static CollectionHistoryDao collectionHistoryDao = CollectionHistoryDaoFactory.getCollectionHistoryDao();

    private static DeleteEventType DELETED_ADDED = new DeleteEventType(Audit.Event.DELETE_MARKER_ADDED,
            DELETE_MARKED_ADDED, EventType.DELETE_MARKER_ADDED);

    private static DeleteEventType DELETE_REMOVED = new DeleteEventType(Audit.Event.DELETE_MARKER_REMOVED,
            DELETE_MARKED_REMOVED, EventType.DELETE_MARKER_REMOVED);

    private static final ImmutableList<PageType> NON_DELETABLE_PAGE_TYPES =
            ImmutableList.of(home_page, taxonomy_landing_page, product_page);

    public static ContentDeleteService instance = null;

    public static ContentDeleteService getInstance() {
        if (instance == null) {
            instance = new ContentDeleteService();
        }
        return instance;
    }

    /**
     * Use get instance method.
     */
    private ContentDeleteService() {
    }

    public List<PendingDelete> getDeleteItemsByCollection(Collection collection) {
        List<PendingDelete> deletes = collection.getDescription().getPendingDeletes();

        deletes.stream().forEach(delete -> {
            LeafCounter leafCounter = new LeafCounter();
            contentTreeNavigator.applyAndPropagate(delete.getRoot(), (node -> {
                if (StringUtils.isNotEmpty(node.uri) && node.type != null) {
                    leafCounter.increment();
                }
            }));
            delete.setTotalDeletes(leafCounter.count);
        });
        return deletes;
    }

    public void addDeleteMarkerToCollection(Session session, Collection collection, DeleteMarker marker)
            throws ZebedeeException, IOException {
        // Page type not deletable.
        if (NON_DELETABLE_PAGE_TYPES.contains(marker.getType())) {
            throw deleteForbiddenForPageTypeError(marker.getType());
        }

        List<ContentDetail> matches = new ArrayList<>();
        collection.description.getPendingDeletes().stream()
                .forEach(pendingDelete -> contentTreeNavigator.search(pendingDelete.getRoot(), (node) -> {
                    if (node.contentPath.equals(marker.getUri())) {
                        matches.add(node);
                    }
                }));

        // Delete marker exists already.
        if (!matches.isEmpty()) {
            throw alreadyMarkedDeleteInCurrentCollectionError(collection, marker.getUri());
        }

        // Calculate all of the nodes that will be delete by this.
        ContentDetail deleteImpact = getAllDeletesForNode(marker);

        // Check they can all be deleted.
        isNodeDeletable(collection, deleteImpact, session);
        //updates all the children with deleted.
        List<String> deletedUris = new ArrayList<>();
        contentTreeNavigator.applyAndPropagate(deleteImpact,
                (node) -> {
                    node.setDeleteMarker(true);
                    deletedUris.add(node.contentPath);
                    logDebug("Marking node as deleted").path(node.contentPath).log();
                }
        );
        collection.description.getPendingDeletes().add(new PendingDelete(marker.getUser(), deleteImpact));
        logDeleteEvent(collection, session, deleteImpact.contentPath, DELETED_ADDED,
                deleteMarkerAdded(deleteImpact.contentPath, deletedUris));
        saveManifest(collection);
    }

    private void isNodeDeletable(Collection currentCollection, ContentDetail node, Session session)
            throws IOException, ZebedeeException {
        if (node == null) return;

        zebedeeCmsService.getZebedee().checkAllCollectionsForDeleteMarker(node.contentPath);
        zebedeeCmsService.getZebedee().isBeingEditedInAnotherCollection(currentCollection, node.contentPath, session);

        if (node.children == null || node.children.isEmpty()) return;

        for (ContentDetail child : node.children) {
            isNodeDeletable(currentCollection, child, session);
        }
    }


    public void cancelPendingDelete(Collection collection, Session session, String contentUri) throws ZebedeeException {
        List<String> cancelledDeleteUris = new ArrayList<>();
        collection.getDescription()
                .getPendingDeletes()
                .stream()
                .forEach(pd -> contentTreeNavigator.applyAndPropagate(pd.getRoot(),
                        (node) -> cancelledDeleteUris.add(node.contentPath)));

        logDeleteEvent(collection, session, contentUri, DELETE_REMOVED,
                deleteMarkerRemoved(contentUri, cancelledDeleteUris));
        collection.description.cancelPendingDelete(contentUri);
        saveManifest(collection);
    }

    public void overlayDeletedNodesInBrowseTree(ContentDetail browseTree) throws IOException {
        getAllDeleteMarkerUris()
                .stream()
                .forEach(deletedUri ->
                        contentTreeNavigator.updateNodeAndDescendants(browseTree,
                                deletedUri, (node) -> node.setDeleteMarker(true)));
    }

    public List<Path> getAllDeleteMarkerUris() throws IOException {
        // For each collection -> iterate over its pending deletes -> for each pending delete add its path to the
        // result list & then do the same for each of its children recursively.
        List<Path> allDeleteMarkers = new ArrayList<>();
        zebedeeCmsService.getZebedee().getCollections().list()
                .stream()
                .forEach(collection -> {
                    collection.description
                            .getPendingDeletes()
                            .stream()
                            .forEach(contentDetailsToPathList(allDeleteMarkers));
                });
        return allDeleteMarkers;
    }

    public List<String> getCollectionDeleteUrisAsList(Collection collection) {
        List<String> uris = new ArrayList<>();
        collection.description.getPendingDeletes().forEach(pendingDelete -> {
            contentTreeNavigator.applyAndPropagate(pendingDelete.getRoot(), (node) -> {
                if (node.type != null && StringUtils.isNotEmpty(node.uri)) {
                    uris.add(node.uri);
                }
            });
        });
        return uris;
    }

    private void saveManifest(Collection collection) throws ZebedeeException {
        try (OutputStream output = Files.newOutputStream(manifestPath(collection))) {
            Serialiser.serialise(output, collection.description);
        } catch (IOException e) {
            // TODO probably want exception type for this.
            logError(e, "Error while serialising delete markers...").logAndThrow(BadRequestException.class);
        }
    }

    public ContentDetail getAllDeletesForNode(DeleteMarker nodeToDelete)
            throws IOException {
        ContentDetail wholeTree = ContentTree.get();
        Optional<ContentDetail> branch = contentTreeNavigator.findContentDetail(wholeTree, nodeToDelete.getPath());
        if (branch.isPresent()) {
            return branch.get();
        }
        throw new RuntimeException("CANNOT FIND PATH IN BROWSE TREE");
    }

    private Path manifestPath(com.github.onsdigital.zebedee.model.Collection collection) {
        return Paths.get(collection.path.toString() + JSON_FILE_EXT);
    }

    private Consumer<PendingDelete> contentDetailsToPathList(List<Path> resultsList) {
        return (pendingDelete) ->
                contentTreeNavigator.search(pendingDelete.getRoot(),
                        (node) -> resultsList.add(Paths.get(node.contentPath)));
    }

    private class LeafCounter {

        private int count;

        public LeafCounter() {
            this.count = 0;
        }

        public void increment() {
            this.count++;
        }

        public int getCount() {
            return count;
        }
    }

    private Event collectionEvent(Session session, EventType eventType) {
        return new Event(DateTime.now().toDate(), eventType, session.getEmail());
    }

    private void logDeleteEvent(Collection collection, Session session, String contentUri,
                                DeleteEventType event, CollectionEventMetaData[] collectionEventMetaData) {
        collectionHistoryDao.saveCollectionHistoryEvent(collection, session,
                event.collectionEventType, collectionEventMetaData);
        collection.addEvent(contentUri, collectionEvent(session, event.eventType));
        event.auditEvent.parameters().collection(collection).user(session.getEmail()).content(contentUri).log();
    }

    private static class DeleteEventType {
        private Audit.Event auditEvent;
        private CollectionEventType collectionEventType;
        private EventType eventType;

        public DeleteEventType(Audit.Event auditEvent, CollectionEventType collectionEventType, EventType eventType) {
            this.auditEvent = auditEvent;
            this.collectionEventType = collectionEventType;
            this.eventType = eventType;
        }
    }
}
