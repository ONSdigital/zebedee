package com.github.onsdigital.zebedee.session.service;

import com.github.onsdigital.exceptions.JWTDecodeException;
import com.github.onsdigital.exceptions.JWTTokenExpiredException;
import com.github.onsdigital.exceptions.JWTVerificationException;
import com.github.onsdigital.impl.UserDataPayload;
import com.github.onsdigital.interfaces.JWTHandler;
import com.github.onsdigital.zebedee.session.model.Session;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;


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

    private static final int JWT_CHUNK_SIZE = 3;

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
     * Create a new {@link Session} for the user - defaults to the NoOp impl.
     *
     * @deprecated Using the new JWT based sessions, sessions are never created within zebedee as the JWT token
     *             issued by the dp-identity-api replaces the sessions in zebedee. Once migration to the dp-identity-api
     *             is completed this method will be removed.
     */
    @Deprecated
    @Override
    public Session create(String email) throws IOException {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @return the {@link Session} from thread local or <code>null</code> if no session is found.
     */
    @Override
    public Session get() {
        UserDataPayload jwtDetails = store.get();
        if (jwtDetails == null) {
            return null;
        }

        return new Session(jwtDetails);
    }

    /**
     * Verify the session token and store in ThreadLocal store.
     *
     * @param token - the access token to be verified and stored.
     * @throws SessionsException for any problem verifying a token or storing a session in ThreadLocal.
     */
    @Override
    public void set(String token) throws SessionsException {
        // Ensure that any existing session is clear in case this is a recycled thread
        resetThread();

        if (StringUtils.isEmpty(token)) {
            throw new SessionsException(ACCESS_TOKEN_REQUIRED_ERROR);
        }

        String[] chunks;
        try {
            chunks = token.split("\\.");
        } catch (NullPointerException e) {
            throw new SessionsException(TOKEN_NULL_ERROR, e);
        }
        // check token validity; throw error if []chunks doesn't contain 3 elements
        if (chunks.length != JWT_CHUNK_SIZE) {
            throw new SessionsException(TOKEN_NOT_VALID_ERROR);
        }

        String publicSigningKey = getPublicSigningKey(chunks[0]);

        try {
            store.set(jwtHandler.verifyJWT(token, publicSigningKey));
        } catch (JWTTokenExpiredException e) {
            throw new SessionsException(ACCESS_TOKEN_EXPIRED_ERROR);
        } catch (JWTVerificationException | JWTDecodeException e) {
            throw new SessionsException(e.getMessage(), e);
        }
    }

    /**
     * Reset the thread by removing the current {@link ThreadLocal} value. If threads are being recycled to serve new
     * requests then this method must be called on each new request to ensure that sessions do not leak from one request
     * to the next causing potential for privilege excalation.
     */
    @Override
    public void resetThread() {
        store.remove();
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
