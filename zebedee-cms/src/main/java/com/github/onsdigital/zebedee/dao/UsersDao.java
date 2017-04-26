package com.github.onsdigital.zebedee.dao;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.json.User;

import javax.crypto.SecretKey;
import java.io.IOException;


public interface UsersDao {

    /**
     * Get a {@link User} by their email address.
     *
     * @param email the email address to query for.
     * @return the requested user.
     * @throws IOException         unexpected error while attempting to find the requested user.
     * @throws NotFoundException   the requested user does not exist.
     * @throws BadRequestException email address was empty or null.
     */
    User getByEmail(String email) throws IOException, NotFoundException, BadRequestException;

    /**
     * Remove collecton encryption keys from the {@link User} for collections that no longer exist.
     * @param userEmail the email address of the {@link User} to clean up.
     * @throws IOException
     * @throws NotFoundException
     * @throws BadRequestException
     */
    void removeStaleCollectionKeys(String userEmail) throws IOException, NotFoundException, BadRequestException;

    // TODO do we need to be able to add more than one at at time?

    /**
     *
     * @param email
     * @param keyIdentifier
     * @param key
     * @return
     * @throws IOException
     */
    User addKeyToKeyring(String email, String keyIdentifier, SecretKey key) throws IOException;


    /**
     *
     * @param email
     * @param keyIdentifier
     * @return
     * @throws IOException
     */
    User removeKeyFromKeyring(String email, String keyIdentifier) throws IOException;

    /**
     *
     * @param email
     * @return
     * @throws IOException
     */
    boolean exists(String email) throws IOException;
}
