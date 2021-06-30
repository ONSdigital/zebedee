package com.github.onsdigital.zebedee.session.store;

import com.github.onsdigital.zebedee.session.store.exceptions.*;
import org.apache.commons.lang.StringUtils;
import com.github.onsdigital.JWTHandlerImpl;
import com.github.onsdigital.exceptions.*;
import com.github.onsdigital.impl.*;
import com.github.onsdigital.interfaces.*;
import javax.servlet.http.HttpServletRequest;

import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.session.service.error.SessionClientException;
import java.io.IOException;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
import com.github.onsdigital.zebedee.user.model.User;

/**
 * This method creates a ThreadLocal for the validated jwt  
 * from a HTTP request and appropriate key 
 * as part of implementing dp-identity-api */

public class SessionsThreadLocalImpl implements Sessions {
    
    private static ThreadLocal<UserDataPayload> store = new ThreadLocal<>();

    public static ThreadLocal<UserDataPayload> getStore() {
        return store;
    }

    /**
     * Find a {@link Session} associated with the user email - defaults to the NoOp impl.
     */
    @Override
    public Session find(String email) throws IOException, SessionClientException {
        info().authIdentityTypeUser().log("no-op session find.");
        return null;
    }

    /**
     * Create a new {@link Session} for the user - defaults to the NoOp impl.
     */
    @Override
    public Session create(User user) throws IOException {
        info().authIdentityTypeUser().log("no-op session create.");
        return null;
    }

    /**
     * Check if the provided {@link Session} is expired - defaults to the NoOp impl.
     */
    @Override
    public boolean expired(Session session) {
        info().authIdentityTypeUser().log("no-op session expired.");
        return session == null;
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @param id the {@link String} to get the session object from thread local for.
     * @return session object from thread local.
     * @throws IOException for any problem getting a session from the request.
     */
    @Override
    public Session get(String id) throws IOException {
        return get();
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @param id the {@link HttpServletRequest} to get the session object from thread local for.
     * @return session object from thread local.
     * @throws IOException for any problem getting a session from the request.
     */
    @Override
    public Session get(HttpServletRequest id) throws IOException {
        return get();
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @param none.
     * @return session object from thread local.
     * @throws IOException for any problem getting a session from the request.
     */
    @Override
    public Session get() throws IOException {
        return get();
    }

        /**
     * Get a {@link Session} session object from thread local.
     *
     * @param token - the access token to be decoded, verified and stored.
     * @throws SessionsDecodeException/SessionsExpiredException/SessionsVerificationException.
     */
    @Override
    public String set(String token) throws SessionsDecodeException, SessionsVerificationException, SessionsTokenExpiredException {
        JWTHandler jwtHandler = new JWTHandlerImpl();
        try {
            store.set(
                jwtHandler.verifyJWT(token, secretKey)
            );
        } catch (JWTTokenExpiredException e) {
            throw new SessionsTokenExpiredException("JWT verification failed as token is expired.");
        } catch (JWTVerificationException e) {
            throw new SessionsVerificationException(e.getMessage().toString(), e);
        } catch (JWTDecodeException e) {
            throw new SessionsDecodeException(e.getMessage().toString(), e);
        }
        return "";
    }

}
