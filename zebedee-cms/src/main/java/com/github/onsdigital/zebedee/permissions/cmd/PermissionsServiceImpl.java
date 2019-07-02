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

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.permitServiceAccountCreateReadUpdateDelete;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.permitUserCreateReadUpdateDelete;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.permitUserNone;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.permitUserRead;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.collectionIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.collectionNotFoundException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.datasetIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.internalServerErrorException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.serviceAccountNotFoundException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.sessionExpiredException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.sessionIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.sessionNotFoundException;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;

public class PermissionsServiceImpl implements PermissionsService {

    private static final String DATASET_ID_NOT_PROVIDED = "no permissions granted to user dataset ID required but not" +
            " provided";

    private static final String DATASET_NOT_IN_COLLECTION = "no permissions granted to viewer user as the requested " +
            "collection does not contain the requested dataset";

    private static final String NO_COLLECTION_VIEW_PERMISSION = "no permissions granted to viewer user as they do not" +
            " have view permissions for the requested collection";

    public static PermissionsService instance = null;

    private SessionsService sessionsService;
    private Collections collectionsService;
    private ServiceStore serviceStore;
    private CollectionPermissionsService collectionPermissions;

    PermissionsServiceImpl(SessionsService sessionsService, Collections collectionsService,
                           ServiceStore serviceStore, CollectionPermissionsService collectionPermissions) {
        this.sessionsService = sessionsService;
        this.collectionsService = collectionsService;
        this.serviceStore = serviceStore;
        this.collectionPermissions = collectionPermissions;
    }

    @Override
    public CRUD getUserDatasetPermissions(String sessionID, String datasetID, String collectionID)
            throws PermissionsException {
        Session session = getSession(sessionID);

        if (isEmpty(datasetID)) {
            info().datasetID(datasetID).collectionID(collectionID).email(session).log(DATASET_ID_NOT_PROVIDED);
            throw datasetIDNotProvidedException();
        }

        if (collectionPermissions.hasEdit(session)) {
            return permitUserCreateReadUpdateDelete(collectionID, datasetID, session);
        }

        Collection collection = getCollection(collectionID);

        if (!collectionPermissions.hasView(session, collection.getDescription())) {
            return permitUserNone(collectionID, datasetID, session, NO_COLLECTION_VIEW_PERMISSION);
        }

        if (!isDatasetPartOfCollection(collection, datasetID)) {
            return permitUserNone(collectionID, datasetID, session, DATASET_NOT_IN_COLLECTION);
        }

        return permitUserRead(collectionID, datasetID, session);
    }

    @Override
    public CRUD getServiceDatasetPermissions(String serviceToken, String datasetID) throws PermissionsException {
        if (StringUtils.isEmpty(datasetID)) {
            throw datasetIDNotProvidedException();
        }

        ServiceAccount serviceAccount = getServiceAccount(serviceToken);
        return permitServiceAccountCreateReadUpdateDelete(serviceAccount, datasetID);
    }


    Session getSession(String sessionID) throws PermissionsException {
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

    Collection getCollection(String id) throws PermissionsException {
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

    ServiceAccount getServiceAccount(String serviceToken) throws PermissionsException {
        if (isEmpty(serviceToken)) {
            throw new PermissionsException("service permissions request denied no service token provided", SC_BAD_REQUEST);
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

    boolean isDatasetPartOfCollection(Collection collection, String datasetID)
            throws PermissionsException {
        return collection.getDescription()
                .getDatasets()
                .stream()
                .filter(dataset -> StringUtils.equals(datasetID, dataset.getId()))
                .findFirst()
                .isPresent();
    }


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
}
