package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.httpino.Serialiser;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;
import com.github.onsdigital.zebedee.user.model.UserSanitised;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * API for managing user accounts. For password management, see {@link Password}.
 */
@Api
public class Users {

    private static final String EMAIL_PARAM = "email";

    /**
     * Wrap static method calls to obtain service in function makes testing easier - class member can be
     * replaced with a mocked giving control of desired behaviour.
     */
    private ServiceSupplier<UsersService> usersServiceSupplier = () -> Root.zebedee.getUsersService();

    /**
     * Get a user or list of users
     *
     * Users are returned without password or keyring details
     *
     * @param request  For a single user, include an {@code ?email=...} parameter.
     * @param response The requested user (null if the email can't be found) or a list of all users if no {@code email} parameter was provided.
     * @return A single user, or a list of users, with password(s) removed.
     * @throws IOException         If a general error occurs.
     * @throws NotFoundException   If the email is not found.
     * @throws BadRequestException If the email is blank.
     */
    @GET
    public Object read(HttpServletRequest request, HttpServletResponse response) throws IOException, NotFoundException, BadRequestException {
        Object result = null;

        String email = request.getParameter(EMAIL_PARAM);
        Session session = Root.zebedee.getSessionsService().get(request);

        if (session != null) {
            // If email is empty
            if (StringUtils.isBlank(email)) {
                result = sanitise(usersServiceSupplier.getService().list());
            } else {
                result = sanitise(usersServiceSupplier.getService().getUserByEmail(email));
            }
        }
        return result;
    }


    /**
     * Create a new user with basic info
     *
     * The user will be inactive until password details are assigned
     *
     * @param request  Expects session details.
     * @param response The created user, without password or keyring details.
     * @param user     User details to create (requires name and email)
     * @return the new user
     * @throws IOException           For general file system errors
     * @throws ConflictException     If user already exists
     * @throws BadRequestException   If insufficient user information is given
     * @throws UnauthorizedException If the session does not have admin rights
     */
    @POST
    public UserSanitised create(HttpServletRequest request, HttpServletResponse response, User user) throws
            IOException, ConflictException, BadRequestException, UnauthorizedException {
        Session session = Root.zebedee.getSessionsService().get(request);
        User created = usersServiceSupplier.getService().create(session, user);

        Audit.Event.USER_CREATED
                .parameters()
                .host(request)
                .actionedByEffecting(session.getEmail(), user.getEmail())
                .log();
        return sanitise(created);
    }

    /**
     * Update user details
     *
     * At present user email cannot be updated
     *
     * @param request  Requires an admin session
     * @param response The updated user
     * @param updatedUser     A user object with the new details
     * @return A sanitised view of the updated {@link User}
     */
    @PUT
    public UserSanitised update(HttpServletRequest request, HttpServletResponse response, User updatedUser) throws
            IOException, NotFoundException, BadRequestException, UnauthorizedException {
        Session session = Root.zebedee.getSessionsService().get(request);

        String email = request.getParameter(EMAIL_PARAM);
        User user = usersServiceSupplier.getService().getUserByEmail(email);
        User updated = usersServiceSupplier.getService().update(session, user, updatedUser);

        Audit.Event.USER_UPDATED
                .parameters()
                .host(request)
                .actionedByEffecting(session.getEmail(), user.getEmail())
                .log();
        return sanitise(updated);
    }

    /**
     * Delete a user account
     *
     * @param request  Requires an admin session - also an email as parameter
     * @param response Whether or not the user was deleted.
     * @return If the user was deleted, true.
     */
    @DELETE
    public boolean delete(HttpServletRequest request, HttpServletResponse response) throws
            UnauthorizedException, IOException, NotFoundException, BadRequestException {

        Session session = Root.zebedee.getSessionsService().get(request);
        String email = request.getParameter(EMAIL_PARAM);
        User user = usersServiceSupplier.getService().getUserByEmail(email);
        boolean result = usersServiceSupplier.getService().delete(session, user);
        if(result) {
            Audit.Event.USER_DELETED
                    .parameters()
                    .host(request)
                    .actionedByEffecting(session.getEmail(), user.getEmail())
                    .log();
        }

        return result;
    }

    // Methods to sanitise user account records going outside the system.
    // The details that are removed are already encrypted or hashed
    // so this is not strictly necessary, but is sensible.

    /**
     * @param user The user to be sanitised.
     * @return A {@link UserSanitised} instance.
     */

    private UserSanitised sanitise(User user) {
        UserSanitised result = null;
        if (user != null) {
            // Blank out the password and keyring.
            // Not strictly necessary, but sensible.
            result = Serialiser.deserialise(Serialiser.serialise(user), UserSanitised.class);
        }
        return result;
    }

    /**
     * @param users The list of users to be sanitised.
     * @return A {@link UserSanitised} list.
     */
    private List<UserSanitised> sanitise(UserList users) {
        List<UserSanitised> result = new ArrayList<>();
        for (User user : users) {
            if (user != null) {
                result.add(sanitise(user));
            }
        }
        return result;
    }
}
