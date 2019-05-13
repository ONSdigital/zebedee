package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;
import static com.github.onsdigital.zebedee.authorisation.DatasetPermissionType.CREATE;
import static com.github.onsdigital.zebedee.authorisation.DatasetPermissionType.DELETE;
import static com.github.onsdigital.zebedee.authorisation.DatasetPermissionType.READ;
import static com.github.onsdigital.zebedee.authorisation.DatasetPermissionType.UPDATE;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

public class AuthorisationServiceImpl implements AuthorisationService {

    private ServiceSupplier<SessionsService> sessionServiceSupplier = () -> Root.zebedee.getSessionsService();
    private ServiceSupplier<UsersService> userServiceSupplier = () -> Root.zebedee.getUsersService();
    private ServiceSupplier<Collections> collectionsSupplier = () -> Root.zebedee.getCollections();
    private ServiceSupplier<PermissionsService> permissionsServiceSupplier = () -> Root.zebedee.getPermissionsService();

    private static final String INTERNAL_ERROR = "internal server error";
    private static final String AUTHENTICATED_ERROR = "user not authenticated";
    private static final String USER_NOT_FOUND = "user does not exist";
    private static final DatasetPermissions ADMIN_EDITOR_PERMISSIONS = new DatasetPermissions(CREATE, READ, UPDATE, DELETE);

    @Override
    public UserIdentity identifyUser(String sessionID) throws UserIdentityException {
        if (StringUtils.isEmpty(sessionID)) {
            warn().log("identify user error, no auth token was provided");
            throw new UserIdentityException(AUTHENTICATED_ERROR, SC_UNAUTHORIZED);
        }

        Session session;
        try {
            session = sessionServiceSupplier.getService().get(sessionID);
        } catch (IOException e) {
            error().data("sessionId", sessionID).logException(e, "identify user error, unexpected error while attempting to get user session");
            throw new UserIdentityException(INTERNAL_ERROR, SC_INTERNAL_SERVER_ERROR);
        }

        if (session == null) {
            warn().data("sessionId", sessionID).log("identify user error, session with specified ID could not be found");
            throw new UserIdentityException(AUTHENTICATED_ERROR, SC_UNAUTHORIZED);
        }

        // The session might exist but ensure the user still exists in the system before confirming their identity
        try {
            if (!userServiceSupplier.getService().exists(session.getEmail())) {
                warn().data("sessionId", session).log("identify user error, valid user session found but user no longer exists");
                throw new UserIdentityException(USER_NOT_FOUND, SC_NOT_FOUND);
            }
        } catch (IOException e) {
            error().data("sessionId", session).logException(e, "identify user error, unexpected error while checking if user exists");
            throw new UserIdentityException(INTERNAL_ERROR, SC_INTERNAL_SERVER_ERROR);
        }
        return new UserIdentity(session);
    }

    @Override
    public DatasetPermissions getUserPermissions(String sessionID, String datasetID, String collectionID)
            throws DatasetPermissionsException {
        Session session = getSession(sessionID);

        Collection collection = getCollection(collectionID);

        if (StringUtils.isEmpty(datasetID)) {
            throw new DatasetPermissionsException("dataset ID required but was empty", SC_BAD_REQUEST);
        }

        validateCollectionContainsRequestedDataset(collection, datasetID);

        DatasetPermissions permissions = new DatasetPermissions();

        if (isAdminOrPublisher(session)) {
            permissions.permit(CREATE, READ, UPDATE, DELETE);
        } else if (isCollectionViewer(session, collection.getDescription())) {
            permissions.permit(READ);
        }

        info().data("user", session.getEmail())
                .data("collection_id", collectionID)
                .data("dataset_id", datasetID)
                .data("permissions", permissions.getPermissions())
                .log("permitting dataset admit /editor permission");
        return permissions;
    }

    Session getSession(String sessionID) throws DatasetPermissionsException {
        if (StringUtils.isEmpty(sessionID)) {
            throw new DatasetPermissionsException("session ID required but empty", SC_BAD_REQUEST);
        }

        Session session = null;
        try {
            session = sessionServiceSupplier.getService().get(sessionID);
        } catch (IOException ex) {
            error().exception(ex).data("session_id", sessionID).log("error getting session");
            throw new DatasetPermissionsException("internal server error", SC_INTERNAL_SERVER_ERROR);
        }

        if (session == null) {
            throw new DatasetPermissionsException("session not found", SC_UNAUTHORIZED);
        }

        if (sessionServiceSupplier.getService().expired(session)) {
            throw new DatasetPermissionsException("session expired", SC_UNAUTHORIZED);
        }

        return session;
    }

    boolean isAdminOrPublisher(Session session) throws DatasetPermissionsException {
        try {
            return permissionsServiceSupplier.getService().canEdit(session);
        } catch (IOException ex) {
            error().exception(ex)
                    .data("email", session.getEmail())
                    .data("permission", "can_edit")
                    .log("error checking user permissions");
            throw new DatasetPermissionsException("internal server error", SC_INTERNAL_SERVER_ERROR);
        }
    }

    boolean isCollectionViewer(Session session, CollectionDescription description) throws DatasetPermissionsException {
        try {
            return permissionsServiceSupplier.getService().canView(session, description);
        } catch (IOException ex) {
            error().exception(ex)
                    .data("email", session.getEmail())
                    .data("permission", "can_edit")
                    .log("error checking user permissions");
            throw new DatasetPermissionsException("internal server error", SC_INTERNAL_SERVER_ERROR);
        }
    }

    Collection getCollection(String id) throws DatasetPermissionsException {
        if (StringUtils.isEmpty(id)) {
            throw new DatasetPermissionsException("collection ID required but was empty", SC_BAD_REQUEST);
        }
        Collection collection = null;
        try {
            collection = collectionsSupplier.getService().getCollection(id);
        } catch (IOException ex) {
            error().exception(ex).data("collection_id", id).log("error getting collection");
            throw new DatasetPermissionsException("internal server error", SC_INTERNAL_SERVER_ERROR);
        }

        if (collection == null) {
            throw new DatasetPermissionsException("collection not found", SC_NOT_FOUND);
        }

        return collection;
    }

    void validateCollectionContainsRequestedDataset(Collection collection, String datasetID)
            throws DatasetPermissionsException {
        boolean isPresent = collection.getDescription()
                .getDatasets()
                .stream()
                .filter(dataset -> StringUtils.equals(datasetID, dataset.getId()))
                .findFirst()
                .isPresent();

        if (!isPresent) {
            info().data("collection_id", collection.getDescription().getId())
                    .data("dataset_id", datasetID)
                    .log("requested dataset does not exist in the specified collection");
            throw new DatasetPermissionsException("requested collection does not contain the requested dataset", SC_BAD_REQUEST);
        }
    }

    @Override
    public DatasetPermissions getServicePermissions(String serviceToken) throws DatasetPermissionsException {
        return null;
    }
}
