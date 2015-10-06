package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
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
     * Get a user or list of users
     *
     * Users are returned without password details
     *
     * @param request for a single user should include ?email=... parameter
     * @param response
     * @return a user with password removed
     *
     * @throws IOException
     * @throws NotFoundException - user email not found
     * @throws BadRequestException
     */
    @GET
    public Object read(HttpServletRequest request, HttpServletResponse response) throws IOException, NotFoundException, BadRequestException {

        String email = request.getParameter("email");
        Session session = Root.zebedee.sessions.get(request);

        // If email is empty
        if (StringUtils.isBlank(email)) {
            return removePasswordHash( Root.zebedee.users.getUserList(session) );
        } else {
            return removePasswordHash( Root.zebedee.users.get(session, email) );
        }

    }

    /**
     * Create a new user with basic info
     *
     * The user will be inactive until password details are assigned
     *
     * @param request Expects session details
     * @param response
     * @param user user details to create (requires name and email)
     * @return the new user
     *
     * @throws IOException - for general file system errors
     * @throws ConflictException - if user already exists
     * @throws BadRequestException - if insufficient user information is given
     * @throws UnauthorizedException - if the session does not have admin rights
     */
    @POST
    public User create(HttpServletRequest request, HttpServletResponse response, User user) throws IOException, ConflictException, BadRequestException, UnauthorizedException {
        Session session = Root.zebedee.sessions.get(request);
        User created = Root.zebedee.users.create(session, user);

        return removePasswordHash(created);
    }

    /**
     * Update user details
     *
     * At present user email cannot be updated
     *
     * @param request - requires an admin session
     * @param response
     * @param user - a user object with the new details
     * @return
     * @throws IOException
     * @throws UnauthorizedException - Session does not have update permissions
     * @throws NotFoundException - user account does not exist
     * @throws BadRequestException - problem with the update
     */
    @PUT
    public User update(HttpServletRequest request, HttpServletResponse response, User user) throws IOException, NotFoundException, BadRequestException, UnauthorizedException {
        Session session = Root.zebedee.sessions.get(request);

        User updated = Root.zebedee.users.update(session, user);

        return removePasswordHash(updated);
    }

    /**
     * Delete a user account
     *
     * @param request - requires an admin session
     * @param response
     * @param user - a user object to delete
     * @return
     * @throws UnauthorizedException
     * @throws IOException
     * @throws NotFoundException
     */
    @DELETE
    public boolean delete(HttpServletRequest request, HttpServletResponse response, User user) throws UnauthorizedException, IOException, NotFoundException {

        Session session = Root.zebedee.sessions.get(request);

        return Root.zebedee.users.delete(session, user);
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
