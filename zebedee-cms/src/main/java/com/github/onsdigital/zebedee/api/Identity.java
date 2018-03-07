package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.authorisation.IdentityError;
import com.github.onsdigital.zebedee.authorisation.JSONable;
import com.github.onsdigital.zebedee.authorisation.UserIdentity;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
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

    static final IdentityError SESSION_NOT_FOUND = new IdentityError("no session found with the specified ID");
    static final IdentityError INTERNAL_SERVER_ERROR = new IdentityError("unexpected error while attempting user identity");
    static final IdentityError USER_PERMISSIONS_NOT_FOUND = new IdentityError("permissions not found for this user");
    static final IdentityError USER_NOT_ATHORISED = new IdentityError("user is not authorised to permissions for this user");
    static final IdentityError GET_PERMISSIONS_ERROR = new IdentityError("unexpected error while attempting to get user permissions");

    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String CHAR_ENCODING = "UTF-8";

    private ZebedeeCmsService zebedeeCmsService;

    @GET
    public void identifyUser(HttpServletRequest request, HttpServletResponse response) throws IOException,
            NotFoundException, UnauthorizedException {
        logInfo("Identity: identifyUser").log();
        Session session;

        JSONable responseBody = null;
        int status;

        try {
            session = getZebedeeCmsService().getSession(request);
        } catch (ZebedeeException e) {
            logError(e, INTERNAL_SERVER_ERROR.getMessage()).log();
            writeResponse(response, INTERNAL_SERVER_ERROR, HttpStatus.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        if (session == null) {
            logWarn(SESSION_NOT_FOUND.getMessage()).log();
            writeResponse(response, SESSION_NOT_FOUND, HttpStatus.SC_NOT_FOUND);
            return;
        }

        PermissionDefinition permissions = null;
        try {
            permissions = getZebedeeCmsService().getPermissions()
                    .userPermissions(session.getEmail(), session);
        } catch (NotFoundException e) {
            logError(e, USER_PERMISSIONS_NOT_FOUND.getMessage()).user(session.getEmail()).log();
            writeResponse(response, USER_PERMISSIONS_NOT_FOUND, HttpStatus.SC_NOT_FOUND);
        } catch (UnauthorizedException e) {
            logError(e, USER_NOT_ATHORISED.getMessage()).user(session.getEmail()).log();
            writeResponse(response, USER_NOT_ATHORISED, HttpStatus.SC_NOT_FOUND);
        } catch (IOException e) {
            logError(e, INTERNAL_SERVER_ERROR.getMessage()).user(session.getEmail()).log();
            writeResponse(response, USER_NOT_ATHORISED, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        logInfo("user identified successfully").user(session.getEmail()).log();
        writeResponse(response, new UserIdentity(session, permissions), HttpStatus.SC_OK);
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
