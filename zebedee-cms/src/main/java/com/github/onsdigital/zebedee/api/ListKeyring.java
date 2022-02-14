package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.keyring.CollectionKeyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.util.Set;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;

/**
 * API endpoint that returns a {@link Set} of collection ID's that a user has stored in their keyring.
 * (This is mainly for testing purposes for the keyring migration).
 */
@Api
public class ListKeyring {

    private CollectionKeyring collectionKeyring;
    private Sessions sessions;

    /**
     * Construct a new instance using the default configuration.
     */
    public ListKeyring() {
        this(
                Root.zebedee.getCollectionKeyring(),
                Root.zebedee.getSessions());
    }

    /**
     * Construct a new instance using the provided configuration.
     */
    public ListKeyring(final CollectionKeyring collectionKeyring, final Sessions sessions) {
        this.collectionKeyring = collectionKeyring;
        this.sessions = sessions;
    }

    /**
     * Return a {@link Set} of collection ID's for the collection keys stored in the user's keyring.
     */
    @GET
    public Set<String> listUserKeys(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException {
        Session session = sessions.get();
        if (session == null) {
            throw new UnauthorizedException("user not authorised to access this resource");
        }

        try {
            return collectionKeyring.list(session);
        } catch (KeyringException ex) {
            error().user(session.getEmail()).exception(ex).log("error listing user's keyring");
            throw new InternalServerError("internal server error");
        }
    }
}
