package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.user.service.UsersService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * API for resetting or changing a password.
 */
@Api
public class Password {

    /**
     * Wrap static method calls to obtain service in function makes testing easier - class member can be
     * replaced with a mocked giving control of desired behaviour.
     */
    private ServiceSupplier<UsersService> usersServiceSupplier = () -> Root.zebedee.getUsersService();

    /**
     * Update password
     *
     * Will set password as permanent if it is the user updating
     * Will set password as temporary if an admin does it
     *
     * @param request     - a session with appropriate permissions
     * @param response
     * @param credentials - new credentials
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     * @throws BadRequestException
     */
    @POST
    public String setPassword(HttpServletRequest request, HttpServletResponse response, Credentials credentials) throws IOException, UnauthorizedException, BadRequestException, NotFoundException {

        // Get the user session
        Session session = Root.zebedee.getSessionsService().get(request);

        // If the user is not logged in, but they are attempting to change their password, authenticate using the old password
        if (session == null && credentials != null) {
            User user = usersServiceSupplier.getService().getUserByEmail(credentials.email);
            if (user.authenticate(credentials.oldPassword)) {
                Credentials oldPasswordCredentials = new Credentials();
                oldPasswordCredentials.email = credentials.email;
                oldPasswordCredentials.password = credentials.oldPassword;
                session = Root.zebedee.openSession(oldPasswordCredentials);
            }
        }

        // Attempt to change or reset the password:
        if (usersServiceSupplier.getService().setPassword(session, credentials)) {
            Audit.Event.PASSWORD_CHANGED_SUCCESS
                    .parameters()
                    .host(request)
                    .user(session.getEmail())
                    .log();
            return "Password updated for " + credentials.email;
        } else {
            Audit.Event.PASSWORD_CHANGED_FAILURE
                    .parameters()
                    .host(request)
                    .user(session.getEmail())
                    .log();
            return "Password not updated for " + credentials.email + " (there may be an issue with the user's keyring password).";
        }
    }
}
