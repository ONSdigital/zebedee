package com.github.onsdigital.zebedee.session.service;

import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.store.LegacySessionsStore;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import com.github.onsdigital.zebedee.teams.service.TeamsServiceImpl;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A transitional {@link Sessions} service implementation that uses the legacy {@link SessionsServiceImpl} to validate
 * the session, but implements the ThreadLocal approach of the new JWT sessions service in order to enable us to gain
 * confidence in this approach prior to migration. It also depends on the {@link PermissionsStore} to get the
 * membership in the admin or publisher groups, as well as the {@link TeamsServiceImpl} for determining group membership.
 * Overall, this layer adds a performance hit in the short term, but allows us to centralise the pain here and refactor
 * the rest of zebedee.
 */
@Deprecated
public class ThreadLocalSessionsServiceImpl extends SessionsServiceImpl {

    private PermissionsService permissionsService;
    private TeamsService teamsService;

    private static ThreadLocal<Session> store = new ThreadLocal<>();

    public static final String ACCESS_TOKEN_REQUIRED_ERROR = "access token required but none provided.";
    public static final String ACCESS_TOKEN_EXPIRED_ERROR = "session token lookup failed as token is expired.";
    public static final String FAILED_PERMISSIONS_LOOKUP_ERROR = "unexpected error: failed to get permissions for user";

    public static final String ADMIN_GROUP = "role-admin";
    public static final String PUBLISHER_GROUP = "role-publisher";

    public ThreadLocalSessionsServiceImpl(LegacySessionsStore legacySessionsStore, PermissionsService permissionsService,
                                          TeamsService teamsService) {
        super(legacySessionsStore);
        this.permissionsService = permissionsService;
        this.teamsService = teamsService;
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
     * Verify the session and store in ThreadLocal store.
     *
     * @param token - the access token to be verified and stored.
     * @throws SessionsException for any problem verifying a token or storing a session in ThreadLocal
     * @throws IOException       if a filesystem error occurs
     */
    @Override
    public void set(String token) throws SessionsException, IOException {
        // Ensure that any existing session is clear in case this is a recycled thread
        resetThread();

        if (StringUtils.isBlank(token)) {
            throw new SessionsException(ACCESS_TOKEN_REQUIRED_ERROR);
        }

        Session session = super.get(token);
        if (session == null) {
            throw new SessionsException(ACCESS_TOKEN_EXPIRED_ERROR);
        }

        List<String> teams = new ArrayList<>();
        PermissionDefinition permissions = null;
        try {
            permissions = permissionsService.userPermissions(session.getEmail(), session);
        } catch (NotFoundException e) {
            throw new SessionsException(FAILED_PERMISSIONS_LOOKUP_ERROR);
        } catch (UnauthorizedException e) {
            // This should be impossible since the email and session passed in will always match
            throw new IOException("if you are seeing this it is definitely a bug, this error should be impossible");
        }

        if (permissions != null) {
            if (permissions.isAdmin()) {
                teams.add(ADMIN_GROUP);
            }
            if (permissions.isEditor()) {
                teams.add(PUBLISHER_GROUP);
            }
        }

        teams.addAll(teamsService.listTeamsForUser(session));

        store.set(new Session(session.getId(), session.getEmail(), teams));
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
        ThreadLocalSessionsServiceImpl.store = store;
    }
}
