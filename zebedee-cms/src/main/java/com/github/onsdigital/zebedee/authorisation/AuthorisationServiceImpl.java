package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.model.Collections;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.service.ServiceStore;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.warn;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

public class AuthorisationServiceImpl implements AuthorisationService {

    private ServiceSupplier<Sessions> sessionsSupplier = () -> Root.zebedee.getSessions();
    private ServiceSupplier<UsersService> userServiceSupplier = () -> Root.zebedee.getUsersService();
    private ServiceSupplier<Collections> collectionsSupplier = () -> Root.zebedee.getCollections();
    private ServiceSupplier<PermissionsService> permissionsServiceSupplier = () -> Root.zebedee.getPermissionsService();
    private ServiceSupplier<ServiceStore> serviceStoreSupplier = () -> Root.zebedee.getServiceStore();

    private static final String INTERNAL_ERROR = "internal server error";
    private static final String AUTHENTICATED_ERROR = "user not authenticated";
    private static final String USER_NOT_FOUND = "user does not exist";

    @Override
    public UserIdentity identifyUser(String sessionID) throws UserIdentityException {
        if (StringUtils.isEmpty(sessionID)) {
            warn().log("identify user error, no auth token was provided");
            throw new UserIdentityException(AUTHENTICATED_ERROR, SC_UNAUTHORIZED);
        }

        Session session;
        try {
            session = sessionsSupplier.getService().get(sessionID);
        } catch (IOException e) {
            error().logException(e, "identify user error, unexpected error while attempting to get user session");
            throw new UserIdentityException(INTERNAL_ERROR, SC_INTERNAL_SERVER_ERROR);
        }

        if (session == null) {
            warn().log("identify user error, session with specified ID could not be found");
            throw new UserIdentityException(AUTHENTICATED_ERROR, SC_UNAUTHORIZED);
        }

        // The session might exist but ensure the user still exists in the system before confirming their identity
        try {
            if (!userServiceSupplier.getService().exists(session.getEmail())) {
                warn().log("identify user error, valid user session found but user no longer exists");
                throw new UserIdentityException(USER_NOT_FOUND, SC_NOT_FOUND);
            }
        } catch (IOException e) {
            error().logException(e, "identify user error, unexpected error while checking if user exists");
            throw new UserIdentityException(INTERNAL_ERROR, SC_INTERNAL_SERVER_ERROR);
        }
        return new UserIdentity(session);
    }
}
