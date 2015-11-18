package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
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
 * API for processing login requests.
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
    public String authenticate(HttpServletRequest request, HttpServletResponse response, Credentials credentials) throws IOException, NotFoundException, BadRequestException {

        if (credentials == null || StringUtils.isBlank(credentials.email)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return "Please provide credentials (email, password).";
        }

        User user = Root.zebedee.users.get(credentials.email);
        boolean result =  user.authenticate(credentials.password);

        if (!result) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return "Authentication failed.";
        }

        // Temponary whilst encryption is being put in place.
        // This can be removed once all users have keyrings.
        Root.zebedee.users.migrateToEncryption(user, credentials.password);

        if (BooleanUtils.isTrue(user.temporaryPassword)) {
            
            // Let Florence know that this user needs to change their password.
            // This isn't what 417 is intended for, but a 4xx variation on 401 seems sensible.
            // I guess we could use 418 just for fun and to avoid confusion.
            response.setStatus(HttpStatus.EXPECTATION_FAILED_417);
            return "Password change required";
        } else {
            response.setStatus(HttpStatus.OK_200);
        }

        return Root.zebedee.openSession(credentials).id;
    }

}
