package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.authorisation.IdentityError;
import com.github.onsdigital.zebedee.authorisation.JSONable;
import com.github.onsdigital.zebedee.authorisation.UserIdentity;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;

@Api
public class Identity {

    static final String SESSION_NOT_FOUND_MSG = "no session found with the specified ID";
    static final IdentityError SESSION_NOT_FOUND = new IdentityError(SESSION_NOT_FOUND_MSG);

    static final String GET_SESSION_ERROR_MSG = "unexpected error while attempting get the session";
    static final IdentityError GET_SESSION_ERROR = new IdentityError(GET_SESSION_ERROR_MSG);

    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String CHAR_ENCODING = "UTF-8";

    private ZebedeeCmsService zebedeeCmsService;

    @GET
    public void identifyUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
        logInfo("Identity: identifyUser").log();
        Session session;

        JSONable responseBody = null;
        int status;

        try {
            session = getZebedeeCmsService().getSession(request);
        } catch (ZebedeeException e) {
            logError(e, GET_SESSION_ERROR_MSG).log();
            writeResponse(response, GET_SESSION_ERROR, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        if (session == null) {
            logWarn(SESSION_NOT_FOUND_MSG).log();
            writeResponse(response, SESSION_NOT_FOUND, HttpStatus.SC_NOT_FOUND);
            return;
        }

        logInfo("user identified successfully").user(session.getEmail()).log();
        writeResponse(response, new UserIdentity(session), HttpStatus.SC_OK);
    }

    private void writeResponse(HttpServletResponse response, JSONable body, int status) throws IOException {
        response.setStatus(status);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setCharacterEncoding(CHAR_ENCODING);
        response.getWriter().write(body.toJSON());
    }

    private ZebedeeCmsService getZebedeeCmsService() {
        if (zebedeeCmsService == null) {
            zebedeeCmsService = ZebedeeCmsService.getInstance();
        }
        return zebedeeCmsService;
    }
}
