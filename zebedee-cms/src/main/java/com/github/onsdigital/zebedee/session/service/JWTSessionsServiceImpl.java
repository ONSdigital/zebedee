package com.github.onsdigital.zebedee.session.service;

import com.github.onsdigital.exceptions.JWTDecodeException;
import com.github.onsdigital.exceptions.JWTTokenExpiredException;
import com.github.onsdigital.exceptions.JWTVerificationException;
import com.github.onsdigital.impl.UserDataPayload;
import com.github.onsdigital.interfaces.JWTHandler;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.exceptions.SessionsDecodeException;
import com.github.onsdigital.zebedee.session.service.exceptions.SessionsRequestException;
import com.github.onsdigital.zebedee.session.service.exceptions.SessionsException;
import com.github.onsdigital.zebedee.session.service.exceptions.SessionsTokenExpiredException;
import com.github.onsdigital.zebedee.session.service.exceptions.SessionsVerificationException;
import com.github.onsdigital.zebedee.user.model.User;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;


/**
 * JWTStore:  class, when instantiated, will
 * *********  allow for access token verification,
 * storage (set()) and retrieval (get())
 * from threadlocal.
 * <p>
 * Implements Sessions interface.
 */
public class JWTSessionsServiceImpl implements Sessions {

    private JWTHandler jwtHandler;

    private Map<String, String> rsaKeyMap;

    private Gson gson;

    private static ThreadLocal<UserDataPayload> store = new ThreadLocal<>();

    private final static int JWT_CHUNK_SIZE = 3;

    public static final String ACCESS_TOKEN_REQUIRED_ERROR = "Access Token required but none provided.";
    public static final String ACCESS_TOKEN_EXPIRED_ERROR = "JWT verification failed as token is expired.";
    public static final String TOKEN_NOT_VALID_ERROR = "Token format not valid.";
    public static final String TOKEN_NULL_ERROR = "Token cannot be null.";
    private static final String UNSUPPORTED_METHOD = "forbidden attempt to call sessions method that is not supported JWT sessions are enabled";

    // class constructor - takes HashMap<String, String> as param.
    public JWTSessionsServiceImpl(JWTHandler jwtHandler, Map<String, String> rsaKeyMap) {
        this.jwtHandler = jwtHandler;
        this.rsaKeyMap = rsaKeyMap;
        this.gson = new Gson();
    }

    /**
     * Find a {@link Session} associated with the user email - defaults to the NoOp impl.
     *
     * @deprecated The JWT based session lookup can only look up the session of the current user. Any code still
     *             referencing this method needs to be reworked so that the current users' session is used. Once the
     *             migration to the dp-identity-api is complete this method will be removed.
     */
    @Deprecated
    @Override
    public Session find(String email) throws IOException {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * Create a new {@link Session} for the user - defaults to the NoOp impl.
     *
     * @deprecated Using the new JWT based sessions, sessions are never created within zebedee as the JWT token
     *             issued by the dp-identity-api replaces the sessions in zebedee. Once migration to the dp-identity-api
     *             is completed this method will be removed.
     */
    @Deprecated
    @Override
    public Session create(User user) throws IOException {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * Check if the provided {@link Session} is expired - defaults to the NoOp impl.
     *
     * @deprecated This method is deprecated as it becomes redundant once we migrate to the JWT sessions. Once this
     *             migration has been completed the users' JWT (which is essentially the new session) is validated
     *             by the {@link com.github.onsdigital.zebedee.filters.AuthenticationFilter}. If the JWT is found to be
     *             expired by the {@link com.github.onsdigital.zebedee.filters.AuthenticationFilter} returns a 401
     *             unauthorised to the user so we would never get far enough in the execution to actually call this.
     */
    @Deprecated
    @Override
    public boolean expired(Session session) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @param id the {@link String} to get the session object from thread local for.
     * @return session object from thread local.
     * @throws IOException for any problem getting a session from the request.
     *
     * @deprecated Since the new JWT sessions implementation can only get the session of the current user, a single
     *             {@link this#get()} method is provided. Once migration to the new JWT sessions is completed all
     *             references to this method should be updated to use the {@link this#get()} instead.
     */
    @Deprecated
    @Override
    public Session get(String id) throws IOException {
        return get();
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @param req the {@link HttpServletRequest} to get the session object from thread local for.
     * @return session object from thread local if it exists, return null if no session exists.
     * @throws IOException for any problem getting a session from the request.
     *
     * @deprecated Since the new JWT sessions implementation can only get the session of the current user, a single
     *             {@link this#get()} method is provided. Once migration to the new JWT sessions is completed all
     *             references to this method that are not simply repeating the
     *             {@link com.github.onsdigital.zebedee.filters.AuthenticationFilter} should be should be updated to
     *             use {@link this#get()} instead. If the call is duplicating the filter, then it should be removed
     *             so as not to waste compute and request latency.
     */
    @Deprecated
    @Override
    public Session get(HttpServletRequest req) throws IOException {
        return get();
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @return session object from containg data from thread local.
     * @throws IOException for any problem getting a session from the request.
     */
    @Override
    public Session get() throws IOException {
        UserDataPayload jwtDetails = store.get();
        if (jwtDetails == null) {
            info().log("no user session found in Threadload session store");
            return null;
        }

        return new Session(jwtDetails);
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @param token - the access token to be decoded.
     * @throws IOException for any problem verifying a token or storing a session in threadlocal.
     */
    @Override
    public void set(String token) throws SessionsException {
        if (StringUtils.isEmpty(token)) {
            throw new SessionsRequestException(ACCESS_TOKEN_REQUIRED_ERROR);
        }

        String[] chunks;
        try {
            chunks = token.split("\\.");
        } catch (NullPointerException e) {
            throw new SessionsDecodeException(TOKEN_NULL_ERROR, e);
        }
        // check token validity; throw error if []chunks doesn't contain 3 elements
        if (chunks.length != JWT_CHUNK_SIZE) {
            throw new SessionsDecodeException(TOKEN_NOT_VALID_ERROR);
        }

        String publicSigningKey = getPublicSigningKey(chunks[0]);

        try {
            store.set(jwtHandler.verifyJWT(token, publicSigningKey));
        } catch (JWTTokenExpiredException e) {
            throw new SessionsTokenExpiredException(ACCESS_TOKEN_EXPIRED_ERROR);
        } catch (JWTVerificationException e) {
            throw new SessionsVerificationException(e.getMessage(), e);
        } catch (JWTDecodeException e) {
            throw new SessionsDecodeException(e.getMessage(), e);
        }
    }

    private String getPublicSigningKey(String tokenHeader) {
        String headerDecoded = new String(
                Base64.getDecoder().decode(tokenHeader),
                StandardCharsets.UTF_8
        );

        Map<String, String> decodedResult = this.gson.fromJson(
                headerDecoded,
                new TypeToken<Map<String, String>>() {
                }.getType()
        );

        // return rsa public signing key from this.rsaKeyMap
        return this.rsaKeyMap.get(
                decodedResult.get("kid")
        );
    }

    static void setStore(ThreadLocal<UserDataPayload> store) {
        JWTSessionsServiceImpl.store = store;
    }
}
