package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.cryptolite.Random;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.Credentials;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * Created by david on 12/03/2015.
 */
@Api
public class Authenticate {

    @POST
    public String authenticate(HttpServletRequest request, HttpServletResponse response, Credentials credentials) throws IOException {

        if (credentials == null || StringUtils.isBlank(credentials.email)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return "Please provide credentials (email, password).";
        }

        String token  = Root.zebedee.users.authenticate(credentials.email, credentials.password);

        if (StringUtils.isNotBlank(token)) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return "Authentication failed.";
        }

        // Login token:
        // TODO: need to make this a real thing.
        return Random.id();
    }

}
