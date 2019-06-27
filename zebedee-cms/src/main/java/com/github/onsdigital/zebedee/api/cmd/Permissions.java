package com.github.onsdigital.zebedee.api.cmd;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.permissions.cmd.CRUD;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsException;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsService;
import com.github.onsdigital.zebedee.permissions.cmd.PermissionsServiceImpl;
import com.github.onsdigital.zebedee.util.HttpResponseWriter;
import com.github.onsdigital.zebedee.util.JsonUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

@Api
public class Permissions {

    static final String DATASET_ID_PARAM = "dataset_id";
    static final String COLLECTION_ID_PARAM = "collection_id";
    static final String SERVICE_AUTH_HEADER = "Authorization";
    static final String FLORENCE_AUTH_HEATHER = "X-Florence-Token";
    static final String DATASET_ID_MISSING = "dataset_id param required but not found";
    static final String COLLECTION_ID_MISSING = "collection_id param required but not found";
    static final String BREARER_PREFIX = "Bearer ";

    private static PermissionsService permissionsService;

    private boolean enabled = false;
    private HttpResponseWriter httpResponseWriter;

    /**
     * Contruct the permissions endpoint with the default values.
     */
    public Permissions() {
        this(cmsFeatureFlags().isEnableDatasetImport(), getPermissionsService(),
                (req, body, status) -> JsonUtils.writeResponse(req, body, status));
    }

    /**
     * Construct a new Permissions endpoint.
     *
     * @param enabled              true enables the endpoint, false all request valid or invaild will return 404.
     * @param authorisationService the authorisation service to use.
     * @param responseWriter       the http reponse writer impl to use.
     */
    public Permissions(boolean enabled,
                       PermissionsService permissionsService,
                       HttpResponseWriter responseWriter) {
        this.enabled = enabled;
        this.permissionsService = permissionsService;
        this.httpResponseWriter = responseWriter;
    }

    @GET
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!enabled) {
            info().data("feature_flag", CMSFeatureFlags.ENABLE_DATASET_IMPORT)
                    .log("permissions api endpoint reqiures CMD feature to be enabled request will not be processed");
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
            error().exception(ex).log("error getting dataset permissions");
            writeResponse(response, null, SC_INTERNAL_SERVER_ERROR);
        }
    }

    CRUD getPermissions(HttpServletRequest request, HttpServletResponse response)
            throws PermissionsException {

        String sessionID = request.getHeader(FLORENCE_AUTH_HEATHER);
        String serviceToken = request.getHeader(SERVICE_AUTH_HEADER);

        if (isEmpty(sessionID) && isEmpty(serviceToken)) {
            info().log("invalid permissions request expected user or service auth token but none found");
            throw new PermissionsException("invalid request", SC_BAD_REQUEST);
        }

        if (isNotEmpty(sessionID)) {
            info().log("handling get permissions request for user");
            String datasetID = request.getParameter(DATASET_ID_PARAM);
            String collectionID = request.getParameter(COLLECTION_ID_PARAM);
            return permissionsService.getUserDatasetPermissions(sessionID, datasetID, collectionID);
        }

        info().log("handling get permissions request for service account");
        return permissionsService.getServiceDatasetPermissions(parseServiceToken(serviceToken));
    }

    void writeResponse(HttpServletResponse response, Object entity, int status) throws IOException {
        try {
            httpResponseWriter.writeJSONResponse(response, entity, status);
        } catch (Exception ex) {
            error().exception(ex).log("error writing user body to response");
            httpResponseWriter.writeJSONResponse(response, null, SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String parseServiceToken(String serviceToken) {
        if (serviceToken.startsWith(BREARER_PREFIX)) {
            serviceToken = serviceToken.replaceFirst(BREARER_PREFIX, "");
        }
        return serviceToken;
    }

    private static PermissionsService getPermissionsService() {
        if (permissionsService == null) {
            permissionsService = PermissionsServiceImpl.getInstance();
        }
        return permissionsService;
    }
}
