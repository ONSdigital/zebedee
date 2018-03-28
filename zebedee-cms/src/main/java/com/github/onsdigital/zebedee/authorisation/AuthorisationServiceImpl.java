package com.github.onsdigital.zebedee.authorisation;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.service.ServiceSupplier;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.SessionsService;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

public class AuthorisationServiceImpl implements AuthorisationService {

	private ServiceSupplier<SessionsService> sessionServiceSupplier = () -> Root.zebedee.getSessionsService();
	private ServiceSupplier<UsersService> userServiceSupplier = () -> Root.zebedee.getUsersService();

	private static final String INTERNAL_ERROR = "internal server error";
    private static final String AUTHENTICATED_ERROR = "user not authenticated";
    private static final String USER_NOT_FOUND = "user does not exist";

	@Override
	public UserIdentity identifyUser(String sessionID) throws UserIdentityException {
		if (StringUtils.isEmpty(sessionID)) {
			logWarn("identify user error, no auth token was provided").log();
			throw new UserIdentityException(AUTHENTICATED_ERROR, SC_UNAUTHORIZED);
		}

		Session session;
		try {
			session = sessionServiceSupplier.getService().get(sessionID);
		} catch (IOException e) {
			logError(e, "identify user error, unexpected error while attempting to get user session")
					.sessionID(sessionID)
					.log();
			throw new UserIdentityException(INTERNAL_ERROR, SC_INTERNAL_SERVER_ERROR);
		}

		if (session == null) {
			logWarn("identify user error, session with specified ID could not be found")
					.sessionID(sessionID)
					.log();
			throw new UserIdentityException(AUTHENTICATED_ERROR, SC_UNAUTHORIZED);
		}

		// The session might exist but ensure the user still exists in the system before confirming their identity
		try {
			if (!userServiceSupplier.getService().exists(session.getEmail())) {
				logWarn("identify user error, valid user session found but user no longer exists")
						.session(session)
						.log();
				throw new UserIdentityException(USER_NOT_FOUND, SC_NOT_FOUND);
			}
		} catch (IOException e) {
			logError(e, "identify user error, unexpected error while checking if user exists")
					.session(session)
					.log();
			throw new UserIdentityException(INTERNAL_ERROR, SC_INTERNAL_SERVER_ERROR);
		}
		return new UserIdentity(session);
	}
}
