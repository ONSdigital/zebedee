package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.keyring.Keyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.keyring.KeyringUtil;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.warn;
import static java.text.MessageFormat.format;
import static org.apache.commons.lang3.StringUtils.trim;


@Api
public class Collection {

    static final String COLLECTION_NAME = "collectionName";

    private Sessions sessionsService;
    private PermissionsService permissionsService;
    private com.github.onsdigital.zebedee.model.Collections collections;
    private UsersService usersService;
    private Keyring keyring;
    private ScheduleCanceller scheduleCanceller;

    /**
     * Construct a new instance of the Collection API endpoint.
     */
    public Collection() {
        this.sessionsService = Root.zebedee.getSessions();
        this.permissionsService = Root.zebedee.getPermissionsService();
        this.collections = Root.zebedee.getCollections();
        this.usersService = Root.zebedee.getUsersService();
        this.keyring = Root.zebedee.getCollectionKeyring();
        this.scheduleCanceller = (c) -> Root.cancelPublish(c);
    }

    /**
     * Constructor for testing.
     */
    Collection(final Sessions sessionsService, final PermissionsService permissionsService,
               final com.github.onsdigital.zebedee.model.Collections collections,
               final UsersService usersService, final Keyring keyring,
               final ScheduleCanceller scheduleCanceller) {
        this.sessionsService = sessionsService;
        this.permissionsService = permissionsService;
        this.collections = collections;
        this.usersService = usersService;
        this.keyring = keyring;
        this.scheduleCanceller = scheduleCanceller;
    }

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

        Session session = sessionsService.get(request);
        if (session == null) {
            info().log("get collection endpoint: request unsuccessful valid session for user was not found.");
            throw new UnauthorizedException("You are not authorised to view collections.");
        }

        String collectionID = collections.getCollectionId(request);
        if (StringUtils.isEmpty(collectionID)) {
            throw new BadRequestException("collection ID required but was null/empty");
        }

        com.github.onsdigital.zebedee.model.Collection collection = collections.getCollection(collectionID);
        if (collection == null) {
            info().log("get collection endpoint: request unsuccessful collection not found");
            throw new NotFoundException("The collection you are trying to get was not found.");
        }

        requireViewPermission(session.getEmail(), collection.getDescription());

        // TODO: Question - I am not sure why it's necessary to duplicate the description instead of just returning it?
        // Collate the result:
        CollectionDescription result = new CollectionDescription();
        result.setId(collection.getDescription().getId());
        result.setName(collection.getDescription().getName());
        result.setPublishDate(collection.getDescription().getPublishDate());
        result.setInProgressUris(collection.inProgressUris());
        result.setCompleteUris(collection.completeUris());
        result.setReviewedUris(collection.reviewedUris());
        result.setEventsByUri(collection.getDescription().getEventsByUri());
        result.setApprovalStatus(collection.getDescription().getApprovalStatus());
        result.setType(collection.getDescription().getType());
        result.setTeams(collection.getDescription().getTeams());
        result.setEncrypted(collection.getDescription().isEncrypted());
        result.setReleaseUri(collection.getDescription().getReleaseUri());

        info().user(session.getEmail())
                .collectionID(collectionID)
                .log("get collection endpoint: request compeleted successfully");

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
    public CollectionDescription create(HttpServletRequest request, HttpServletResponse response,
                                        CollectionDescription collectionDescription)
            throws IOException, ZebedeeException {
        info().log("create collection endpoint: request received");

        Session session = sessionsService.get(request);
        if (session == null) {
            warn().log("create collection endpoint: request unsuccessful no valid session found");
            throw new UnauthorizedException("You are not authorised to create collections.");
        }

        if (StringUtils.isBlank(collectionDescription.getName())) {
            warn().log("create collection endpoint: request unsuccessful collection name is blank");
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return null;
        }

        requireEditPermission(session.getEmail());

        info().user(session.getEmail()).log("create collection endpoint: user granted canEdit permission");

        collectionDescription.setName(trim(collectionDescription.getName()));
        if (collections.list().hasCollection(collectionDescription.getName())) {
            warn().user(session.getEmail())
                    .data(COLLECTION_NAME, collectionDescription.getName())
                    .log("create collection endpoint: request unsuccessful a collection already exists with this name");
            throw new ConflictException("Could not create collection. A collection with this name already exists.");
        }


        com.github.onsdigital.zebedee.model.Collection collection =
                com.github.onsdigital.zebedee.model.Collection.create(collectionDescription, Root.zebedee, session);

        String collectionId = Collections.getCollectionId(request);

        if (collection.getDescription().getType().equals(CollectionType.scheduled)) {
            info().user(session.getEmail())
                    .collectionID(collectionId)
                    .log("create collection endpoint: adding scheduled collection to publishing queue");
            Root.schedulePublish(collection);
        }

        Audit.Event.COLLECTION_CREATED
                .parameters()
                .host(request)
                .collection(collection)
                .actionedBy(session.getEmail())
                .log();

        info().user(session.getEmail())
                .collectionID(collectionId)
                .log("create collection endpoint: request completed successfully");

        return collection.getDescription();
    }

