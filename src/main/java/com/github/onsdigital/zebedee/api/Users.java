package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.Credentials;
import com.github.onsdigital.zebedee.json.User;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import java.io.IOException;

/**
 * Created by david on 12/03/2015.
 */
@Api
public class Users {

    @GET
    public User read(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String email = request.getParameter("email");
        if (StringUtils.isBlank(email)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return null;
        }

        User result = Root.zebedee.users.get(email);

        if (result == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
        } else {
            // Blank out the password hash.
            // Not strictly necessary, but sensible.
            result.passwordHash = null;
        }

        return result;
    }

    @POST
    public User create(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {

        if (Root.zebedee.users.exists(user)) {
            response.setStatus(HttpStatus.CONFLICT_409);
            return null;
        }

        User created = Root.zebedee.users.create(user);

        if (created == null) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
        }

        return created;
    }

    @PUT
    public User update(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
        User result = null;

        if (Root.zebedee.users.exists(user)) {
            User existing = Root.zebedee.users.get(user.email);
            existing.name = user.name;
            existing.inactive = user.inactive;
            result = Root.zebedee.users.update(user);
        }

        // We'll allow changing the email at some point.
        // It entails renaming the json file and checking
        // that the new email doesn't already exist.

        return result;
    }

    private String computerSaysNo(HttpServletResponse response, String message) {
        response.setStatus(HttpStatus.UNAUTHORIZED_401);
        return message;
    }


    private boolean checkCredentials(Credentials credentials, HttpServletResponse response) {
        if (credentials == null || StringUtils.isBlank(credentials.email)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return false;
        }
        return true;
    }
}
