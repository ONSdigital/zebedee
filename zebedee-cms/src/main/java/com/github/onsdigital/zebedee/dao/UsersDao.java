package com.github.onsdigital.zebedee.dao;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.json.UserList;

import javax.crypto.SecretKey;
import java.io.IOException;


public interface UsersDao {

    String BLACK_EMAIL_MSG = "User email cannot be blank";
    String UNKNOWN_USER_MSG = "User for email {0} not found";
    String USER_ALREADY_EXISTS_MSG = "User for email {0} already exists";
    String REMOVING_STALE_KEY_LOG_MSG = "Removing stale collection key from user.";
    String SYSTEM_USER_ALREADY_EXISTS_MSG = "A system user already exists, no futher action required.";
    String CREATE_USER_AUTH_ERROR_MSG = "This account is not permitted to create users.";
    String USER_DETAILS_INVALID_MSG = "Name & email are required fields for User.";

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
     * @throws IOException
     * @throws NotFoundException
     * @throws BadRequestException
     */
    void removeStaleCollectionKeys(String userEmail) throws IOException, NotFoundException, BadRequestException;

    /**
     * @param email
     * @param keyIdentifier
     * @param key
     * @return
     * @throws IOException
     */
    User addKeyToKeyring(String email, String keyIdentifier, SecretKey key) throws IOException;

    /**
     * @param email
     * @param keyIdentifier
     * @return
     * @throws IOException
     */
    User removeKeyFromKeyring(String email, String keyIdentifier) throws IOException;

    /**
     * @param email
     * @return
     * @throws IOException
     */
    boolean exists(String email) throws IOException;

    /**
     *
     * @param user
     * @return
     * @throws IOException
     */
    boolean exists(User user) throws IOException;

    /**
     *
     * @param zebedee
     * @param user
     * @param password
     * @throws IOException
     * @throws UnauthorizedException
     * @throws NotFoundException
     * @throws BadRequestException
     */
    void createSystemUser(User user, String password) throws IOException, UnauthorizedException, NotFoundException, BadRequestException;

    /**
     *
     * @param user
     * @param password
     * @param session
     * @throws IOException
     * @throws UnauthorizedException
     * @throws ConflictException
     * @throws BadRequestException
     * @throws NotFoundException
     */
    void createPublisher(User user, String password, Session session) throws IOException,
            UnauthorizedException, ConflictException, BadRequestException, NotFoundException;

    /**
     *
     * @param session
     * @param user
     * @return
     * @throws UnauthorizedException
     * @throws IOException
     * @throws ConflictException
     * @throws BadRequestException
     */
    User create(Session session, User user) throws UnauthorizedException, IOException, ConflictException,
            BadRequestException;

    /**
     *
     * @param session
     * @param credentials
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     * @throws BadRequestException
     * @throws NotFoundException
     */
    boolean setPassword(Session session, Credentials credentials) throws IOException, UnauthorizedException,
            BadRequestException, NotFoundException;

    /**
     *
     * @return
     * @throws IOException
     */
    UserList list() throws IOException;

    /**
     *
     * @param session
     * @param user
     * @param updatedUser
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     * @throws NotFoundException
     * @throws BadRequestException
     */
    User update(Session session, User user, User updatedUser) throws IOException, UnauthorizedException,
            NotFoundException, BadRequestException;

    /**
     *
     * @param session
     * @param user
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     * @throws NotFoundException
     */
    boolean delete(Session session, User user) throws IOException, UnauthorizedException, NotFoundException;

    /**
     *
     * @param zebedee
     * @param user
     * @param password
     * @throws IOException
     */
    void migrateToEncryption(User user, String password) throws IOException;

}
