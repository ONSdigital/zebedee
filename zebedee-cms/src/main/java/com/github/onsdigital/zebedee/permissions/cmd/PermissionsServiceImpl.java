package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Optional;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static com.github.onsdigital.zebedee.permissions.cmd.Permissions.permitCreateReadUpdateDelete;
import static com.github.onsdigital.zebedee.permissions.cmd.Permissions.permitNone;
import static com.github.onsdigital.zebedee.permissions.cmd.Permissions.permitRead;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

public class PermissionsServiceImpl implements PermissionsService {

    public static PermissionsService instance = null;

    private SessionsService sessionsService;
    private Collections collectionsService;
    private ServiceStore serviceStore;
    private CollectionPermissionsService collectionPermissions;

    public static PermissionsService getInstance() {
        if (instance == null) {
            synchronized (PermissionsServiceImpl.class) {
                if (instance == null) {
                    SessionsService sessionsService = Root.zebedee.getSessionsService();
                    ServiceStore serviceStore = Root.zebedee.getServiceStore();
                    Collections collections = Root.zebedee.getCollections();
                    CollectionPermissionsService collectionPermissionsService =
                            new CollectionPermissionsServiceImpl(Root.zebedee.getPermissionsService());

                    instance = new PermissionsServiceImpl(
                            sessionsService, collections, serviceStore, collectionPermissionsService);
                }
            }
        }
        return instance;
    }

    PermissionsServiceImpl(SessionsService sessionsService, Collections collectionsService,
                                  ServiceStore serviceStore, CollectionPermissionsService collectionPermissions) {
        this.sessionsService = sessionsService;
        this.collectionsService = collectionsService;
        this.serviceStore = serviceStore;
        this.collectionPermissions = collectionPermissions;
    }

    @Override
    public Permissions getUserDatasetPermissions(String sessionID, String datasetID, String collectionID)
            throws PermissionsException {

        // If the session is valid then the user is with an Admin, Editor or Viewer.
        Session session = getSession(Optional.ofNullable(sessionID));

        // If user has collection edit permission then grant full CRUD permissions. We aren't bothered if the
        // collection and dataset combination is valid for this user type.
        if (collectionPermissions.hasEdit(session)) {
            Permissions crud = permitCreateReadUpdateDelete();
            info().collectionID(collectionID)
                    .datasetID(datasetID)
                    .email(session)
                    .datasetPermissions(crud)
                    .log("granting permissions to admin/editor");
            return crud;
        }

        // Otherwise the user is a viewer - check the collection exists
        Collection collection = getCollection(collectionID);

        if (StringUtils.isEmpty(datasetID)) {
            info().log("user dataset permissions request denied dataset ID required but was empty");
            throw new PermissionsException("dataset ID required but was empty", SC_BAD_REQUEST);
        }

        // check the viewer can view this collection.

        if (!collectionPermissions.hasView(session, collection.getDescription())) {
            info().collectionID(collectionID)
                    .datasetID(datasetID)
                    .email(session)
                    .log("no permissions granted to viewer user as they not have view permissions for the requested collection");
            return permitNone();
        }

        // check the requested dataset is part of the collection.
        if (!isDatasetInCollection(collection, datasetID)) {
            info().collectionID(collectionID)
                    .datasetID(datasetID)
                    .email(session)
                    .log("no permissions granted to viewer user as the requested collection does not contain the requested dataset");
            return permitNone();
        }

        // grant READ only permission
        return permitRead();
    }

    @Override
    public Permissions getServiceDatasetPermissions(String serviceToken) throws PermissionsException {
        ServiceAccount serviceAccount = getServiceAccount(serviceToken);
        Permissions servicePermissions = permitCreateReadUpdateDelete();
        info().serviceAccountID(serviceAccount.getId())
                .data("permissions", servicePermissions)
                .log("granting dataset permissions to valid service account");
        return servicePermissions;
    }


    Session getSession(Optional<String> sess) throws PermissionsException {
        if (!sess.isPresent()) {
            throw new PermissionsException("user dataset permissions request denied session ID required but empty", SC_BAD_REQUEST);
        }

        Session session = null;
        try {
            session = sessionsService.get(sess.get());
        } catch (IOException ex) {
            error().exception(ex).sessionID(sess.get())
                    .log("user dataset permissions request failed error getting session");
            throw new PermissionsException("internal server error", SC_INTERNAL_SERVER_ERROR);
        }

        if (session == null) {
            info().sessionID(sess.get()).log("user dataset permissions request denied session not found");
            throw new PermissionsException("session not found", SC_UNAUTHORIZED);
        }

        if (sessionsService.expired(session)) {
            info().sessionID(sess.get()).log("user dataset permissions request denied session expired");
            throw new PermissionsException("session expired", SC_UNAUTHORIZED);
        }

        return session;
    }

    Collection getCollection(String id) throws PermissionsException {
        if (StringUtils.isEmpty(id)) {
            info().log("user dataset permissions request denied collection ID required but was empty");
            throw new PermissionsException("collection ID required but was empty", SC_BAD_REQUEST);
        }

        Collection collection = null;
        try {
            collection = collectionsService.getCollection(id);
        } catch (IOException ex) {
            error().exception(ex)
                    .collectionID(id)
                    .log("user dataset permissions request denied error getting collection");
            throw new PermissionsException("internal server error", SC_INTERNAL_SERVER_ERROR);
        }

        if (collection == null) {
            info().collectionID(id)
                    .log("user dataset permissions request denied  collection not found");
            throw new PermissionsException("collection not found", SC_NOT_FOUND);
        }

        return collection;
    }

    ServiceAccount getServiceAccount(String serviceToken) throws PermissionsException {
        if (StringUtils.isEmpty(serviceToken)) {
            throw new PermissionsException("service permissions request denied no service token provided", SC_BAD_REQUEST);
        }

        ServiceAccount account = null;

        try {
            account = serviceStore.get(serviceToken);
        } catch (IOException ex) {
            error().exception(ex)
                    .serviceAccountToken(serviceToken)
                    .log("service dataset permissons request failed error getting service account");
            throw new PermissionsException("internal server error", SC_INTERNAL_SERVER_ERROR);
        }

        if (account == null) {
            error().serviceAccountToken(serviceToken)
                    .log("service dataset permissons request denied service account not found");
            throw new PermissionsException("permisson denied service account not found", SC_UNAUTHORIZED);
        }
        return account;
    }

    boolean isDatasetInCollection(Collection collection, String datasetID)
            throws PermissionsException {
        return collection.getDescription()
                .getDatasets()
                .stream()
                .filter(dataset -> StringUtils.equals(datasetID, dataset.getId()))
                .findFirst()
                .isPresent();
    }
}
