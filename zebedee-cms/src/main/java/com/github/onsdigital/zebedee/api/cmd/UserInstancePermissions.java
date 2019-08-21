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
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.internalServerErrorException;
import static com.github.onsdigital.zebedee.permissions.cmd.PermissionsException.sessionIDNotProvidedException;
import static com.github.onsdigital.zebedee.util.JsonUtils.writeResponseEntity;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * API endpoint for getting a user's CMD instance permissions.
 */
@Api
public class UserInstancePermissions extends PermissionsAPIBase {

    /**
     * Create a new UserInstancePermissions endpoint using the default costructor parameters.
     */
    public UserInstancePermissions() {
        super(cmsFeatureFlags().isPermissionsAuthEnabled(), CMDPermissionsServiceImpl.getInstance(), (r, b, s) -> writeResponseEntity(r, b, s));
    }

    /**
     * Construct a new UserInstancePermissions endpoint.
     *
     * @param enabled               true enables the endpoint, false all request valid or invaild will return 404.
     * @param cmdPermissionsService
     * @param responseWriter        the http reponse writer impl to use.
     */
    public UserInstancePermissions(boolean enabled, CMDPermissionsService cmdPermissionsService, HttpResponseWriter responseWriter) {
        super(enabled, cmdPermissionsService, responseWriter);
    }

    @Override
    public CRUD getPermissions(HttpServletRequest request, HttpServletResponse response) throws PermissionsException {
        info().log("handling get user instance permissions request");
        validateRequest(request);
        GetPermissionsRequest getPermissionsRequest = new GetPermissionsRequest(request);
        return permissionsService.getUserInstancePermissions(getPermissionsRequest);
    }

    void validateRequest(HttpServletRequest request) throws PermissionsException {
        if (request == null) {
            throw internalServerErrorException();
        }

        String sessionID = request.getHeader(FLORENCE_AUTH_HEATHER);
        if (isEmpty(sessionID)) {
            throw sessionIDNotProvidedException();
        }
        info().log("handling valid get user instance permissions request");
    }
}
