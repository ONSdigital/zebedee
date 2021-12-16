package com.github.onsdigital.zebedee.user.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;

import java.io.IOException;

/**
 * Interface defining User management functions.
 */
@Deprecated
public interface UsersService {

    String BLANK_EMAIL_MSG = "User email cannot be blank";
    String UNKNOWN_USER_MSG = "User for email {0} not found";
    String USER_ALREADY_EXISTS_MSG = "User for email {0} already exists";
    String SYSTEM_USER_ALREADY_EXISTS_MSG = "A system user already exists, no futher action required.";
    String CREATE_USER_AUTH_ERROR_MSG = "This account is not permitted to create users.";
    String USER_DETAILS_INVALID_MSG = "Name & email are required fields for User.";
    String USER_IS_NULL_MSG = "User was null";

    /**
     * Get a {@link User} by their email address.
     *
     * @param email the email address to query for.
     * @return the requested user.
     * @throws IOException         unexpected error while attempting to find the requested user.
     * @throws NotFoundException   the requested user does not exist.
     * @throws BadRequestException email address was empty or null.
     *
     * @deprecated as the users will be moved to dp-identity-api so this sort of lookup will not be possible. Any usages
     *             should either be written out or should be updated to get the current user from the JWT session instead
     */
    @Deprecated
    User getUserByEmail(String email) throws IOException, NotFoundException, BadRequestException;

    /**
     * Check if a user exists for the email address provided.
     *
     * @param email the email address of the user to search for.
     * @return true if a user exists with the specified email address false otherwise.
     * @throws IOException unexpected problem.
     *
     * @deprecated when JWT sessions are enabled we are no longer able to complete this check. This check is only used
     *             by the {@link com.github.onsdigital.zebedee.authorisation.AuthorisationServiceImpl} as an extra
     *             precaution when checking the users' session. This is because in the old implementation a session
     *             could live on even if the user was removed. When using the JWTs the need for this check is mitigated
     *             by the short validity duration of the JWT and the risk of a user continuing to perform for a few
     *             minutes after their user has been deactivated has been accepted.
     */
    @Deprecated
    boolean exists(String email) throws IOException;

    /**
     * Create a new system user.
     *
     * @param user     the user to create.
     * @param password the password for the user.
     * @throws IOException           unexpected problem creating user.
     * @throws UnauthorizedException unexpected problem creating user.
     * @throws NotFoundException     unexpected problem creating user.
     * @throws BadRequestException   unexpected problem creating user.
     *
     * @deprecated since this logic will no longer be required after migrating to the dp-identity-api.
     */
    @Deprecated
    void createSystemUser(User user, String password) throws IOException, UnauthorizedException, NotFoundException, BadRequestException;

    /**
     * Create a new user.
     *
     * @param session the session of the user creating the new user.
     * @param user    the user to create.
     * @return the new user.
     * @throws UnauthorizedException unexpected problem creating user.
     * @throws IOException           unexpected problem creating user.
     * @throws ConflictException     unexpected problem creating user.
     * @throws BadRequestException   unexpected problem creating user.
     *
     * @deprecated as the user management functionality is being migrated to the dp-identity-api.
     */
    @Deprecated
    User create(Session session, User user) throws UnauthorizedException, IOException, ConflictException,
            BadRequestException;

    /**
     * Set a user password.
     *
     * @param session     the session of the user setting the password.
     * @param credentials the credentials of user the password is being set on.
     * @return true if successfully updated, false otherwise.
     * @throws IOException           unexpected problem setting the user password.
     * @throws UnauthorizedException unexpected problem setting the user password.
     * @throws BadRequestException   unexpected problem setting the user password.
     * @throws NotFoundException     unexpected problem setting the user password.
     *
     * @deprecated as the user management functionality is being migrated to the dp-identity-api.
     */
    @Deprecated
    boolean setPassword(Session session, Credentials credentials) throws IOException, UnauthorizedException,
            BadRequestException, NotFoundException;

    /**
     * List all of the users.
     *
     * @throws IOException unexpected list the system users.
     *
     * @deprecated as the user management functionality is being migrated to the dp-identity-api.
     */
    @Deprecated
    UserList list() throws IOException;

    /**
     * Update a {@link User}.
     *
     * @param session     the {@link Session} of updating the user.
     * @param user        the user.
     * @param updatedUser the updated user.
     * @return the updated user.
     * @throws IOException           unexpected problem updating the user.
     * @throws UnauthorizedException unexpected problem updating the user.
     * @throws NotFoundException     unexpected problem updating the user.
     * @throws BadRequestException   unexpected problem updating the user.
     *
     * @deprecated as the user management functionality is being migrated to the dp-identity-api.
     */
    @Deprecated
    User update(Session session, User user, User updatedUser) throws IOException, UnauthorizedException,
            NotFoundException, BadRequestException;

    /**
     * Delete a {@link User}.
     *
     * @param session the {@link Session} of the user triggering the delete.
     * @param user    the user to be delete.
     * @return true if the user was successfully delete, false otherwise.
     * @throws IOException           unexpected problem deleting the user.
     * @throws UnauthorizedException unexpected problem deleting the user.
     * @throws NotFoundException     unexpected problem deleting the user.
     *
     * @deprecated as the user management functionality is being migrated to the dp-identity-api.
     */
    @Deprecated
    boolean delete(Session session, User user) throws IOException, UnauthorizedException, NotFoundException,
            BadRequestException;
}
