package com.github.onsdigital.zebedee.filters;

import com.github.davidcarboni.restolino.framework.Filter;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.api.Login;
import com.github.onsdigital.zebedee.api.Password;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.json.Session;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthenticationFilter implements Filter {


    /**
     * This filter protects all resources except {@link com.github.onsdigital.zebedee.api.Login}.
     *
     * @param request
     * @param response
     * @return <ul>
     * <li>If the first path segment is login, true.</li>
     * <li>Otherwise, if a {@link com.github.onsdigital.zebedee.json.Session} can be found for the login token, true.</li>
     * <li>Otherwise false.</li>
     * </ul>
     */
    @Override
    public boolean filter(HttpServletRequest request, HttpServletResponse response) {

        // Pass through OPTIONS request without authentication for cross-origin preflight requests:
        if (StringUtils.equalsIgnoreCase("OPTIONS", request.getMethod())) {
            return true;
        }

        // Pass through without authentication for login requests:
        // Password requests check
        Path path = Path.newInstance(request);
        if (StringUtils.equalsIgnoreCase(Login.class.getSimpleName(), path.firstSegment())
                || StringUtils.equalsIgnoreCase(Password.class.getSimpleName(), path.firstSegment())) {
            return true;
        }

        // Check all other requests:
        boolean result = false;
        try {
            Session session = Root.zebedee.sessions.get(request);
            if (session == null) {
                forbidden(response);
            } else {
                result = true;
            }
        } catch (IOException e) {
            error(response);
        }
        return result;
    }

    private void forbidden(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setStatus(HttpStatus.UNAUTHORIZED_401);
        Serialiser.serialise(response, "Please log in");
    }

    private void error(HttpServletResponse response) {
        response.setContentType("application/json");
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
    }
}
