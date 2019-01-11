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

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logTrace;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;

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
        logInfo("get collection endpoint: request received").log();

        Session session = Root.zebedee.getSessionsService().get(request);
        if (session == null) {
            logInfo("get collection endpoint: request unsuccessful valid session for user was not found").log();
            throw new UnauthorizedException("You are not authorised to view collections.");
        }

        com.github.onsdigital.zebedee.model.Collection collection = Collections
                .getCollection(request);

        // Check whether we found the collection:
        if (collection == null) {
            logInfo("get collection endpoint: request unsuccessful collection not found").user(session.getEmail()).log();
            throw new NotFoundException("The collection you are trying to get was not found.");
        }

        boolean canView = Root.zebedee.getPermissionsService().canView(session.getEmail(), collection.getDescription());
        if (!canView) {
            logWarn("get collection endpoint: request unsuccessful user denied canView permission")
                    .user(session.getEmail())
                    .collectionId(collection)
                    .log();
            throw new UnauthorizedException("You are not authorised to view collections.");
        }
        logTrace("get collection endpoint: user granted canView permission")
                .collectionId(collection)
                .log();

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

        logInfo("get collection endpoint: request compeleted successfully")
                .collectionId(collection)
                .user(session.getEmail())
                .log();
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
        logInfo("create collection endpoint: request received").log();

        Session session = Root.zebedee.getSessionsService().get(request);
        if (session == null) {
            logWarn("create collection request unsuccessful: no valid session found").log();
            throw new UnauthorizedException("You are not authorised to create collections.");
        }

        if (StringUtils.isBlank(collectionDescription.getName())) {
            logWarn("create collection request unsuccessful: collection name is blank").log();
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return null;
        }

        boolean canEdit = Root.zebedee.getPermissionsService().canEdit(session.getEmail());
        if (!canEdit) {
            logWarn("create collection request unsuccessful: user denied canEdit permission")
                    .user(session.getEmail())
                    .log();
            throw new UnauthorizedException("You are not authorised to create collections.");
        }

        logDebug("create collection endpoint: user granted canEdit permission")
                .user(session.getEmail())
                .log();

        Keyring keyring = Root.zebedee.getKeyringCache().get(session);
        if (keyring == null) {
            logWarn("create collection request unsuccessful: Keyring is not initialised")
                    .user(session.getEmail())
                    .log();
            throw new UnauthorizedException("Keyring is not initialised.");
        }

        collectionDescription.setName(StringUtils.trim(collectionDescription.getName()));
        if (Root.zebedee.getCollections().list().hasCollection(
                collectionDescription.getName())) {
            logWarn("create collection request unsuccessful: a collection already exists with this name")
                    .user(session.getEmail())
                    .param(COLLECTION_NAME, collectionDescription.getName())
                    .log();
            throw new ConflictException("Could not create collection. A collection with this name already exists.");
        }

        com.github.onsdigital.zebedee.model.Collection collection = com.github.onsdigital.zebedee.model.Collection.create(
                collectionDescription, Root.zebedee, session);

        if (collection.getDescription().getType().equals(CollectionType.scheduled)) {
            logInfo("create collection endpoint: adding scheduled collection to publishing queue")
                    .user(session.getEmail())
                    .collectionId(collection)
                    .log();
            Root.schedulePublish(collection);
        }

        Audit.Event.COLLECTION_CREATED
                .parameters()
                .host(request)
                .collection(collection)
                .actionedBy(session.getEmail())
                .log();

        logInfo("create collection endpoint: request completed successfully")
                .user(session.getEmail())
                .collectionId(collection)
                .log();
        return collection.getDescription();
    }

    @PUT
    public CollectionDescription update(
            HttpServletRequest request,
            HttpServletResponse response,
            CollectionDescription collectionDescription
    ) throws IOException, ZebedeeException {

        Session session = Root.zebedee.getSessionsService().get(request);
        if (!Root.zebedee.getPermissionsService().canEdit(session.getEmail())) {
            throw new UnauthorizedException("You are not authorised to update collections.");
        }

        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
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

        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        Session session = Root.zebedee.getSessionsService().get(request);

        Root.zebedee.getCollections().delete(collection, session);

        Root.cancelPublish(collection);

        Audit.Event.COLLECTION_DELETED
                .parameters()
                .host(request)
                .collection(collection)
                .actionedBy(session.getEmail())
                .log();

        return true;
    }
}
