package com.github.onsdigital.zebedee.filters;

import com.github.davidcarboni.restolino.framework.PreFilter;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.api.ClickEventLog;
import com.github.onsdigital.zebedee.api.CsdbKey;
import com.github.onsdigital.zebedee.api.CsdbNotify;
import com.github.onsdigital.zebedee.reader.api.endpoint.Health;
import com.github.onsdigital.zebedee.api.Identity;
import com.github.onsdigital.zebedee.api.Login;
import com.github.onsdigital.zebedee.api.Password;
import com.github.onsdigital.zebedee.api.Ping;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.api.cmd.ServiceDatasetPermissions;
import com.github.onsdigital.zebedee.api.cmd.ServiceInstancePermissions;
import com.github.onsdigital.zebedee.api.cmd.UserDatasetPermissions;
import com.github.onsdigital.zebedee.api.cmd.UserInstancePermissions;
import com.github.onsdigital.zebedee.search.api.endpoint.ReIndex;
import com.github.onsdigital.zebedee.session.model.Session;
import com.google.common.collect.ImmutableList;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.github.onsdigital.zebedee.session.store.exceptions.SessionsStoreException;

import com.github.onsdigital.zebedee.session.service.Sessions;

import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;

public class AuthenticationFilter implements PreFilter {

    private Sessions sessions;

    public String NO_AUTH_HEADER_FOUND = "No authorisation header found. Exiting...";

    private boolean jwtSessionsEnabled = false;

    public AuthenticationFilter() {
        this(cmsFeatureFlags().isJwtSessionsEnabled(), Root.zebedee.getSessions());
    }

    public AuthenticationFilter(Boolean jwtSessionsEnabled, Sessions sessions) {
        this.jwtSessionsEnabled = jwtSessionsEnabled;
        this.sessions = sessions;
    }

    /**
     * Endpoints that do not require authorisation.
     */
    private static final ImmutableList<Class> NO_AUTH_REQUIRED = new ImmutableList.Builder<Class>()
            .add(Login.class)
            .add(Password.class)
            .add(CsdbKey.class)
            .add(CsdbNotify.class)
            .add(ReIndex.class)
            .add(Ping.class)
            .add(ClickEventLog.class)
            .add(Identity.class)
            .add(UserDatasetPermissions.class)
            .add(UserInstancePermissions.class)
            .add(ServiceDatasetPermissions.class)
            .add(ServiceInstancePermissions.class)
            .add(Health.class)
            .build();

    /**
     * This filter protects all resources except {@link com.github.onsdigital.zebedee.api.Login}.
     *
     * @param request
     * @param response
     * @return <ul>
     * <li>If the first path segment is login, true.</li>
     * <li>Otherwise, if a {@link Session} can be found for the login token, true.</li>
     * <li>Otherwise false.</li>
     * </ul>
     */
    @Override
    public boolean filter(HttpServletRequest request, HttpServletResponse response) {

        // Pass through OPTIONS request without authentication for cross-origin preflight requests:
        if (StringUtils.equalsIgnoreCase("OPTIONS", request.getMethod())) {
            return true;
        }

        Path path = Path.newInstance(request);
    
        if (noAuthorisationRequired(path)) {
            return true;
        }

        // Check all other requests:
        boolean result = false;
        if (jwtSessionsEnabled) {
            try {
                //ensure that authorisation header present
                if (request.getHeader("Authorization") == null) {
                    badRequest(response, NO_AUTH_HEADER_FOUND);
                } else {
                    try {
                        sessions.set(request.getHeader("Authorization"));   
                        result = true;
                    } catch (SessionsStoreException e) {
                        accessTokenError(response, e.getMessage());
                    }
                }
            } catch (IOException e) {
                error(response);
            }
        } else {
            try {
                Session session = sessions.get(request);
    
                if (session == null) {
                    forbidden(response);
                } else {
                    result = true;
                }
            } catch (IOException e) {
                error(response);
            }
        }
        return result;
    }

    private void badRequest(HttpServletResponse response, String errorMessage) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpStatus.BAD_REQUEST_400);
        Serialiser.serialise(response, errorMessage);
    }

    private void forbidden(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpStatus.UNAUTHORIZED_401);
        Serialiser.serialise(response, "Please log in");
    }

    private void accessTokenError(HttpServletResponse response, String errorMessage) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        Serialiser.serialise(response, errorMessage);
    }

    private void error(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }

    public boolean noAuthorisationRequired(Path path) {
        return NO_AUTH_REQUIRED
                .stream()
                .filter(clazzName -> clazzName.getSimpleName().toLowerCase().equals(path.lastSegment().toLowerCase()))
                .findFirst()
                .isPresent();
    }
}