package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.grantUserCreateReadUpdateDelete;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.grantUserNone;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.grantUserRead;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.permitServiceAccountCreateReadUpdateDelete;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.collectionIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.collectionNotFoundException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.datasetIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.internalServerErrorException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.serviceAccountNotFoundException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.serviceTokenNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.sessionExpiredException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.sessionIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.sessionNotFoundException;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;

public class CMDPermissionsServiceImpl implements CMDPermissionsService {

    private static final String DATASET_NOT_IN_COLLECTION = "no permissions granted to viewer user as the requested " +
            "collection does not contain the requested dataset";

    private static final String NO_COLLECTION_VIEW_PERMISSION = "no permissions granted to viewer user as they do not" +
            " have view permissions for the requested collection";

    public static CMDPermissionsService instance = null;

    private SessionsService sessionsService;
    private Collections collectionsService;
    private ServiceStore serviceStore;
    private PermissionsService collectionPermissions;

    CMDPermissionsServiceImpl(SessionsService sessionsService, Collections collectionsService,
                              ServiceStore serviceStore, PermissionsService collectionPermissions) {
        this.sessionsService = sessionsService;
        this.collectionsService = collectionsService;
        this.serviceStore = serviceStore;
        this.collectionPermissions = collectionPermissions;
    }

    @Override
    public CRUD getUserDatasetPermissions(GetPermissionsRequest request)
            throws PermissionsException {
        Session userSession = getSessionByID(request.getSessionID());

        if (userHasEditCollectionPermission(userSession)) {
            return grantUserCreateReadUpdateDelete(request, userSession);
        }

        Collection targetCollection = getCollectionByID(request.getCollectionID());

        if (!userHasViewCollectionPermission(userSession, targetCollection.getDescription())) {
            return grantUserNone(request, userSession, NO_COLLECTION_VIEW_PERMISSION);
        }

        if (!collectionContainsDataset(targetCollection, request.getDatasetID())) {
            return grantUserNone(request, userSession, DATASET_NOT_IN_COLLECTION);
        }

        return grantUserRead(request, userSession);
    }

    @Override
    public CRUD getServiceDatasetPermissions(GetPermissionsRequest request) throws PermissionsException {
        if (isEmpty(request.getDatasetID())) {
            throw datasetIDNotProvidedException();
        }

        if (isEmpty(request.getServiceToken())) {
            throw serviceTokenNotProvidedException();
        }

        ServiceAccount serviceAccount = getServiceAccountByID(request.getServiceToken());
        return permitServiceAccountCreateReadUpdateDelete(request, serviceAccount);
    }


    Session getSessionByID(String sessionID) throws PermissionsException {
        if (isEmpty(sessionID)) {
            throw sessionIDNotProvidedException();
        }

        Session session = null;
        try {
            session = sessionsService.get(sessionID);
        } catch (IOException ex) {
            error().exception(ex)
                    .sessionID(sessionID)
                    .log("user dataset permissions request failed error getting session");
            throw internalServerErrorException();
        }

        if (session == null) {
            info().sessionID(sessionID).log("user dataset permissions request denied session not found");
            throw sessionNotFoundException();
        }

        if (sessionsService.expired(session)) {
            info().sessionID(sessionID).log("user dataset permissions request denied session expired");
            throw sessionExpiredException();
        }
        return session;
    }

    Collection getCollectionByID(String id) throws PermissionsException {
        if (isEmpty(id)) {
            throw collectionIDNotProvidedException();
        }

        Collection collection = null;
        try {
            collection = collectionsService.getCollection(id);
        } catch (IOException ex) {
            error().exception(ex)
                    .collectionID(id)
                    .log("user dataset permissions request denied error getting collection");
            throw internalServerErrorException();
        }

        if (collection == null) {
            info().collectionID(id)
                    .log("user dataset permissions request denied collection not found");
            throw collectionNotFoundException();
        }

        return collection;
    }

    ServiceAccount getServiceAccountByID(String serviceToken) throws PermissionsException {
        if (isEmpty(serviceToken)) {
            throw serviceTokenNotProvidedException();
        }

        ServiceAccount account = null;

        try {
            account = serviceStore.get(serviceToken);
        } catch (IOException ex) {
            error().exception(ex)
                    .serviceAccountToken(serviceToken)
                    .log("service dataset permissons request failed error getting service account");
            throw internalServerErrorException();
        }

        if (account == null) {
            error().serviceAccountToken(serviceToken)
                    .log("service dataset permissons request denied service account not found");
            throw serviceAccountNotFoundException();
        }
        return account;
    }

    boolean collectionContainsDataset(Collection collection, String datasetID)
            throws PermissionsException {
        return collection.getDescription()
                .getDatasets()
                .stream()
                .filter(dataset -> StringUtils.equals(datasetID, dataset.getId()))
                .findFirst()
                .isPresent();
    }

    boolean userHasEditCollectionPermission(Session session) throws PermissionsException {
        try {
            return collectionPermissions.canEdit(session);
        } catch (IOException ex) {
            error().exception(ex)
                    .user(session)
                    .data("permission", "can_edit")
                    .log("user dataset permissions request denied error checking user permissions");
            throw internalServerErrorException();
        }
    }

    boolean userHasViewCollectionPermission(Session session, CollectionDescription description)
            throws PermissionsException {
        try {
            return collectionPermissions.canView(session, description);
        } catch (IOException ex) {
            error().exception(ex)
                    .user(session)
                    .data("permission", "can_view")
                    .log("user dataset permissions request denied error checking user permissions");
            throw internalServerErrorException();
        }
    }

    public static CMDPermissionsService getInstance() {
        if (instance == null) {
            synchronized (CMDPermissionsServiceImpl.class) {
                if (instance == null) {
                    SessionsService sessionsService = Root.zebedee.getSessionsService();
                    ServiceStore serviceStore = Root.zebedee.getServiceStore();
                    Collections collections = Root.zebedee.getCollections();
                    PermissionsService permissionsService = Root.zebedee.getPermissionsService();

                    instance = new CMDPermissionsServiceImpl(sessionsService, collections, serviceStore,
                            permissionsService);
                }
            }
        }
        return instance;
    }
}