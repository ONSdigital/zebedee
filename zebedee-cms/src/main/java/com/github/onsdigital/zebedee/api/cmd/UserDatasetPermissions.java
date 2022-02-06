package com.github.onsdigital.zebedee.api.cmd;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.permissions.cmd.CMDPermissionsService;
import com.github.onsdigital.zebedee.permissions.cmd.CMDPermissionsServiceImpl;
import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.permissions.cmd.GetPermissionsRequest;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsException;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.util.HttpResponseWriter;

import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.collectionIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.datasetIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.internalServerErrorException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.sessionNotProvidedException;
import static com.github.onsdigital.zebedee.util.JsonUtils.writeResponseEntity;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * @deprecated the permissions APIs are deprecated in favour of the new dp-permissions-api. Once the migration of all
 *             dataset services to the new dp-authorisation v2 library has been completed these endpoints should be
 *             removed.
 */
@Deprecated
@Api
public class UserDatasetPermissions extends PermissionsAPIBase {

    public UserDatasetPermissions() {
        super(CMDPermissionsServiceImpl.getInstance(),
                (r, b, s) -> writeResponseEntity(r, b, s));
    }

    /**
     * Construct a new ServiceDatasetPermissions endpoint.
     *
     * @param cmdPermissionsService
     * @param responseWriter        the http response writer impl to use.
     * @param sessionsService       the sessions service
     */
    public UserDatasetPermissions(CMDPermissionsService cmdPermissionsService, HttpResponseWriter responseWriter,
                                     Sessions sessionsService) {
        super(cmdPermissionsService, responseWriter, sessionsService);
    }

    @Override
    public CRUD getPermissions(GetPermissionsRequest request) throws PermissionsException {
        validateGetPermissionsRequest(request);

        return permissionsService.getUserDatasetPermissions(request);
    }

    void validateGetPermissionsRequest(GetPermissionsRequest request) throws PermissionsException {
        if (request == null) {
            throw internalServerErrorException();
        }

        if (request.getSession() == null) {
            throw sessionNotProvidedException();
        }

        if (isEmpty(request.getDatasetID())) {
            throw datasetIDNotProvidedException();
        }

        if (isEmpty(request.getCollectionID())) {
            throw collectionIDNotProvidedException();
        }
    }
}
