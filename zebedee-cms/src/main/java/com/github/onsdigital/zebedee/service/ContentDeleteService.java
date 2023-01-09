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

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.zebedee.content.page.base.PageType.HOME_PAGE;
import static com.github.onsdigital.zebedee.content.page.base.PageType.PRODUCT_PAGE;
import static com.github.onsdigital.zebedee.content.page.base.PageType.TAXONOMY_LANDING_PAGE;
import static com.github.onsdigital.zebedee.exceptions.DeleteContentRequestDeniedException.alreadyMarkedDeleteInCurrentCollectionError;
import static com.github.onsdigital.zebedee.exceptions.DeleteContentRequestDeniedException.deleteForbiddenForPageTypeError;

public class ContentDeleteService {

    private static final String JSON_FILE_EXT = ".json";
    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();
    private static ContentTreeNavigator contentTreeNavigator = ContentTreeNavigator.getInstance();

    private static final DeleteEventType DELETED_ADDED = new DeleteEventType(Audit.Event.DELETE_MARKER_ADDED,
            EventType.DELETE_MARKER_ADDED);

    private static final DeleteEventType DELETE_REMOVED = new DeleteEventType(Audit.Event.DELETE_MARKER_REMOVED,
            EventType.DELETE_MARKER_REMOVED);

    private static final ImmutableList<PageType> NON_DELETABLE_PAGE_TYPES =
            ImmutableList.of(HOME_PAGE, TAXONOMY_LANDING_PAGE, PRODUCT_PAGE);

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
                if (StringUtils.isNotEmpty(node.uri) && node.getType() != null) {
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
        collection.getDescription().getPendingDeletes().stream()
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
                    deletedUris.add(node.contentPath);
                    info().data("path", node.contentPath).log("Marking node as deleted");
                }
        );
        collection.getDescription().getPendingDeletes().add(new PendingDelete(marker.getUser(), deleteImpact));
        logDeleteEvent(collection, session, deleteImpact.contentPath, DELETED_ADDED);
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

        logDeleteEvent(collection, session, contentUri, DELETE_REMOVED);
        collection.getDescription().cancelPendingDelete(contentUri);
        saveManifest(collection);
    }

    private void saveManifest(Collection collection) throws ZebedeeException {
        try (OutputStream output = Files.newOutputStream(manifestPath(collection))) {
            Serialiser.serialise(output, collection.getDescription());
        } catch (IOException e) {
            // TODO probably want exception type for this.
            error().logException(e, "Error while serialising delete markers...");
            throw new BadRequestException("Unexpected error while attempting to save manifest.");
        }
    }

    private ContentDetail getAllDeletesForNode(DeleteMarker nodeToDelete)
            throws IOException {
        ContentDetail wholeTree = ContentTree.get();
        Optional<ContentDetail> branch = contentTreeNavigator.findContentDetail(wholeTree, nodeToDelete.getPath());
        if (branch.isPresent()) {
            return branch.get();
        }
        throw new RuntimeException("CANNOT FIND PATH IN BROWSE TREE");
    }

    private Path manifestPath(com.github.onsdigital.zebedee.model.Collection collection) {
        return Paths.get(collection.getPath().toString() + JSON_FILE_EXT);
    }

    private class LeafCounter {

        private int count;

        public LeafCounter() {
            this.count = 0;
        }

        public void increment() {
            this.count++;
        }
    }

    private Event collectionEvent(Session session, EventType eventType) {
        return new Event(DateTime.now().toDate(), eventType, session.getEmail());
    }

    private void logDeleteEvent(Collection collection, Session session, String contentUri,
                                DeleteEventType event) {
        collection.addEvent(contentUri, collectionEvent(session, event.eventType));
        event.auditEvent.parameters().collection(collection).user(session.getEmail()).content(contentUri).log();
    }

    private static class DeleteEventType {
        private Audit.Event auditEvent;
        private EventType eventType;

        public DeleteEventType(Audit.Event auditEvent, EventType eventType) {
            this.auditEvent = auditEvent;
            this.eventType = eventType;
        }
    }
}
