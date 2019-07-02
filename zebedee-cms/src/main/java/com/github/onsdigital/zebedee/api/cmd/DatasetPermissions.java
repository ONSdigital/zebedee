package com.github.onsdigital.zebedee.api.cmd;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsException;
import com.github.onsdigital.zebedee.util.HttpResponseWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static com.github.onsdigital.zebedee.util.JsonUtils.writeResponseEntity;

/**
 * API endpoint for getting CMD dataset permissions for a caller. The code for handling CMD permissions requests
 * is pretty much all contained in the base class.<br/>
 * This subclass exists only to provide the base constructor with the {@link DatasetPermissionsRequestHandler}
 * implemetation of {@link PermissionsRequestHandler} and to register a separate API route - Restolino uses the class
 * name of classes annotated with {@link Api} to register API routes. Basically we need to create a new
 * class for each API endpoint if their implementation is inherited from a base class.
 */
@Api
public class DatasetPermissions extends PermissionsAPIBase {

    private PermissionsRequestHandler permissionsRequestHandler;

    /**
     * Contruct the permissions endpoint with the default values.
     */
    public DatasetPermissions() {
        this(cmsFeatureFlags().isEnableDatasetImport(),
                new DatasetPermissionsRequestHandler(),
                (r, b, s) -> writeResponseEntity(r, b, s));
    }

    /**
     * Construct a new Permissions endpoint.
     *
     * @param enabled              true enables the endpoint, false all request valid or invaild will return 404.
     * @param authorisationService the authorisation service to use.
     * @param responseWriter       the http reponse writer impl to use.
     */
    public DatasetPermissions(boolean enabled, PermissionsRequestHandler permissionsRequestHandler, HttpResponseWriter responseWriter) {
        super(enabled, responseWriter);
        this.permissionsRequestHandler = permissionsRequestHandler;
    }

    @Override
    public CRUD getPermissions(HttpServletRequest request, HttpServletResponse response) throws PermissionsException {
        return permissionsRequestHandler.get(request, response);
    }
}
