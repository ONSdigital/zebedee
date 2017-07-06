package com.github.onsdigital.zebedee.user.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;

import javax.crypto.SecretKey;
import java.io.IOException;

/**
 * Interface defining User management functions.
 */
public interface UsersService {

    String BLACK_EMAIL_MSG = "User email cannot be blank";
    String UNKNOWN_USER_MSG = "User for email {0} not found";
    String USER_ALREADY_EXISTS_MSG = "User for email {0} already exists";
    String REMOVING_STALE_KEY_LOG_MSG = "Removing stale collection key from user.";
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
     */
    User getUserByEmail(String email) throws IOException, NotFoundException, BadRequestException;

    /**
     * Remove collecton encryption keys from the {@link User} for collections that no longer exist.
     *
     * @param userEmail the email address of the {@link User} to clean up.
     * @throws IOException         unexpected problem
     * @throws NotFoundException
     * @throws BadRequestException
     */
    void removeStaleCollectionKeys(String userEmail) throws IOException, NotFoundException, BadRequestException;

    /**
     * Add a collection key to a {@link User#keyring}
     *
     * @param email         the email of the user to add the key to.
     * @param keyIdentifier the collection ID the key is for.
     * @param key           the key to add.
     * @return the updated user.
     * @throws IOException unexpected problem adding key to user.
     */
    User addKeyToKeyring(String email, String keyIdentifier, SecretKey key) throws IOException;

    /**
     * Remove a collection key from a {@link User#keyring}.
     *
     * @param email         the email of the user to remove the key from.
     * @param keyIdentifier the ID of the collection of the key to remove.
     * @return the updated user.
     * @throws IOException unexpected problem removing the key from the user.
     */
    User removeKeyFromKeyring(String email, String keyIdentifier) throws IOException;

    /**
     * Check if a user exists for the email address provided.
     *
     * @param email the email address of the user to search for.
     * @return true if a user exists with the specified email address false otherwise.
     * @throws IOException unexpected problem.
     */
    boolean exists(String email) throws IOException;

    /**
     * Check if a user exists for the email address provided.
     *
     * @param user the user
     * @return true if a user exists with the specified email address false otherwise.
     * @throws IOException unexpected problem.
     */
    boolean exists(User user) throws IOException;

    /**
     * Create a new system user.
     *
     * @param user     the user to create.
     * @param password the password for the user.
     * @throws IOException           unexpected problem creating user.
     * @throws UnauthorizedException unexpected problem creating user.
     * @throws NotFoundException     unexpected problem creating user.
     * @throws BadRequestException   unexpected problem creating user.
     */
    void createSystemUser(User user, String password) throws IOException, UnauthorizedException, NotFoundException, BadRequestException;

    /**
     * Create a new publisher user.
     *
     * @param user     the user to create.
     * @param password the password to set.
     * @param session  the session of the user creating the new publisher user.
     * @throws IOException           unexpected problem creating user.
     * @throws UnauthorizedException unexpected problem creating user.
     * @throws ConflictException     unexpected problem creating user.
     * @throws BadRequestException   unexpected problem creating user.
     * @throws NotFoundException     unexpected problem creating user.
     */
    void createPublisher(User user, String password, Session session) throws IOException,
            UnauthorizedException, ConflictException, BadRequestException, NotFoundException;

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
     */
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
     */
    boolean setPassword(Session session, Credentials credentials) throws IOException, UnauthorizedException,
            BadRequestException, NotFoundException;

    /**
     * List all of the users.
     *
     * @throws IOException unexpected list the system users.
     */
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
     */
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
     */
    boolean delete(Session session, User user) throws IOException, UnauthorizedException, NotFoundException,
            BadRequestException;

    /**
     * Migrate the {@link User} to use collection key encryption. WE THINK THIS IS NO LONGER REQUIRED.
     */
    void migrateToEncryption(User user, String password) throws IOException;

    /**
     * Update a {@link User#keyring}.
     *
     * @param user the {@link User} to update.
     * @return the updated user.
     * @throws IOException unexpected problem updating the user keyring.
     */
    User updateKeyring(User user) throws IOException;

}
