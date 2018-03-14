package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.session.model.Session;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

/**
 * Created by dave on 13/03/2018.
 */
public class AuthorisationServiceStub implements AuthorisationService {


    @Override
    public UserIdentity identifyUser(String sessionID) throws UserIdentityException {
        Session session = new Session();
        session.setEmail("test@ons.gv.uk");

        switch (sessionID) {
            case "1":
                logWarn("identify user error, unexpected error while attempting to get user session")
                        .addParameter("sessionID", sessionID)
                        .log();
                throw new UserIdentityException("internal server error", SC_INTERNAL_SERVER_ERROR);
            case "2":
                logWarn("identify user error, session with specified ID could not be found")
                        .addParameter("sessionID", sessionID)
                        .log();
                throw new UserIdentityException("user not authenticated", SC_UNAUTHORIZED);
            case "3":
                logWarn("identify user error, no user exists with the specified email")
                        .user(session.getEmail())
                        .addParameter("sessionID", sessionID)
                        .log();
                throw new UserIdentityException("user does not exist", SC_NOT_FOUND);
            case "4":
                logWarn("identify user error, unexpected error while checking if user exists")
                        .user(session.getEmail())
                        .addParameter("sessionID", sessionID)
                        .log();
                throw new UserIdentityException("internal server error", SC_INTERNAL_SERVER_ERROR);
            default:
                return new UserIdentity(session);
        }
    }
}
