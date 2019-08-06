package com.github.onsdigital.zebedee.api.cmd;

import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.permissions.cmd.CMDPermissionsService;
import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsException;
import com.github.onsdigital.zebedee.util.HttpResponseWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

public abstract class PermissionsAPIBase {

    public static final String DATASET_ID_PARAM = "dataset_id";
    public static final String COLLECTION_ID_PARAM = "collection_id";
    public static final String SERVICE_AUTH_HEADER = "Authorization";
    public static final String FLORENCE_AUTH_HEATHER = "X-Florence-Token";
    public static final String DATASET_ID_MISSING = "dataset_id param required but not found";
    public static final String COLLECTION_ID_MISSING = "collection_id param required but not found";
    public static final String BREARER_PREFIX = "Bearer ";

    protected CMDPermissionsService permissionsService;
    protected boolean enabled = false;
    protected HttpResponseWriter httpResponseWriter;

    /**
     * Construct a new Permissions endpoint.
     *
     * @param enabled              true enables the endpoint, false all request valid or invaild will return 404.
     * @param authorisationService the authorisation service to use.
     * @param responseWriter       the http reponse writer impl to use.
     */
    public PermissionsAPIBase(boolean enabled, CMDPermissionsService cmdPermissionsService,
                              HttpResponseWriter responseWriter) {
        this.enabled = enabled;
        this.permissionsService = cmdPermissionsService;
        this.httpResponseWriter = responseWriter;
    }

    @GET
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!enabled) {
            info().data("feature_flag", CMSFeatureFlags.ENABLE_PERMISSIONS_AUTH)
                    .data("requested_uri", request.getRequestURI())
                    .log("CMD permissions api endpoint reqiures CMD feature to be enabled request will not be " +
                            "processed");
            writeResponse(response, null, SC_NOT_FOUND);
            return;
        }

        try {
            CRUD permissions = getPermissions(request, response);
            writeResponse(response, permissions, SC_OK);
        } catch (PermissionsException ex) {
            error().exception(ex).log("error getting dataset permissions");
            writeResponse(response, new Error(ex.getMessage()), ex.statusCode);
        } catch (IOException ex) {
            error().exception(ex).log("internal sever error failed to write response body");
            response.setStatus(SC_INTERNAL_SERVER_ERROR);
        }
    }

    public abstract CRUD getPermissions(HttpServletRequest request, HttpServletResponse response)
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
