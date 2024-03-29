package com.github.onsdigital.zebedee.permissions.cmd;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.grantServiceAccountDatasetCreateReadUpdateDelete;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.grantServiceAccountInstanceCreateReadUpdateDelete;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.grantUserDatasetCreateReadUpdateDelete;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.grantUserDatasetRead;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.grantUserInstanceCreateReadUpdateDelete;
import static com.github.onsdigital.zebedee.permissions.cmd.CRUD.grantUserNone;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.collectionIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.collectionNotFoundException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.datasetIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.internalServerErrorException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.serviceAccountNotFoundException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.serviceTokenNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.sessionNotFoundException;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * @deprecated in favour of the dp-permissions-api. Once all dataset related APIs have been updated to use the
 *             dp-authorisation v2 library and JWT sessions are in use, this service will be removed.
 */
@Deprecated
public class CMDPermissionsServiceImpl implements CMDPermissionsService {

    private static final String DATASET_NOT_IN_COLLECTION = "no permissions granted to viewer user as the requested " +
            "collection does not contain the requested dataset";

    private static final String NO_COLLECTION_VIEW_PERMISSION = "no permissions granted to viewer user as they do not" +
            " have view permissions for the requested collection";

    private static final String INSTANCE_PERMISSIONS_DENIED = "no instance permissions granted to user as they do not" +
            " have admin or publisher collection permissions";

    public static CMDPermissionsService instance = null;

    private Collections collectionsService;
    private ServiceStore serviceStore;
    private PermissionsService collectionPermissions;

    CMDPermissionsServiceImpl(Collections collectionsService, ServiceStore serviceStore,
                              PermissionsService collectionPermissions) {
        this.collectionsService = collectionsService;
        this.serviceStore = serviceStore;
        this.collectionPermissions = collectionPermissions;
    }

    @Override
    public CRUD getUserDatasetPermissions(GetPermissionsRequest request)
            throws PermissionsException {
        if (request.getSession() == null) {
            info().log("user dataset permissions request denied session not found");
            throw sessionNotFoundException();
        }

        if (userHasEditCollectionPermission(request.getSession())) {
            return grantUserDatasetCreateReadUpdateDelete(request);
        }

        String collectionId = request.getCollectionID();

        if (!userHasViewCollectionPermission(request.getSession(), collectionId)) {
            return grantUserNone(request, NO_COLLECTION_VIEW_PERMISSION);
        }

        Collection targetCollection = getCollectionByID(collectionId);
        if (!collectionContainsDataset(targetCollection, request.getDatasetID())) {
            return grantUserNone(request, DATASET_NOT_IN_COLLECTION);
        }

        return grantUserDatasetRead(request);
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
        return grantServiceAccountDatasetCreateReadUpdateDelete(request, serviceAccount);
    }

    @Override
    public CRUD getUserInstancePermissions(GetPermissionsRequest request) throws PermissionsException {
        if (request == null) {
            throw internalServerErrorException();
        }

        if (request.getSession() == null) {
            info().log("user instance permissions request denied session not found");
            throw sessionNotFoundException();
        }

        if (userHasPublisherPermissions(request.getSession())) {
            return grantUserInstanceCreateReadUpdateDelete(request);
        }
        return grantUserNone(request, INSTANCE_PERMISSIONS_DENIED);
    }

    @Override
    public CRUD getServiceInstancePermissions(GetPermissionsRequest request) throws PermissionsException {
        if (request == null) {
            throw internalServerErrorException();
        }

        if (isEmpty(request.getServiceToken())) {
            throw serviceTokenNotProvidedException();
        }
        ServiceAccount serviceAccount = getServiceAccountByID(request.getServiceToken());
        return grantServiceAccountInstanceCreateReadUpdateDelete(request, serviceAccount);
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
            error().exception(ex).log("service dataset permissons request failed error getting service account");
            throw internalServerErrorException();
        }

        if (account == null) {
            error().log("service dataset permissons request denied service account not found");
            throw serviceAccountNotFoundException();
        }
        return account;
    }

    boolean collectionContainsDataset(Collection collection, String datasetID) {
        return collection.getDescription()
                .getDatasets()
                .stream()
                .anyMatch(dataset -> StringUtils.equals(datasetID, dataset.getId()));
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

    boolean userHasPublisherPermissions(Session session) throws PermissionsException {
        return userHasEditCollectionPermission(session);
    }

    boolean userHasViewCollectionPermission(Session session, String collectionId)
            throws PermissionsException {
        try {
            return collectionPermissions.canView(session, collectionId);
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
                    Sessions sessions = Root.zebedee.getSessions();
                    ServiceStore serviceStore = Root.zebedee.getServiceStore();
                    Collections collections = Root.zebedee.getCollections();
                    PermissionsService permissionsService = Root.zebedee.getPermissionsService();

                    instance = new CMDPermissionsServiceImpl(collections, serviceStore, permissionsService);
                }
            }
        }
        return instance;
    }
}
