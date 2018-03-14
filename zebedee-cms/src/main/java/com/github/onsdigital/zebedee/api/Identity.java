package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.authorisation.AuthorisationService;
import com.github.onsdigital.zebedee.authorisation.AuthorisationServiceImpl;
import com.github.onsdigital.zebedee.authorisation.UserIdentity;
import com.github.onsdigital.zebedee.authorisation.UserIdentityException;
import com.github.onsdigital.zebedee.json.JSONable;
import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

@Api
public class Identity {

    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String CHAR_ENCODING = "UTF-8";

    private AuthorisationService authorisationService;

    @GET
    public void identifyUser(HttpServletRequest request, HttpServletResponse response) throws IOException,
            UserIdentityException {
        String sessionID = RequestUtils.getSessionId(request);

        if (StringUtils.isEmpty(sessionID)) {
            Error responseBody = new Error("user not authenticated");
            logWarn(responseBody.getMessage()).log();
            writeResponse(response, responseBody, SC_UNAUTHORIZED);
            return;
        }

        try {
            UserIdentity identity = getAuthorisationService().identifyUser(sessionID);
            logInfo("user identity confirmed")
                    .addParameter("sessionID", sessionID)
                    .user(identity.getEmail())
                    .log();
            writeResponse(response, identity, SC_OK);
        } catch (UserIdentityException e) {
            writeResponse(response, new Error(e.getMessage()), e.getResponseCode());
        }
    }

    private void writeResponse(HttpServletResponse response, JSONable body, int status) throws IOException {
        try {
            response.setStatus(status);
            response.setContentType(JSON_CONTENT_TYPE);
            response.setCharacterEncoding(CHAR_ENCODING);
            response.getWriter().write(body.toJSON());
        } catch (IOException e) {
            logError(e, "error while attempting to write userIdentity to HTTP response").log();
            throw e;
        }
    }

    private AuthorisationService getAuthorisationService() {
        if (authorisationService == null) {
            this.authorisationService = new AuthorisationServiceImpl();
        }
        return authorisationService;
    }
}
