package com.github.onsdigital.zebedee.filters;

import com.github.davidcarboni.restolino.framework.Filter;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.json.Session;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AuthenticationFilter implements Filter {

    public static final String tokenHeader = "x-florence-token";

    /**
     * This filter protects all resources except {@link com.github.onsdigital.zebedee.api.Login}.
     *
     * @param request
     * @param response
     * @return <ul>
     * <li>If the first path segment is login, true.</li>
     * <li>Otherwise, if an {@value #tokenHeader} header is present and matches a {@link com.github.onsdigital.zebedee.json.Session}, true.</li>
     * <li>Otherwise false.</li>
     * </ul>
     */
    @Override
    public boolean filter(HttpServletRequest request, HttpServletResponse response) {

        // Pass through without authentication for login requests:
        Path path = Path.newInstance(request);
        if (StringUtils.equalsIgnoreCase("login", path.firstSegment())) {
            return true;
        }

        String token = request.getHeader(tokenHeader);
        try {
            boolean result = false;
            Session session = Root.zebedee.sessions.get(token);
            if (session == null) {
                response.setStatus(HttpStatus.FORBIDDEN_403);
                try {
                    Serialiser.serialise(response, "Please log in");
                } catch (IOException e1) {
                    System.out.println("Error sending error response.");
                    e1.printStackTrace();
                }
            } else {
                result = true;
            }
            return result;
        } catch (IOException e) {
            response.setContentType("application/json");
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            return false;
        }
    }
}
