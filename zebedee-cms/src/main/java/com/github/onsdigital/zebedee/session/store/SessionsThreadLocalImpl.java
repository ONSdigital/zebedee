package com.github.onsdigital.zebedee.session.store;

import com.github.onsdigital.zebedee.session.store.exceptions.*;

import com.github.onsdigital.exceptions.*;

import com.github.onsdigital.impl.*;
import com.github.onsdigital.interfaces.*;
import javax.servlet.http.HttpServletRequest;
import com.github.onsdigital.JWTHandlerImpl;

/**
 * This method creates a ThreadLocal for the validated jwt 
 * from a HTTP request and appropriate key 
 * as part of implementing dp-identity-api
 */

public class SessionsThreadLocalImpl implements SessionsThreadLocal {

    private static HttpServletRequest request;
    private static String secretKey;
    private static ThreadLocal<UserDataPayload> store =
            new ThreadLocal<>();

    private JWTHandler jwtHandler = new JWTHandlerImpl();


    public SessionsThreadLocalImpl(HttpServletRequest request, String secretKey) {
        this.request = request;
        this.secretKey = secretKey;
    }

    @Override
    public void store(HttpServletRequest request, String secretKey ) throws SessionsStoreException {
       /**
        * Validates the Http Request header for null or empty 
        * HttpServletRequest method for getHeader returns null if the request does not have a header of that name
        */  

        if ( request.getHeader("Authorization") == null || 
                request.getHeader("Authorization").isEmpty() ) {
                    throw new SessionsRequestException("Request does not have Authorization in Header");
                };

        /**
         * validates the Key argument for null or empty string
         *  */        
        if (secretKey == null || 
                secretKey.isEmpty()
         ) {
            throw new SessionsKeyException("Key is invalid");
         }

        /** From the two arguments adds the result of the jwtvalidation to Threadlocal store   */
        try {
            store.set(
            jwtHandler.verifyJWT(request.getHeader("Authorization"), secretKey));
        } catch (JWTTokenExpiredException e) {
            throw new SessionsTokenExpiredException(e.getMessage().toString(), e);
        } catch (JWTVerificationException e) {
            throw new SessionsVerificationException(e.getMessage().toString(), e);
        } catch (JWTDecodeException e) {
            throw new SessionsDecodeException(e.getMessage().toString(), e);
        }

    }

    public static ThreadLocal<UserDataPayload> getStore() {
        return store;
    }

}
