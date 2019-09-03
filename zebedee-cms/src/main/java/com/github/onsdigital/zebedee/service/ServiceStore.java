package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.model.ServiceAccount;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a repository for storing and retriving service accounts.
 */
public interface ServiceStore {

    /**
     * Get a service account by its token value.
     *
     * @param token the token of the service account to retrive.
     * @return the {@link ServiceAccount} if it exists otherwise return null;
     * @throws IOException thrown for any errors encountered getting the service account.
     */
    ServiceAccount get(String token) throws IOException;

    /**
     * Store a service account.
     *
     * @param token   of the service account to save.
     * @param service a byte array input stream of the service account to store.
     * @return a service account object for the byte array input stream saved.
     * @throws IOException any problem saving the service account.
     */
    ServiceAccount store(String token, InputStream service) throws IOException;
}