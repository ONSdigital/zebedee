package com.github.onsdigital.zebedee.api.cmd;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.authorisation.AuthorisationService;
import com.github.onsdigital.zebedee.authorisation.AuthorisationServiceImpl;
import com.github.onsdigital.zebedee.authorisation.DatasetPermissions;
import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.json.JSONable;
import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.util.HttpResponseWriter;
import com.github.onsdigital.zebedee.util.JsonUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

@Api
public class Permissions {

    static final String DATASET_ID_PARAM = "dataset_id";
    static final String COLLECTION_ID_PARAM = "collection_id";
    static final String SERVICE_AUTH_HEADER = "authroization";
    static final String FLORENCE_AUTH_HEATHER = "X-Florence-Token";


    static final JSONable DATASET_ID_MISSING = new Error("dataset_id param required but not found");
    static final JSONable COLLECTION_ID_MISSING = new Error("collection_id param required but not found");

    private boolean enabled = false;
    private HttpResponseWriter httpResponseWriter;
    private AuthorisationService authorisationService;

    /**
     * Contruct the permissions endpoint with the default values.
     */
    public Permissions() {
        this(cmsFeatureFlags().isEnableDatasetImport(),
                new AuthorisationServiceImpl(),
                (req, body, status) -> JsonUtils.writeResponse(req, body, status));
    }

    /**
     * COnstruct a new Permissions endpoint.
     *
     * @param enabled              true enables the endpoint, false all request valid or invaild will return 404.
     * @param authorisationService the authorisation service to use.
     * @param responseWriter       the http reponse writer impl to use.
     */
    public Permissions(boolean enabled,
                       AuthorisationService authorisationService,
                       HttpResponseWriter responseWriter) {
        this.enabled = enabled;
        this.authorisationService = authorisationService;
        this.httpResponseWriter = responseWriter;
    }

    @GET
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!enabled) {
            info().data("endpoint", "permissions")
                    .data("feature_flag", CMSFeatureFlags.ENABLE_DATASET_IMPORT)
                    .log("api endpoint reqiures CMD feature to be enabled request will not be processed");
            httpResponseWriter.writeJSONResponse(response, null, SC_NOT_FOUND);
            return;
        }

        String sessionID = null;
        String serviceToken = null;

        sessionID = request.getHeader(FLORENCE_AUTH_HEATHER);
        if (StringUtils.isNotEmpty(sessionID)) {
            info().log("handling permissions request for user");
            getUserPermissions(request, response, sessionID);
            return;
        }

        serviceToken = request.getHeader(SERVICE_AUTH_HEADER);
        if (StringUtils.isNotEmpty(serviceToken)) {
            info().log("handling permissions request for service");
            getServicePermissions(request, response);
            return;
        }

        info().log("invalid permissions request expected user or service auth header token but none found");
        httpResponseWriter.writeJSONResponse(response, null, SC_BAD_REQUEST);
    }

    private void getServicePermissions(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // TODO
    }

    private void getUserPermissions(HttpServletRequest request, HttpServletResponse response, String sessionID) throws IOException {
        String datasetID = request.getParameter(DATASET_ID_PARAM);
        if (StringUtils.isEmpty(datasetID)) {
            info().data("param", DATASET_ID_PARAM)
                    .log("invalid user permissions request mandatory parameter not provided");
            httpResponseWriter.writeJSONResponse(response, DATASET_ID_MISSING, SC_BAD_REQUEST);
            return;
        }

        String collectionID = request.getParameter(COLLECTION_ID_PARAM);
        if (StringUtils.isEmpty(collectionID)) {
            info().data("param", COLLECTION_ID_PARAM)
                    .log("invalid user permissions request mandatory parameter not provided");
            httpResponseWriter.writeJSONResponse(response, COLLECTION_ID_MISSING, SC_BAD_REQUEST);
            return;
        }

        DatasetPermissions callerPermissions = authorisationService.getUserPermissions(sessionID, datasetID, collectionID);
        try {
            httpResponseWriter.writeJSONResponse(response, callerPermissions, SC_OK);
        } catch (Exception ex) {
            error().exception(ex).log("error writing user permissions body to response");
            httpResponseWriter.writeJSONResponse(response, null, SC_INTERNAL_SERVER_ERROR);
        }
    }
}
