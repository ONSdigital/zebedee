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

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.warn;
import static com.github.onsdigital.zebedee.service.ServiceTokenUtils.extractServiceAccountTokenFromAuthHeader;
import static com.github.onsdigital.zebedee.service.ServiceTokenUtils.isValidServiceAuthorizationHeader;
import static com.github.onsdigital.zebedee.service.ServiceTokenUtils.isValidServiceToken;
import static com.github.onsdigital.zebedee.util.JsonUtils.writeResponseEntity;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.apache.hc.core5.http.HttpStatus.SC_UNAUTHORIZED;

/**
 * @deprecated The GET /identity endpoint is deprecated in favour of the new JWT sessions. Validating the JWT signature
 *             accomplishes the same functionality as this implementation, but in a more distributed and performant
 *             fashion.
 *
 * TODO: Once the migration to JWT sessions has been completed and all microservices have been updated to use the new
 *       dp-authorisation implementation that includes JWT validation, then these API endpoints should be removed
 */
@Deprecated
@Api
public class Identity {

    private AuthorisationService authorisationService;
    private ServiceStore serviceStore;

    static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Construct the default Identity api endpoint.
     */
    public Identity() {
        this(Root.zebedee.getServiceStore(), new AuthorisationServiceImpl());
    }

    /**
     * Construct and Identity api endpoint explicitly enabling/disabling the datasetImportEnabled feature.
     */
    public Identity(ServiceStore serviceStore, AuthorisationService authorisationService) {
        this.serviceStore = serviceStore;
        this.authorisationService = authorisationService;
    }

    /**
     * Endpoint validates user session token (provided via X-Florence-Token or Authorization header) and returns the
     * user's email for human users or the user's service name (e.g. dp-dataset-exporter) for automated users.
     *
     * This is currently used by dp-api-clients-go/identity client which is in turn used by dp-net/handlers/Identity.
     *
     * @deprecated usage of this endpoint is deprecated in favour of JWT validation provided via the dp-authorisation library.
     */
    @Deprecated
    @GET
    public void identifyUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String sessionID = RequestUtils.getSessionId(request);
        if (StringUtils.isNotBlank(sessionID)) {
            // If JWT Token then it will contain a "."
            // This logic removes the option to provide a legacy 'florence' token but is a transitional step
            // due to the complications of completely extricating the legacy auth code from zebedee.
            if (sessionID.contains(".")) {
                try {
                    UserIdentity identity = findUser(sessionID);
                    writeResponseEntity(response, identity, SC_OK);
                    return;
                } catch (UserIdentityException e) {
                    warn().log(e.getMessage());
                    writeResponseEntity(response, new Error(e.getMessage()), e.getResponseCode());
                    return;
                }
            } else {
                // TODO: Remove after new service user JWT auth is implemented
                ServiceAccount serviceAccount = handleServiceAccountRequest(sessionID);
                if (serviceAccount != null) {
                    writeResponseEntity(response, new UserIdentity(serviceAccount.getID()), SC_OK);
                    return;
                } else {
                    writeResponseEntity(response, new Error("service not authenticated"), SC_UNAUTHORIZED);
                    return;
                }
            }
        }
        writeResponseEntity(response, new Error("no authentication provided"), SC_UNAUTHORIZED);
    }

    private UserIdentity findUser(String sessionID) throws UserIdentityException {
        if (StringUtils.isEmpty(sessionID)) {
            throw new UserIdentityException("user not authenticated", SC_UNAUTHORIZED);
        }

        UserIdentity identity;
        try {
            identity = authorisationService.identifyUser(sessionID);
        } catch (UserIdentityException e) {
            error().logException(e, "identity endpoint: identify user failure, returning error response");
            throw e;
        }

        return identity;
    }

    private ServiceAccount handleServiceAccountRequest(String serviceToken) throws IOException {
        ServiceAccount serviceAccount = null;

        if (isValidServiceToken(serviceToken)) {
            serviceAccount = getServiceAccount(serviceToken);
        }
        return serviceAccount;
    }

    private ServiceAccount getServiceAccount(String serviceToken) throws IOException {
        ServiceAccount serviceAccount = null;
        try {
            serviceAccount = serviceStore.get(serviceToken);
            if (serviceAccount == null) {
                warn().log("service account not found for service token");
            }
        } catch (Exception ex) {
            error().exception(ex).log("unexpected error getting service account from service store");
            throw new IOException(ex);
        }
        return serviceAccount;
    }
}
