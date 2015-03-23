package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.Session;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * Created by david on 12/03/2015.
 */
@Api
public class Login {

    @POST
    public String authenticate(HttpServletRequest request, HttpServletResponse response, Credentials credentials) throws IOException {

        if (credentials == null || StringUtils.isBlank(credentials.email)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return "Please provide credentials (email, password).";
        }

        boolean result = Root.zebedee.users.authenticate(credentials.email, credentials.password);

        if (!result) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return "Authentication failed.";
        }

        return getSession(credentials.email);
    }

    /**
     * Creates a new session, or returns an existing one if one exists.
     *
     * @param email The user's email address.
     * @return A session for this user, whether new or existing.
     * @throws IOException If a filesystem error occurs.
     */
    private String getSession(String email) throws IOException {

        // Get the session ID
        Session session = Root.zebedee.sessions.create(email);
        String sessionId = null;
        if (session != null) {
            sessionId = session.id;
        }

        return sessionId;
    }

}
