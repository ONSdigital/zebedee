package com.github.onsdigital.zebedee.api.cmd;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.permissions.cmd.CMDPermissionsService;
import com.github.onsdigital.zebedee.permissions.cmd.CMDPermissionsServiceImpl;
import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.permissions.cmd.GetPermissionsRequest;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsException;
import com.github.onsdigital.zebedee.util.HttpResponseWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.collectionIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.datasetIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.internalServerErrorException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.sessionIDNotProvidedException;
import static com.github.onsdigital.zebedee.util.JsonUtils.writeResponseEntity;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Api
public class UserDatasetPermissions extends PermissionsAPIBase {

    public UserDatasetPermissions() {
        this(cmsFeatureFlags().isPermissionsAuthEnabled(),
                CMDPermissionsServiceImpl.getInstance(),
                (r, b, s) -> writeResponseEntity(r, b, s));
    }

    public UserDatasetPermissions(boolean enabled, CMDPermissionsService cmdPermissionsService,
                                  HttpResponseWriter responseWriter) {
        super(enabled, cmdPermissionsService, responseWriter);
    }

    @Override
    public CRUD getPermissions(HttpServletRequest request, HttpServletResponse response) throws PermissionsException {
        GetPermissionsRequest getPermissionsRequest = new GetPermissionsRequest(request);

        validateGetPermissionsRequest(getPermissionsRequest);

        info().datasetID(getPermissionsRequest.getDatasetID())
                .collectionID(getPermissionsRequest.getCollectionID())
                .log("handling get dataset permissions request for user");

        return permissionsService.getUserDatasetPermissions(getPermissionsRequest);
    }

    void validateGetPermissionsRequest(GetPermissionsRequest request) throws PermissionsException {
        if (request == null) {
            throw internalServerErrorException();
        }

        if (isEmpty(request.getSessionID())) {
            throw sessionIDNotProvidedException();
        }

        if (isEmpty(request.getDatasetID())) {
            throw datasetIDNotProvidedException();
        }

        if (isEmpty(request.getCollectionID())) {
            throw collectionIDNotProvidedException();
        }
    }
}
