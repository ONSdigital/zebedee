package com.github.onsdigital.zebedee.session.service;

import com.github.onsdigital.impl.UserDataPayload;
import com.github.onsdigital.zebedee.json.PermissionDefinition;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.permissions.store.PermissionsStore;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.store.SessionsStore;
import com.github.onsdigital.zebedee.teams.service.TeamsService;
import com.github.onsdigital.zebedee.teams.service.TeamsServiceImpl;
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

    private static ThreadLocal<UserDataPayload> store = new ThreadLocal<>();

    public static final String ACCESS_TOKEN_REQUIRED_ERROR = "access token required but none provided.";
    public static final String ACCESS_TOKEN_EXPIRED_ERROR = "session token lookup failed as token is expired.";

    public static final String ADMIN_GROUP = "role-admin";
    public static final String PUBLISHER_GROUP = "role-publisher";

    public ThreadLocalSessionsServiceImpl(SessionsStore sessionsStore, PermissionsService permissionsService,
                                          TeamsService teamsService) {
        super(sessionsStore);
        this.permissionsService = permissionsService;
        this.teamsService = teamsService;
    }

    /**
     * Get a {@link Session} session object from thread local.
     *
     * @param id the {@link String} to get the session object from thread local for.
     * @return the {@link Session} from thread local or <code>null</code> if no session is found.
     *
     * @deprecated Since the new JWT sessions implementation can only get the session of the current user, a single
     *             {@link this#get()} method is provided. Once migration to the new JWT sessions is completed all
     *             references to this method should be updated to use the {@link this#get()} instead.
     *
     * TODO: Write out usage of this method prior to JWT migration
     */
    @Deprecated
    @Override
    public Session get(String id) {
        return get();
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
     * Verify the session and store in ThreadLocal store.
     *
     * @param token - the access token to be verified and stored.
     * @throws SessionsException for any problem verifying a token or storing a session in ThreadLocal
     * @throws IOException if a filesystem error occurs
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
        PermissionDefinition permissions = permissionsService.userPermissions(session);

        if (permissions.isAdmin()) {
            teams.add(ADMIN_GROUP);
        }
        if (permissions.isEditor()) {
            teams.add(PUBLISHER_GROUP);
        }
        teams.addAll(teamsService.listTeamsForUser(session));

        store.set(new UserDataPayload(session.getEmail(), teams.toArray(new String[teams.size()])));
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

    static void setStore(ThreadLocal<UserDataPayload> store) {
        ThreadLocalSessionsServiceImpl.store = store;
    }
}
