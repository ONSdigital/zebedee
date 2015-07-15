package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.Session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * Created by thomasridd on 14/07/15.
 */
@Api
public class Ping {

    /**
     * Sends a message to a user session requiring it to stay alive
     *
     * Returns true if the session is alive, false otherwise
     */
    @POST
    public boolean ping(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Any access to a session keeps it alive
        Session session = Root.zebedee.sessions.get(request);
        if (session == null) {
            return false;
        } else {
            return true;
        }
    }
}