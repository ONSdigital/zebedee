package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.authorisation.AuthorisationService;
import com.github.onsdigital.zebedee.authorisation.AuthorisationServiceImpl;
import com.github.onsdigital.zebedee.authorisation.UserIdentity;
import com.github.onsdigital.zebedee.authorisation.UserIdentityException;
import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import com.github.onsdigital.zebedee.service.ServiceStore;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static com.github.onsdigital.zebedee.util.JsonUtils.writeResponse;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

@Api
public class Identity {

    private AuthorisationService authorisationService;
    private ServiceStore serviceStore;
    private boolean datasetImportEnabled = false;

    static final String AUTHORIZATION_HEADER = "Authorization";
    static final Error NOT_FOUND_ERROR = new Error("Not found");

    /**
     * Construct the default Identity api endpoint.
     */
    public Identity() {
        this(cmsFeatureFlags().isEnableDatasetImport());
    }

    /**
     * Construct and Identity api endpoint explicitly enabling/disabling the datasetImportEnabled feature.
     */
    public Identity(boolean datasetImportEnabled) {
        this.datasetImportEnabled = datasetImportEnabled;
    }

    @GET
    public void identifyUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // FIXME CMD feature
        if (!datasetImportEnabled) {
            warn().data("responseStatus", SC_NOT_FOUND)
                    .log("Identity endpoint is not supported as feature EnableDatasetImport is disabled");
            writeResponse(response, NOT_FOUND_ERROR, SC_NOT_FOUND);
            return;
        }

        if (request.getHeader(RequestUtils.TOKEN_HEADER) != null) {
            findUser(request, response);
            return;
        }

        if (StringUtils.isNotBlank(request.getHeader(AUTHORIZATION_HEADER))) {
            ServiceAccount serviceAccount = findService(request);
            if (serviceAccount != null) {

                writeResponse(response, new UserIdentity(serviceAccount.getId()), SC_OK);
                return;
            }
        }
        writeResponse(response, new Error("service not authenticated"), SC_UNAUTHORIZED);
    }

    private void findUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String sessionID = RequestUtils.getSessionId(request);

        if (StringUtils.isEmpty(sessionID)) {
            Error responseBody = new Error("user not authenticated");
            warn().log(responseBody.getMessage());
            writeResponse(response, responseBody, SC_UNAUTHORIZED);
            return;
        }

        try {
            UserIdentity identity = getAuthorisationService().identifyUser(sessionID);
            info().data("sessionId", sessionID).data("user", identity.getIdentifier())
                    .log("authenticated user identity confirmed");
            writeResponse(response, identity, SC_OK);
        } catch (UserIdentityException e) {
            error().logException(e, "identify endpoint: identify user failure, returning error response");
            writeResponse(response, new Error(e.getMessage()), e.getResponseCode());
        }
    }

    private ServiceAccount findService(HttpServletRequest request) throws IOException {
        final ServiceStore serviceStore = getServiceStoreImpl();
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.isNotEmpty(authorizationHeader)) {
            authorizationHeader = authorizationHeader.toLowerCase();
            if (authorizationHeader.toLowerCase().startsWith("bearer ")) {
                String[] cred = authorizationHeader.split("bearer ");
                if (cred.length != 2) {
                    return null;
                }
                String token = cred[1];
                final ServiceAccount service = serviceStore.get(token);
                if (service != null) {
                    info().data("user", service.getId()).log("authenticated service account confirmed");
                    return service;
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    private AuthorisationService getAuthorisationService() {
        if (authorisationService == null) {
            this.authorisationService = new AuthorisationServiceImpl();
        }
        return authorisationService;
    }

    private ServiceStore getServiceStoreImpl() {
        if (serviceStore == null) {
            this.serviceStore = Root.zebedee.getServiceStore();
        }
        return serviceStore;
    }

}
