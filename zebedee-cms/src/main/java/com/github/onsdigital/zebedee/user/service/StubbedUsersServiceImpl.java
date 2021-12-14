package com.github.onsdigital.zebedee.user.service;

import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.model.UserList;

import java.io.IOException;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

/**
 * Implementation of the UsersService to be used during migration to JWT login using the dp-identity-api. All methods
 * will throw exceptions to help in identifying any missing dependencies we forget to update.
 */
public class StubbedUsersServiceImpl implements UsersService {

    private static final String UNSUPPORTED_METHOD = "unsupported attempt to call pre-JWT user service when JWT sessions are enabled";

    /**
     * Get a singleton instance of {@link StubbedUsersServiceImpl}.
     */
    public static UsersService getInstance() {
        return new StubbedUsersServiceImpl();
    }

    /**
     * @deprecated as the users will be moved to dp-identity-api so this sort of lookup will not be possible. Any usages
     *             should either be written out or should be updated to get the current user from the JWT session instead
     */
    @Deprecated
    @Override
    public User getUserByEmail(String email) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated when JWT sessions are enabled we are no longer able to complete this check. This check is only used
     *             by the {@link com.github.onsdigital.zebedee.authorisation.AuthorisationServiceImpl} as an extra
     *             precaution when checking the users' session. This is because in the old implementation a session
     *             could live on even if the user was removed. When using the JWTs the need for this check is mitigated
     *             by the short validity duration of the JWT and the risk of a user continuing to perform for a few
     *             minutes after their user has been deactivated has been accepted.
     */
    @Deprecated
    @Override
    public boolean exists(String email) throws IOException {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated since this logic will no longer be required after migrating to the dp-identity-api.
     */
    @Deprecated
    @Override
    public void createSystemUser(User user, String password) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated as the user management functionality is being migrated to the dp-identity-api.
     */
    @Deprecated
    @Override
    public User create(Session session, User user) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated as the user management functionality is being migrated to the dp-identity-api.
     */
    @Deprecated
    @Override
    public boolean setPassword(Session session, Credentials credentials) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated as the user management functionality is being migrated to the dp-identity-api.
     */
    @Deprecated
    @Override
    public UserList list() throws IOException, UnsupportedOperationException {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated as the user management functionality is being migrated to the dp-identity-api.
     */
    @Deprecated
    @Override
    public User update(Session session, User user, User updatedUser) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }

    /**
     * @deprecated as the user management functionality is being migrated to the dp-identity-api.
     */
    @Deprecated
    @Override
    public boolean delete(Session session, User user) {
        error().log(UNSUPPORTED_METHOD);
        throw new UnsupportedOperationException(UNSUPPORTED_METHOD);
    }
}
