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
import static com.github.onsdigital.zebedee.service.ServiceTokenUtils.extractServiceAccountTokenFromAuthHeader;
import static com.github.onsdigital.zebedee.service.ServiceTokenUtils.isValidServiceAuthorizationHeader;
import static com.github.onsdigital.zebedee.service.ServiceTokenUtils.isValidServiceToken;
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
    static final Error NOT_FOUND_ERROR = new Error("Not found");

    /**
     * Construct the default Identity api endpoint.
     */
    public Identity() {
        this(cmsFeatureFlags().isEnableDatasetImport(), Root.zebedee.getServiceStore(), new AuthorisationServiceImpl());
    }

    /**
     * Construct and Identity api endpoint explicitly enabling/disabling the datasetImportEnabled feature.
     */
    public Identity(boolean datasetImportEnabled, ServiceStore serviceStore, AuthorisationService authorisationService) {
        this.datasetImportEnabled = datasetImportEnabled;
        this.serviceStore = serviceStore;
        this.authorisationService = authorisationService;
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
            ServiceAccount serviceAccount = handleServiceAccountRequest(request);
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
            UserIdentity identity = authorisationService.identifyUser(sessionID);
            info().data("user", identity.getIdentifier()).log("authenticated user identity confirmed");
            writeResponseEntity(response, identity, SC_OK);
        } catch (UserIdentityException e) {
            error().logException(e, "identity endpoint: identify user failure, returning error response");
            writeResponseEntity(response, new Error(e.getMessage()), e.getResponseCode());
        }
    }

    private ServiceAccount handleServiceAccountRequest(HttpServletRequest request) throws IOException {
        info().log("identity: handling service identity request");
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        ServiceAccount serviceAccount = null;

        if (isValidServiceAuthorizationHeader(authorizationHeader)) {
            String serviceToken = extractServiceAccountTokenFromAuthHeader(authorizationHeader);

            if (isValidServiceToken(serviceToken)) {
                serviceAccount = getServiceAccount(serviceToken);
            }
        }
        return serviceAccount;
    }

    private ServiceAccount getServiceAccount(String serviceToken) throws IOException {
        ServiceAccount serviceAccount = null;
        try {
            serviceAccount = serviceStore.get(serviceToken);
            if (serviceAccount == null) {
                warn().log("service account not found for service token");
            } else {
                info().data("service_id", serviceAccount.getID()).log("identified valid service account");
            }
        } catch (Exception ex) {
            error().exception(ex).log("unexpected error getting service account from service store");
            throw new IOException(ex);
        }
        return serviceAccount;
    }
}
