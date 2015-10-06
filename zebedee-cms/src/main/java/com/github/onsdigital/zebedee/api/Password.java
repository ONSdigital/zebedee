package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.Session;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * Created by david on 12/03/2015.
 */
@Api
public class Password {

    /**
     * Updates a user password.
     *
     * @param request     This should contain a {@link Credentials} Json object.
     * @param response    <ul>
     *                    <li>If setting the password succeeds: a 200 OK message.</li>
     *                    <li>If credentials are not provided:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                    <li>If the logged in user is not an administrator:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                    <li>If the password is not updated for any other reason:  {@link HttpStatus#BAD_REQUEST_400}.</li>
     *                    </ul>
     * @param credentials A {@link Credentials} Json object
     * @return A session ID to be passed in the {@value com.github.onsdigital.zebedee.model.Sessions#TOKEN_HEADER} header.
     * @throws IOException
     */
//    @POST
//    public String setPassword(HttpServletRequest request, HttpServletResponse response, Credentials credentials) throws IOException {
//
//        // Check the user session
//        Session session = Root.zebedee.sessions.get(request);
//        if (!Root.zebedee.permissions.isAdministrator(session.email)) {
//            response.setStatus(HttpStatus.UNAUTHORIZED_401);
//            System.out.println(session + " is not an administrator.");
//            return "Unauthorised.";
//        }
//
//        // Check the request
//        if (credentials == null || !Root.zebedee.users.exists(credentials.email)) {
//            response.setStatus(HttpStatus.BAD_REQUEST_400);
//            System.out.println(credentials + " is not a valid user.");
//            return "Please provide credentials (email, password).";
//        }
//
//        // Attempt to change the password
//        if (!Root.zebedee.users.setPassword(credentials.email, credentials.password, session)) {
//            response.setStatus(HttpStatus.BAD_REQUEST_400);
//            System.out.println(session + " failed to update password for " + credentials);
//            return "Failed to update password for " + credentials.email;
//        }
//
//        return "Password updated for " + credentials.email;
//    }

    /**
     * Update password
     *
     * Will set password as permanent if it is the user updating
     * Will set password as temporary if an admin does it
     *
     * @param request - a session with appropriate permissions
     * @param response
     * @param credentials - new credentials
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     * @throws BadRequestException
     */
    @POST
    public String setPassword(HttpServletRequest request, HttpServletResponse response, Credentials credentials) throws IOException, UnauthorizedException, BadRequestException {
        // Check the user session
        Session session = Root.zebedee.sessions.get(request);

        Root.zebedee.users.setPassword(session, credentials);

        return "Password updated for " + credentials.email;
    }
}
