package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.User;
import org.apache.commons.lang.BooleanUtils;
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

    /**
     * Authenticates with Zebedee.
     * @param request This should contain a {@link Credentials} Json object.
     * @param response <ul>
     *                      <li>If authentication succeeds: a new or existing session ID.</li>
     *                      <li>If credentials are not provided:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                      <li>If authentication fails:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                      </ul>
     * @param credentials The user email and password.
     * @return A session ID to be passed in the {@value com.github.onsdigital.zebedee.model.Sessions#TOKEN_HEADER} header.
     * @throws IOException
     */
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

        User user = Root.zebedee.users.get(credentials.email);
        if (BooleanUtils.isTrue(user.temporaryPassword)) {
            response.setStatus(HttpStatus.EXPECTATION_FAILED_417);
            return "Password change required";
        } else {
            response.setStatus(HttpStatus.OK_200);
        }

        return Root.zebedee.sessions.create(credentials.email).id;
    }

}
