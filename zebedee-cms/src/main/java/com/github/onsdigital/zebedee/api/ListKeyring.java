package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.InternalServerError;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.keyring.Keyring;
import com.github.onsdigital.zebedee.keyring.KeyringException;
import com.github.onsdigital.zebedee.permissions.service.PermissionsService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.user.model.User;
import com.github.onsdigital.zebedee.user.service.UsersService;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.util.Set;

import static com.github.onsdigital.zebedee.keyring.KeyringUtil.getUser;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;

/**
 * API endpoint that returns a {@link Set} of collection ID's that a user has stored in their keyring.
 * (This is mainly for testing purposes for the keyring migration).
 */
@Api
public class ListKeyring {

    private Keyring keyring;
    private Sessions sessions;
    private PermissionsService permissionsService;
    private UsersService usersService;

    /**
     * Construct a new instance using the default configuration.
     */
    public ListKeyring() {
        this(Root.zebedee.getCollectionKeyring(),
                Root.zebedee.getSessions(),
                Root.zebedee.getPermissionsService(),
                Root.zebedee.getUsersService());
    }

    /**
     * Construct a new instance using the provided configuration.
     */
    public ListKeyring(final Keyring keyring, final Sessions sessions,
                       final PermissionsService permissionsService, final UsersService usersService) {
        this.keyring = keyring;
        this.sessions = sessions;
        this.permissionsService = permissionsService;
        this.usersService = usersService;
    }

    /**
     * Return a {@link Set} of collection ID's for the collection keys stored in the user keyring. Endpoint requires
     * admin permissions.
     */
    @GET
    public Set<String> listUserKeys(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException {
        checkPermission(getSession(request));
        User user = getUser(usersService, getEmail(request));
        return listKeyring(user);
    }

    Session getSession(HttpServletRequest request) throws UnauthorizedException, InternalServerError {
        Session session;
        try {
            session = sessions.get(request);
        } catch (IOException ex) {
            error().exception(ex).log("error getting session from request");
            throw new InternalServerError("internal server error");
        }

        if (session == null) {
            throw new UnauthorizedException("user not authorised to access this resource");
        }

        return session;
    }

    void checkPermission(Session session) throws UnauthorizedException, InternalServerError {
        boolean isAdmin;
        try {
            isAdmin = permissionsService.isAdministrator(session);
        } catch (IOException ex) {
            error().exception(ex).log("error checking user permissions");
            throw new InternalServerError("internal server error");
        }

        if (!isAdmin) {
            throw new UnauthorizedException("admin permissions required to access this resource");
        }
    }

    String getEmail(HttpServletRequest request) throws BadRequestException {
        String email = request.getParameter("email");

        if (StringUtils.isEmpty(email)) {
            throw new BadRequestException("user email required but was null/empty");
        }

        return email;
    }

    Set<String> listKeyring(User user) throws InternalServerError {
        try {
            return keyring.list(user);
        } catch (KeyringException ex) {
            error().user(user.getEmail()).exception(ex).log("error listing user keyring");
            throw new InternalServerError("internal server error");
        }
    }
}
