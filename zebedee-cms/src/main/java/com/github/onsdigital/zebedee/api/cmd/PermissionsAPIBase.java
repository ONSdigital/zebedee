package com.github.onsdigital.zebedee.api.cmd;

import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.permissions.cmd.CMDPermissionsService;
import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.permissions.cmd.GetPermissionsRequest;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsException;
import com.github.onsdigital.zebedee.util.HttpResponseWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * @deprecated the permissions APIs are deprecated in favour of the new dp-permissions-api. Once the migration of all
 *             dataset services to the new dp-authorisation v2 library has been completed these endpoints should be
 *             removed.
 */
@Deprecated
public abstract class PermissionsAPIBase {

    public static final String SERVICE_AUTH_HEADER = "Authorization";

    protected CMDPermissionsService permissionsService;
    protected HttpResponseWriter httpResponseWriter;

    /**
     * Construct a new Permissions endpoint.
     *
     * @param cmdPermissionsService the permission service to use.
     * @param responseWriter        the http response writer impl to use.
     */
    public PermissionsAPIBase(CMDPermissionsService cmdPermissionsService,
                              HttpResponseWriter responseWriter) {
        this.permissionsService = cmdPermissionsService;
        this.httpResponseWriter = responseWriter;
    }

    @GET
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            GetPermissionsRequest getPermissionsRequest = new GetPermissionsRequest(request);
            CRUD permissions = getPermissions(getPermissionsRequest);
            writeResponse(response, permissions, SC_OK);
        } catch (PermissionsException ex) {
            error().exception(ex).log("error getting dataset permissions");
            writeResponse(response, new Error(ex.getMessage()), ex.statusCode);
        } catch (IOException ex) {
            error().exception(ex).log("internal sever error failed to write response body");
            response.setStatus(SC_INTERNAL_SERVER_ERROR);
        }
    }

    public abstract CRUD getPermissions(GetPermissionsRequest request)
            throws PermissionsException;

    void writeResponse(HttpServletResponse response, Object entity, int status) throws IOException {
        try {
            httpResponseWriter.writeJSONResponse(response, entity, status);
        } catch (Exception ex) {
            error().exception(ex).log("error writing user body to response");
            httpResponseWriter.writeJSONResponse(response, null, SC_INTERNAL_SERVER_ERROR);
        }
    }
}
