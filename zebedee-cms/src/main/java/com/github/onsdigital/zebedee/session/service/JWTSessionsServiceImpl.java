package com.github.onsdigital.zebedee.session.service;

import com.github.onsdigital.JWTVerifier;
import com.github.onsdigital.UserDataPayload;
import com.github.onsdigital.exceptions.JWTDecodeException;
import com.github.onsdigital.exceptions.JWTTokenExpiredException;
import com.github.onsdigital.exceptions.JWTVerificationException;
import com.github.onsdigital.zebedee.model.ServiceAccount;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;

import static com.github.onsdigital.zebedee.service.ServiceTokenUtils.isValidServiceToken;

import java.io.IOException;

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

    public static final String ACCESS_TOKEN_REQUIRED_ERROR = "Access Token required but none provided.";
    public static final String ACCESS_TOKEN_EXPIRED_ERROR = "JWT verification failed as token is expired.";
    private static final String UNSUPPORTED_METHOD = "forbidden attempt to call sessions method that is not supported JWT sessions are enabled";

    private static ThreadLocal<Session> store = new ThreadLocal<>();
    private JWTVerifier jwtVerifier;
    private ServiceStore serviceStore;
    private Gson gson;

    /**
     * Initialises a new {@link JWTSessionsServiceImpl}.
     *
     * @param jwtVerifier the {@link JWTVerifier} implementation to use to verify JWTs
     */
    public JWTSessionsServiceImpl(JWTVerifier jwtVerifier, ServiceStore serviceStore) {
        this.jwtVerifier = jwtVerifier;
        this.serviceStore = serviceStore;
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
        return store.get();
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

        if (StringUtils.isBlank(token)) {
            throw new SessionsException(ACCESS_TOKEN_REQUIRED_ERROR);
        }
        if (StringUtils.contains(token, ".")) {
            // This is a user token
            try {
                UserDataPayload jwtData = jwtVerifier.verify(token);
                store.set(new Session(token, jwtData.getEmail(), jwtData.getGroups()));
            } catch (JWTTokenExpiredException e) {
                throw new SessionsException(ACCESS_TOKEN_EXPIRED_ERROR);
            } catch (JWTVerificationException | JWTDecodeException e) {
                throw new SessionsException(e.getMessage(), e);
            }
        } else {
            // This might be a service token.
            try {
                if (isValidServiceToken(token)) {
                    ServiceAccount serviceAccount = getServiceAccount(token);
                    if (serviceAccount == null) {
                        throw new SessionsException("invalid service token - no matching service account found");
                    }
                    store.set(new Session(token, serviceAccount.getID()));
                } else {
                    throw new SessionsException("invalid service token format");
                }
            } catch (IOException e) {
                throw new SessionsException("error retrieving service account for service token", e);
            }   
        }
    }

    private ServiceAccount getServiceAccount(String serviceToken) throws IOException {
        ServiceAccount serviceAccount = null;
        try {
            serviceAccount = serviceStore.get(serviceToken);
        } catch (Exception ex) {
            error().exception(ex).log("unexpected error getting service account from service store");
            throw new IOException(ex);
        }
        return serviceAccount;
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

    @VisibleForTesting
    static void setStore(ThreadLocal<Session> store) {
        JWTSessionsServiceImpl.store = store;
    }
}
