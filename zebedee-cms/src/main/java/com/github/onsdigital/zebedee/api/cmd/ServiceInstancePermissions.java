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
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.serviceTokenNotProvidedException;
import static com.github.onsdigital.zebedee.util.JsonUtils.writeResponseEntity;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * API endpoint for getting instance permissions for a service account.
 */
@Api
public class ServiceInstancePermissions extends PermissionsAPIBase {

    /**
     * Create a new ServiceInstancePermissions endpoint using the default costructor parameters.
     */
    public ServiceInstancePermissions() {
        super(cmsFeatureFlags().isPermissionsAuthEnabled(), CMDPermissionsServiceImpl.getInstance(), (r, b, s) -> writeResponseEntity(r, b, s));
    }

    /**
     * Construct a new ServiceInstancePermissions endpoint.
     *
     * @param enabled               true enables the endpoint, false all request valid or invaild will return 404.
     * @param cmdPermissionsService
     * @param responseWriter        the http reponse writer impl to use.
     */
    public ServiceInstancePermissions(boolean enabled, CMDPermissionsService cmdPermissionsService, HttpResponseWriter responseWriter) {
        super(enabled, cmdPermissionsService, responseWriter);
    }

    @Override
    public CRUD getPermissions(HttpServletRequest request, HttpServletResponse response) throws PermissionsException {
        info().log("handling get service instance permissions request");
        validateRequest(request);
        GetPermissionsRequest getPermissionsRequest = new GetPermissionsRequest(request);
        return permissionsService.getServiceInstancePermissions(getPermissionsRequest);
    }

    void validateRequest(HttpServletRequest request) throws PermissionsException {
        if (request == null) {
            throw PermissionsException.internalServerErrorException();
        }

        String serviceToken = request.getHeader(SERVICE_AUTH_HEADER);
        if (isEmpty(serviceToken)) {
            throw serviceTokenNotProvidedException();
        }

        info().log("handling valid service instance permissions request");
    }
}
