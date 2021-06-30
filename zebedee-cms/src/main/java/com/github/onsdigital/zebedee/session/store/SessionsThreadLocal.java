package com.github.onsdigital.zebedee.session.store;

import com.github.onsdigital.zebedee.session.store.exceptions.SessionsStoreException;

import javax.servlet.http.HttpServletRequest;

/**
 * This method creates a ThreadLocal for the validated jwt
 * from a HTTP request and appropriate key
 * as part of implementing dp-identity-api
 * Throws a SessionStoreException from the following list:-
 * SessionsDecodeException,
 * SessionsKeyException,
 * SessionsRequestException,
 * SessionsTokenExpiredException,
 * SessionsVerificationException
 */
public interface SessionsThreadLocal {

    /**
     * Stores a user session.
     *
     * @param request   the request to get the sesison detail from.
     * @param secretKey the key to use to verify the session details.
     * @throws SessionsStoreException thrown if there is a problem storing the session.
     */
    void store(HttpServletRequest request, String secretKey) throws SessionsStoreException;
}
