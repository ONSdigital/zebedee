package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.json.UserList;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import java.io.IOException;

/**
 * API for managing user accounts. For password management, see {@link Password}.
 */
@Api
public class Users {

    /**
     * Allows you to read the details of user accounts.
     * @param request Expects an "email" get parameter.
     * @param response <ul>
     *                      <li>The user details with the password hash removed.</li>
     *                      <li>If no email parameter is provided:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                      <li>if no user exists for the giver email:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                      </ul>
     * @return A {@link User} object.
     */
    @GET
    public Object read(HttpServletRequest request, HttpServletResponse response) throws IOException, NotFoundException, BadRequestException {

        String email = request.getParameter("email");
        if (StringUtils.isBlank(email)) {
            return Root.zebedee.users.getUserList(request, response);
        } else {
            return Root.zebedee.users.get(request, response, email);
        }

    }

    /**
     * Create a new user. The user will be created as inactive and no password will be set. Use {@link Password} to complete account setup.
     *
     * @param request Should contain a {@link User} Json message for the account details.
     * @param response <ul>
     *                      <li>The {@link User} that was created.</li>
     *                      <li>if the user (i.e. email) already exists:  {@link HttpStatus#CONFLICT_409}</li>
     *                      <li>if the user can't be created:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                      </ul>
     * @param user The {@link User} Json message for the account details.
     * @return The {@link User} that was created.
     * @throws IOException
     */
    @POST
    public User create(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {

        Session session = Root.zebedee.sessions.get(request);
        if (Root.zebedee.permissions.isAdministrator(session.email) == false) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return null;
        }

        if (Root.zebedee.users.exists(user)) {
            response.setStatus(HttpStatus.CONFLICT_409);
            return null;
        }


        User created = Root.zebedee.users.create(user);

        if (created == null) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
        }

        return removePasswordHash(created);
    }

    /**
     * Update details of a user account. NB it's not currently possible to change a user's email address.
     * @param request Should contain a {@link User} Json message for the account details.
     * @param response <ul>
     *                      <li>The {@link User} that was updated.</li>
     *                      <li>if the user (i.e. email) doesn't exist:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                      <li>if the user can't be updated: {@link HttpStatus#BAD_REQUEST_400}</li>
     *                      </ul>
     * @param user The {@link User} Json message containing updated account details.
     * @return The {@link User} that was updated.
     * @throws IOException
     */
    @PUT
    public User update(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
        Session session = Root.zebedee.sessions.get(request);
        if (Root.zebedee.permissions.isAdministrator(session.email) == false) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return null;
        }

        if (!Root.zebedee.users.exists(user)) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return null;
        }

        User updated = null;

        updated = Root.zebedee.users.update(user);

        // We'll allow changing the email at some point.
        // It entails renaming the json file and checking
        // that the new email doesn't already exist.

        if (updated == null) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
        }


        return removePasswordHash(updated);
    }

    /**
     * Allows you to delete a user account.
     * @param request Expects an "email" get parameter.
     * @param response <ul>
     *                      <li>The user details with the password hash removed.</li>
     *                      <li>If no email parameter is provided:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                      <li>if no user exists for the giver email:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                      </ul>
     * @return A {@link User} object.
     */
    @DELETE
    public User delete(HttpServletRequest request, HttpServletResponse response) throws IOException {

        Session session = Root.zebedee.sessions.get(request);
        if (Root.zebedee.permissions.isAdministrator(session.email) == false) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return null;
        }

        String email = request.getParameter("email");
        if (StringUtils.isBlank(email)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return null;
        }

        User result = Root.zebedee.users.get(email);
        if (result == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
        }



        return removePasswordHash(result);
    }

    private User removePasswordHash(User user) {
        if (user != null) {
            // Blank out the password hash.
            // Not strictly necessary, but sensible.
            user.passwordHash = null;
        }
        return user;
    }

    private UserList removePasswordHash(UserList users) {
        for (User user : users) {
            removePasswordHash(user);
        }
        return users;
    }
}
