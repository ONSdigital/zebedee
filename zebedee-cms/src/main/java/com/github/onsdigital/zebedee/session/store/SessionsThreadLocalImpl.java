package com.github.onsdigital.zebedee.session.store;

import com.github.onsdigital.exceptions.JWTDecodeException;
import com.github.onsdigital.exceptions.JWTTokenExpiredException;
import com.github.onsdigital.exceptions.JWTVerificationException;
import com.github.onsdigital.impl.UserDataPayload;
import com.github.onsdigital.interfaces.JWTHandler;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsDecodeException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsKeyException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsRequestException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsStoreException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsTokenExpiredException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsVerificationException;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * This method creates a ThreadLocal for the validated jwt from a HTTP request and appropriate key as part of
 * implementing dp-identity-api
 */
public class SessionsThreadLocalImpl implements SessionsThreadLocal {

    private static ThreadLocal<UserDataPayload> store = new ThreadLocal<>();

    private JWTHandler jwtHandler;

    /**
     * Create a new instance.
     *
     * @param jwtHandler the {@link JWTHandler} implementation to use.
     */
    public SessionsThreadLocalImpl(JWTHandler jwtHandler) {
        this.jwtHandler = jwtHandler;
    }

    @Override
    public void store(HttpServletRequest request, String secretKey) throws SessionsStoreException {
        String token = request.getHeader("Authorization");
        if (StringUtils.isEmpty(token)) {
            throw new SessionsRequestException("Authorization Header required but none provided.");
        }

        if (StringUtils.isEmpty(secretKey)) {
            throw new SessionsKeyException("Secret key value expected but was null or empty.");
        }

        try {
            store.set(jwtHandler.verifyJWT(token, secretKey));
        } catch (JWTTokenExpiredException e) {
            throw new SessionsTokenExpiredException("JWT verification failed as token is expired.");
        } catch (JWTVerificationException e) {
            throw new SessionsVerificationException(e.getMessage(), e);
        } catch (JWTDecodeException e) {
            throw new SessionsDecodeException(e.getMessage(), e);
        }
    }

    public static ThreadLocal<UserDataPayload> getStore() {
        return store;
    }

}
