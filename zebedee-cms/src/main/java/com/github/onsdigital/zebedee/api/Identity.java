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

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;
import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static com.github.onsdigital.zebedee.util.JsonUtils.writeResponseEntity;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

@Api
public class Identity {

    private AuthorisationService authorisationService;
    private ServiceStore serviceStore;
    private boolean datasetImportEnabled = false;

    static final String AUTHORIZATION_HEADER = "Authorization";
    static final String BEARER_PREFIX_UC = "Bearer";
    static final String BEARER_PREFIX_LC = "bearer";
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
            writeResponseEntity(response, NOT_FOUND_ERROR, SC_NOT_FOUND);
            return;
        }

        if (request.getHeader(RequestUtils.TOKEN_HEADER) != null) {
            findUser(request, response);
            return;
        }

        if (StringUtils.isNotBlank(request.getHeader(AUTHORIZATION_HEADER))) {
            info().log("checking service identity");
            ServiceAccount serviceAccount = findService(request);
            if (serviceAccount != null) {
                writeResponseEntity(response, new UserIdentity(serviceAccount.getID()), SC_OK);
                return;
            }
        }
        writeResponseEntity(response, new Error("service not authenticated"), SC_UNAUTHORIZED);
    }

    private void findUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String sessionID = RequestUtils.getSessionId(request);

        if (StringUtils.isEmpty(sessionID)) {
            Error responseBody = new Error("user not authenticated");
            warn().log(responseBody.getMessage());
            writeResponseEntity(response, responseBody, SC_UNAUTHORIZED);
            return;
        }

        try {
            UserIdentity identity = getAuthorisationService().identifyUser(sessionID);
            info().data("sessionId", sessionID).data("user", identity.getIdentifier())
                    .log("authenticated user identity confirmed");
            writeResponseEntity(response, identity, SC_OK);
        } catch (UserIdentityException e) {
            error().logException(e, "identity endpoint: identify user failure, returning error response");
            writeResponseEntity(response, new Error(e.getMessage()), e.getResponseCode());
        }
    }

    private ServiceAccount findService(HttpServletRequest request) throws IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (!isValidAuthorizationHeader(authorizationHeader)) {
            return null;
        }

        String serviceToken = removeBearerPrefix(authorizationHeader);

        ServiceAccount serviceAccount = null;
        try {
            serviceAccount = getServiceStore().get(serviceToken);
        } catch (IOException ex) {
            error().exception(ex).log("unexpected error getting service account");
            throw ex;
        }

        if (serviceAccount == null) {
            warn().log("service account not found for service token ");
            return null;
        }

        info().log("identified valid service account");
        return serviceAccount;
    }

    String removeBearerPrefix(String rawHeader) {
        if (StringUtils.isEmpty(rawHeader)) {
            warn().log("cannot remove Bearer prefix from null value");
            return null;
        }
        return rawHeader.replaceFirst(BEARER_PREFIX_UC, "").trim();
    }

    boolean isValidAuthorizationHeader(String value) {
        if (StringUtils.isEmpty(value)) {
            warn().log("invalid authorization header value is null or empty");
            return false;
        }

        if (!value.startsWith(BEARER_PREFIX_UC)) {
            warn().log("invalud authorization header value not prefixed with Bearer (case sensitive) returning null");
            return false;
        }
        return true;
    }

    private AuthorisationService getAuthorisationService() {
        if (authorisationService == null) {
            this.authorisationService = new AuthorisationServiceImpl();
        }
        return authorisationService;
    }

    private ServiceStore getServiceStore() {
        if (serviceStore == null) {
            this.serviceStore = Root.zebedee.getServiceStore();
        }
        return serviceStore;
    }

}
