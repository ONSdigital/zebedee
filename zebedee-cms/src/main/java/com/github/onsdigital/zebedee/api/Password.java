package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.Session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * Created by david on 12/03/2015.
 */
@Api
public class Password {

    /**
     * Update password
     *
     * Will set password as permanent if it is the user updating
     * Will set password as temporary if an admin does it
     *
     * @param request - a session with appropriate permissions
     * @param response
     * @param credentials - new credentials
     * @return
     * @throws IOException
     * @throws UnauthorizedException
     * @throws BadRequestException
     */
    @POST
    public String setPassword(HttpServletRequest request, HttpServletResponse response, Credentials credentials) throws IOException, UnauthorizedException, BadRequestException {
        // Check the user session
        Session session = Root.zebedee.sessions.get(request);

        Root.zebedee.users.setPassword(session, credentials);

        return "Password updated for " + credentials.email;
    }
}
