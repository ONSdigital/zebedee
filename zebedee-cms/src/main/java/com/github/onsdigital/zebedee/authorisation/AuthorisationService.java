package com.github.onsdigital.zebedee.authorisation;


/**
 * Service provides methods for determining user identity and authorisation.
 */
public interface AuthorisationService {

    /**
     * Get the {@link UserIdentity} from the specifed sessionID.
     *
     * @param sessionID the sessionID to get the user identity from
     * @return the users Identity if the sessionID is valid, the user exists and is authenticated.
     * @throws UserIdentityException if there is any problem determining the user identity.
     */
    UserIdentity identifyUser(String sessionID) throws UserIdentityException;
}
