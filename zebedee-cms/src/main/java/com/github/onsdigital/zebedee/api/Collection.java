package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import java.io.IOException;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

@Api
public class Collection {

    static final String COLLECTION_NAME = "collectionName";

    /**
     * Retrieves a CollectionDescription object at the endpoint /Collection/[CollectionName]
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If no collection exists:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                 </ul>
     * @return the CollectionDescription.
     * @throws IOException
     */
    @GET
    public CollectionDescription get(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ZebedeeException {
        info().log("get collection endpoint: request received");

        Session session = Root.zebedee.getSessions().get(request);
        if (session == null) {
            info().log("get collection endpoint: request unsuccessful valid session for user was not found.");
            throw new UnauthorizedException("You are not authorised to view collections.");
        }

        com.github.onsdigital.zebedee.model.Collection collection = Collections
                .getCollection(request);

        // Check whether we found the collection:
        if (collection == null) {
            info().log("get collection endpoint: request unsuccessful collection not found");
            throw new NotFoundException("The collection you are trying to get was not found.");
        }

        String collectionId = Collections.getCollectionId(request);

        boolean canView = Root.zebedee.getPermissionsService().canView(session.getEmail(), collection.getDescription());
        if (!canView) {
            warn().data("user", session.getEmail()).data("collectionId", collectionId).log("get collection endpoint: request unsuccessful user denied canView permission");
            throw new UnauthorizedException("You are not authorised to view collections.");
        }

        // Collate the result:
        CollectionDescription result = new CollectionDescription();
        result.setId(collection.getDescription().getId());
        result.setName(collection.getDescription().getName());
        result.setPublishDate(collection.getDescription().getPublishDate());
        result.inProgressUris = collection.inProgressUris();
        result.completeUris = collection.completeUris();
        result.reviewedUris = collection.reviewedUris();
        result.eventsByUri = collection.getDescription().eventsByUri;
        result.setApprovalStatus(collection.getDescription().approvalStatus);
        result.setType(collection.getDescription().getType());
        result.setTeams(collection.getDescription().getTeams());
        result.isEncrypted = collection.getDescription().isEncrypted;
        result.setReleaseUri(collection.getDescription().getReleaseUri());

        info().data("user", session.getEmail()).data("collectionId", collectionId).log("get collection endpoint: request compeleted successfully");
        return result;
    }

    /**
     * Creates or updates collection details the endpoint /Collection/
     * <p>
     * Checks if a collection exists using {@link CollectionDescription#name}
     *
     * @param request               This should contain a X-Florence-Token header for the current session
     * @param response              <ul>
     *                              <li>If no name has been passed:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                              <li>If user cannot create collections:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                              <li>If collection with name already exists:  {@link HttpStatus#CONFLICT_409}</li>
     *                              </ul>
     * @param collectionDescription
     * @return
     * @throws IOException
     */
    @POST
    public CollectionDescription create(HttpServletRequest request,
                                        HttpServletResponse response,
                                        CollectionDescription collectionDescription) throws IOException, ZebedeeException {
        info().log("create collection endpoint: request received");

        Session session = Root.zebedee.getSessions().get(request);
        if (session == null) {
            warn().log("create collection endpoint: request unsuccessful no valid session found");
            throw new UnauthorizedException("You are not authorised to create collections.");
        }

        if (StringUtils.isBlank(collectionDescription.getName())) {
            warn().log("create collection endpoint: request unsuccessful collection name is blank");
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return null;
        }

        boolean canEdit = Root.zebedee.getPermissionsService().canEdit(session.getEmail());
        if (!canEdit) {
            warn().data("user", session.getEmail()).log("create collection endpoint: request unsuccessful:user denied canEdit permission");
            throw new UnauthorizedException("You are not authorised to create collections.");
        }

        info().data("user", session.getEmail()).log("create collection endpoint: user granted canEdit permission");

        Keyring keyring = Root.zebedee.getKeyringCache().get(session);
        if (keyring == null) {
            warn().data("user", session.getEmail()).log("create collection endpoint: request unsuccessful Keyring is not initialised");
            throw new UnauthorizedException("Keyring is not initialised.");
        }

        collectionDescription.setName(StringUtils.trim(collectionDescription.getName()));
        if (Root.zebedee.getCollections().list().hasCollection(
                collectionDescription.getName())) {
            warn().data("user", session.getEmail()).data(COLLECTION_NAME, collectionDescription.getName())
                    .log("create collection endpoint: request unsuccessful a collection already exists with this name");
            throw new ConflictException("Could not create collection. A collection with this name already exists.");
        }

        com.github.onsdigital.zebedee.model.Collection collection = com.github.onsdigital.zebedee.model.Collection.create(
                collectionDescription, Root.zebedee, session);

        String collectionId = Collections.getCollectionId(request);

        if (collection.getDescription().getType().equals(CollectionType.scheduled)) {
            info().data("user", session.getEmail()).data("collectionId", collectionId)
                    .log("create collection endpoint: adding scheduled collection to publishing queue");
            Root.schedulePublish(collection);
        }

        Audit.Event.COLLECTION_CREATED
                .parameters()
                .host(request)
                .collection(collection)
                .actionedBy(session.getEmail())
                .log();

        info().data("user", session.getEmail()).data("collectionId", collectionId).log("create collection endpoint: request completed successfully");
        return collection.getDescription();
    }

    @PUT
    public CollectionDescription update(
            HttpServletRequest request,
            HttpServletResponse response,
            CollectionDescription collectionDescription
    ) throws IOException, ZebedeeException {

        info().log("update collection endpoint: request received");

        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        if (collection == null) {
            warn().log("update collection endpoint: request unsuccessful collection not found");
            throw new NotFoundException("The collection you are trying to update was not found.");
        }

        String collectionId = Collections.getCollectionId(request);

        Session session = Root.zebedee.getSessions().get(request);
        if (session == null) {
            warn().data("user", session.getEmail()).data("collectionId", collectionId).log("update collection endpoint: request unsuccessful no valid session found");
            throw new UnauthorizedException("You are not authorised to update collections.");
        }

        boolean canEdit = Root.zebedee.getPermissionsService().canEdit(session.getEmail());
        if (!canEdit) {
            warn().data("user", session.getEmail()).data("collectionId", collectionId).log("update collection endpoint: request unsuccessful user denied canEdit permission");
            throw new UnauthorizedException("You are not authorised to update collections.");
        }

        info().data("user", session.getEmail()).data("collectionId", collectionId).log("update collection endpoint: user granted canEdit permission");

        com.github.onsdigital.zebedee.model.Collection updatedCollection = collection.update(
                collection,
                collectionDescription,
                Root.zebedee,
                Root.getScheduler(),
                session);

        Audit.Event.COLLECTION_UPDATED
                .parameters()
                .host(request)
                .fromTo(collection.path.toString(), updatedCollection.path.toString())
                .actionedBy(session.getEmail())
                .log();

        info().data("user", session.getEmail()).data("collectionId", collectionId).log("update collection endpoint: request completed successfully");
        return updatedCollection.getDescription();
    }

    /**
     * Deletes the collection details at the endpoint /Collection/[CollectionName]
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If the collection doesn't exist:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                 <li>If user is not authorised to delete this collection:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                 <li>If the collection has contents preventing deletion:  {@link HttpStatus#CONFLICT_409}</li>
     *                 </ul>
     * @return
     * @throws IOException
     */
    @DELETE
    public boolean deleteCollection(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ZebedeeException {
        info().log("delete collection endpoint: request received");

        Session session = Root.zebedee.getSessions().get(request);
        if (session == null) {
            error().log("delete collection endpoint: request unsuccessful no valid session found");
            throw new UnauthorizedException("You are not authorised to delete collections.");
        }

        String collectionId = Collections.getCollectionId(request);

        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        if (collection == null) {
            warn().data("user", session.getEmail()).data("collectionId", collectionId).log("delete collection endpoint: request unsuccessful collection not found");
            throw new NotFoundException("The collection you are trying to delete was not found");
        }

        Root.zebedee.getCollections().delete(collection, session);

        Root.cancelPublish(collection);

        Audit.Event.COLLECTION_DELETED
                .parameters()
                .host(request)
                .collection(collection)
                .actionedBy(session.getEmail())
                .log();

        info().data("user", session.getEmail()).data("collectionId", collectionId).log("delete collection endpoint: request completed successfully");
        return true;
    }
}
