package com.github.onsdigital.zebedee.session.store;

import java.util.Map;
import java.util.Base64;
import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.google.common.reflect.TypeToken;
import com.github.onsdigital.exceptions.*;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import com.github.onsdigital.JWTHandlerImpl;
import com.github.onsdigital.impl.UserDataPayload;
import com.github.onsdigital.interfaces.JWTHandler;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsStoreException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsDecodeException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsRequestException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsTokenExpiredException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsVerificationException;

/**
 * JWTStore:  class, when instantiated, will
 * *********  allow for access token verification,
 *            storage (set()) and retrieval (get())
 *            from threadlocal.
 *           
 *            Implements Sessions interface.
 */
public class JWTStore implements Sessions {

    private JWTHandler jwtHandler = new JWTHandlerImpl();

    private Map<String, String> rsaKeyMap;

    private static ThreadLocal<UserDataPayload> store = new ThreadLocal<>();

    private final static int JWT_CHUNK_SIZE = 3;

    public final String ACCESS_TOKEN_REQUIRED_ERROR = "Access Token required but none provided.";
    public final String ACCESS_TOKEN_EXPIRED_ERROR  = "JWT verification failed as token is expired.";
    private static final String GET_STRING_ID_NOOP  = "Session get(String id) - no-Op.";
    private static final String GET_REQUEST_ID_NOOP = "Session get(HttpServletRequest id) - no-Op.";
    public final String TOKEN_NOT_VALID_ERROR       = "Token format not valid.";
    public final String TOKEN_NULL_ERROR            = "Token cannot be null.";

    // class constructor - takes HashMap<String, String> as param.
    public JWTStore(Map<String, String> rsaKeyMap) {
        this.rsaKeyMap = rsaKeyMap;
    }

    /**
     * Find a {@link Session} associated with the user email - defaults to the NoOp impl.
     */
    @Override
    public Session find(String email) throws IOException {
        return null;
    }

    /**
     * Create a new {@link Session} for the user - defaults to the NoOp impl.
     */
    @Override
    public Session create(User user) throws IOException {
        return null;
    }

    /**
     * Check if the provided {@link Session} is expired - defaults to the NoOp impl.
     */
    @Override
    public boolean expired(Session session) {
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
        info().log(GET_STRING_ID_NOOP);
        return null;
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
        info().log(GET_REQUEST_ID_NOOP);
        return null;
    }

    /**
     * Get a {@link Session} session object from thread local.
     *  
     * @param none.
     * @return session object from thread local.
     * @throws IOException for any problem getting a session from the request.
     */
    @Override
    public ThreadLocal<UserDataPayload> get() throws IOException {
        return store;
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @param token - the access token to be decoded.
     * @throws IOException for any problem verifying a token or storing a session in threadlocal.
     */
    @Override
    public void set(String token) throws SessionsStoreException {
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
            store.set(
                this.jwtHandler.verifyJWT(token, publicSigningKey)
            );
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

        Map<String, String> decodedResult = new Gson().fromJson(
            headerDecoded,
            new TypeToken<Map<String, String>>(){}.getType()
        );

        // return rsa public signing key from this.rsaKeyMap
        return this.rsaKeyMap.get(
            decodedResult.get("kid")
        );
    }

}
