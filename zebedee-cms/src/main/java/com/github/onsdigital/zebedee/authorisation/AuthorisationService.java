package com.github.onsdigital.zebedee.authorisation;


/**
 * ServiceAccount provides methods for determining user identity and authorisation.
 *
 * @deprecated The AuthorisationService is deprecated in favour of the new JWT sessions. Validating the JWT signature
 *             accomplishes the same functionality as this implementation, but in a more distributed fashion.
 *
 * TODO: Once the migration to JWT sessions has been completed and all microservices have been updated to use the new
 *       dp-authorisation implementation that includes JWT validation, then this service should be removed
 */
@Deprecated
public interface AuthorisationService {

    /**
     * Get the {@link UserIdentity} for the user the specifed sessionID belongs to.
     *
     * @param sessionID the sessionID to get the user identity from
     * @return the users Authenticate if the sessionID is valid, the user exists and is authenticated.
     * @throws UserIdentityException if there is any problem determining the user identity.
     */
    UserIdentity identifyUser(String sessionID) throws UserIdentityException;
}