    @PUT
    public CollectionDescription update(HttpServletRequest request, HttpServletResponse response,
                                        CollectionDescription collectionDescription)
            throws IOException, ZebedeeException {

        info().log("update collection endpoint: request received");

        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        if (collection == null) {
            warn().log("update collection endpoint: request unsuccessful collection not found");
            throw new NotFoundException("The collection you are trying to update was not found.");
        }

        String collectionId = Collections.getCollectionId(request);

        Session session = sessionsService.get(request);
        if (session == null) {
            warn().user(session.getEmail())
                    .collectionID(collectionId)
                    .log("update collection endpoint: request unsuccessful no valid session found");
            throw new UnauthorizedException("You are not authorised to update collections.");
        }

        requireEditPermission(session.getEmail());

        info().user(session.getEmail())
                .collectionID(collectionId)
                .log("update collection endpoint: user granted canEdit permission");

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

        info().user(session.getEmail())
                .collectionID(collectionId)
                .log("update collection endpoint: request completed successfully");
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

        Session session = sessionsService.get(request);
        if (session == null) {
            error().log("delete collection endpoint: request unsuccessful no valid session found");
            throw new UnauthorizedException("You are not authorised to delete collections.");
        }

        String collectionId = Collections.getCollectionId(request);

        com.github.onsdigital.zebedee.model.Collection collection = collections.getCollection(collectionId);
        if (collection == null) {
            warn().user(session.getEmail())
                    .collectionID(collectionId)
                    .log("delete collection endpoint: request unsuccessful collection not found");
            throw new NotFoundException("The collection you are trying to delete was not found");
        }

        deleteCollection(collection, session);

        Audit.Event.COLLECTION_DELETED
                .parameters()
                .host(request)
                .collection(collection)
                .actionedBy(session.getEmail())
                .log();

        info().user(session.getEmail())
                .collectionID(collectionId)
                .log("delete collection endpoint: request completed successfully");
        return true;
    }

    private void deleteCollection(com.github.onsdigital.zebedee.model.Collection c, Session s)
            throws InternalServerError, NotFoundException, BadRequestException {
        // Delete the collection.
        try {
            collections.delete(c, s);
        } catch (Exception ex) {
            String message = format("error attempting to delete collection: {0}", c.getId());
            throw new InternalServerError(message, ex);
        }

        // Cancel any scheduled publish for this collection.
        scheduleCanceller.cancel(c);

        // Remove the collection encryption key from the keyring
        User user = KeyringUtil.getUser(usersService, s.getEmail());
        try {
            keyring.remove(user, c);
        } catch (KeyringException ex) {
            String message = format("error attempting to remove collection key from keyring: {0}", c.getId());
            throw new InternalServerError(message, ex);
        }
    }

    private void requireViewPermission(String email, CollectionDescription description) throws UnauthorizedException {
        boolean canView = false;
        try {
            canView = permissionsService.canView(email, description);
        } catch (IOException ex) {
            throw new UnauthorizedException("You are not authorised to view this collection");
        }

        if (!canView) {
            warn().user(email)
                    .collectionID(description)
                    .log("request unsuccessful user denied view permission");

            throw new UnauthorizedException("You are not authorised to view this collection");
        }
    }

    private void requireEditPermission(String email) throws UnauthorizedException {
        boolean canEdit = false;
        try {
            canEdit = permissionsService.canEdit(email);
        } catch (IOException ex) {
            throw new UnauthorizedException("You are not authorised to edit collections.");
        }

        if (!canEdit) {
            warn().user(email)
                    .log("request unsuccessful user denied edit permission");

            throw new UnauthorizedException("You are not authorised to edit collections.");
        }
    }

    /**
     * ScheduleCanceller is an abstraction wrapper for functionality to cancel a scheduled publish.
     */
    public static interface ScheduleCanceller {

        /**
         * Cancel a scheduled publish for the provided collection.
         *
         * @param collection the collection to cancel.
         */
        void cancel(com.github.onsdigital.zebedee.model.Collection collection);
    }
}
