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
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.datasetIDNotProvidedException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.internalServerErrorException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.serviceTokenNotProvidedException;
import static com.github.onsdigital.zebedee.util.JsonUtils.writeResponseEntity;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Api
public class ServiceDatasetPermissions extends PermissionsAPIBase {

    public ServiceDatasetPermissions() {
        super(cmsFeatureFlags().isPermissionsAuthEnabled(),
                CMDPermissionsServiceImpl.getInstance(),
                (r, b, s) -> writeResponseEntity(r, b, s));
    }

    /**
     * Construct a new Permissions endpoint.
     *
     * @param enabled               true enables the endpoint, false all request valid or invaild will return 404.
     * @param cmdPermissionsService
     * @param responseWriter        the http reponse writer impl to use.
     */
    public ServiceDatasetPermissions(boolean enabled, CMDPermissionsService cmdPermissionsService, HttpResponseWriter responseWriter) {
        super(enabled, cmdPermissionsService, responseWriter);
    }

    @Override
    public CRUD getPermissions(HttpServletRequest request, HttpServletResponse response) throws PermissionsException {
        GetPermissionsRequest getPermissionsRequest = new GetPermissionsRequest(request);

        validateGetPermissionsRequest(getPermissionsRequest);

        info().datasetID(getPermissionsRequest.getDatasetID())
                .log("handling get dataset permissions request for service account");

        return permissionsService.getServiceDatasetPermissions(getPermissionsRequest);
    }

    void validateGetPermissionsRequest(GetPermissionsRequest request) throws PermissionsException {
        if (request == null) {
            throw internalServerErrorException();
        }

        if (isEmpty(request.getServiceToken())) {
            throw serviceTokenNotProvidedException();
        }

        if (isEmpty(request.getDatasetID())) {
            throw datasetIDNotProvidedException();
        }
    }
}
