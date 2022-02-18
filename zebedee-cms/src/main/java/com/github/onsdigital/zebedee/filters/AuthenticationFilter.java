package com.github.onsdigital.zebedee.filters;

import com.github.davidcarboni.restolino.framework.PreFilter;
import com.github.davidcarboni.restolino.framework.Priority;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.api.ClickEventLog;
import com.github.onsdigital.zebedee.api.Identity;
import com.github.onsdigital.zebedee.api.Login;
import com.github.onsdigital.zebedee.api.Password;
import com.github.onsdigital.zebedee.api.Ping;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.api.cmd.ServiceDatasetPermissions;
import com.github.onsdigital.zebedee.api.cmd.ServiceInstancePermissions;
import com.github.onsdigital.zebedee.api.cmd.UserDatasetPermissions;
import com.github.onsdigital.zebedee.api.cmd.UserInstancePermissions;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.reader.api.endpoint.Health;
import com.github.onsdigital.zebedee.reader.api.endpoint.PublishedData;
import com.github.onsdigital.zebedee.reader.api.endpoint.PublishedIndex;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import com.github.onsdigital.zebedee.search.api.endpoint.ReIndex;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.session.service.SessionsException;
import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

@Priority(2)
public class AuthenticationFilter implements PreFilter {

    static final String AUTH_HEADER = "Authorization";
    static final String BEARER_PREFIX = "Bearer ";
    private Sessions sessions;

    /**
     * AuthenticationFilter() - Default constructor - Creates new instance of AuthenticationFilter class
     */
    public AuthenticationFilter() {}

    /**
     * AuthenticationFilter(Sessions sessions) - Creates new instance of AuthenticationFilter class
     * and initialises class variables.
     * <p/>
     *
     * @param sessions           Root.zebedee.getSessions() object
     */
    public AuthenticationFilter(Sessions sessions) {
        this.sessions = sessions;
    }

    /**
     * Endpoints that do not require authorisation.
     */
    private static final ImmutableList<Class> NO_AUTH_REQUIRED = new ImmutableList.Builder<Class>()
            .add(Login.class)
            .add(Password.class)
            .add(ReIndex.class)
            .add(Ping.class)
            .add(ClickEventLog.class)
            .add(Identity.class)
            .add(UserDatasetPermissions.class)
            .add(UserInstancePermissions.class)
            .add(ServiceDatasetPermissions.class)
            .add(ServiceInstancePermissions.class)
            .add(Health.class)
            .add(PublishedData.class)
            .add(PublishedIndex.class)
            .build();

    /**
     * This filter ensures API requests are appropriately authenticated.
     *
     * For paths included in the <code>NO_AUTH_REQUIRED</code> list above, if a valid session token is provided it is
     * validated and stored on the thread, but any invalid token errors are ignored. Any internal IO exceptions from the
     * sessions service will always result in an internal server error because for some of the endpoints in the list the
     * session is optional and still used in some cases (e.g. /password requires the session when called by an admin,
     * but not when changing a temporary password). If we ignored all errors it would lead to unexpected behaviour in
     * these cases.
     *
     * @param request  the http request
     * @param response the http response
     * @return <code>true</code> if the request is authorised and the session was validated and stored successfully,
     *         otherwise return <code>false</code>
     */
    @Override
    public boolean filter(HttpServletRequest request, HttpServletResponse response) {
        /*
        Ensure that any existing session is clear in case this is a recycled thread
        Do not remove this line unless we are no longer using ThreadLocal for passing session information as it can
        result in privilege escalation. It is crucial that the session is removed from the thread upon each new request.
         */
        getSessions().resetThread();

        // Pass through OPTIONS request without authentication for cross-origin preflight requests:
        if (StringUtils.equalsIgnoreCase("OPTIONS", request.getMethod())) {
            return true;
        }

        Path path = Path.newInstance(request);
        String authToken = RequestUtils.getSessionId(request);

        try {
            verifyAndStoreAccessToken(authToken, path);
            return true;
        } catch (UnauthorizedException e) {
            unauthorisedRequestResponse(response, e.getMessage());
        } catch (Exception e) {
            error().exception(e).log(e.getMessage());
            internalServerErrorResponse(response);
        }
        return false;
    }

    /**
     *
     * @param authToken the auth token to verify
     * @param path      the request API path
     * @throws UnauthorizedException if an authenticated API path is requested, but no valid auth token was provided
     * @throws IOException if any
     */
    private void verifyAndStoreAccessToken(String authToken, Path path) throws UnauthorizedException, IOException {
        try {
            getSessions().set(authToken);
        } catch (SessionsException e) {
            if (authorisationRequired(path)) {
                // treat access token expired or malformed access token as unauthorised
                throw new UnauthorizedException(e.getMessage());
            }
        }
    }

    private void unauthorisedRequestResponse(HttpServletResponse response, String errorMessage) {
        try {
            Serialiser.serialise(response, errorMessage);
            response.setContentType(MediaType.JSON_UTF_8.toString());
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
        } catch (IOException ex) {
            fallbackErrorResponse(response);
        }
    }

    private void internalServerErrorResponse(HttpServletResponse response) {
        try {
            Serialiser.serialise(response, "Internal Server Error");
            response.setContentType(MediaType.JSON_UTF_8.toString());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
        } catch (IOException ex) {
            fallbackErrorResponse(response);
        }
    }

    private void fallbackErrorResponse(HttpServletResponse response) {
        response.setContentType(MediaType.JSON_UTF_8.toString());
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }

    private boolean authorisationRequired(Path path) {
        return NO_AUTH_REQUIRED.stream()
                .noneMatch(clazzName -> clazzName.getSimpleName().equalsIgnoreCase(path.lastSegment()));
    }

    /**
     * Lazy load the sessions service
     */
    private Sessions getSessions() {
        if (sessions == null) {
            synchronized (AuthenticationFilter.class) {
                if (sessions == null) {
                    this.sessions = Root.zebedee.getSessions();
                }
            }
        }
        return this.sessions;
    }
}
