package com.github.onsdigital.zebedee.session.store;

import javax.servlet.http.HttpServletRequest;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsStoreException;

/**
 * This method creates a ThreadLocal for the validated jwt 
 * from a HTTP request and appropriate key 
 * as part of implementing dp-identity-api
 * Throws a SessionStoreException from the following list:-
 *              SessionsDecodeException, 
 *              SessionsKeyException,
 *              SessionsRequestException,
 *              SessionsTokenExpiredException,
 *              SessionsVerificationException 
 */

public interface SessionsThreadLocal {

    void store(HttpServletRequest request, String secretKey) throws SessionsStoreException;
}
