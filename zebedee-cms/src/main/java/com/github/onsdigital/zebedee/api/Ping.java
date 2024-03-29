package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.PingRequest;
import com.github.onsdigital.zebedee.json.PingResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;

/**
 * Created by thomasridd on 14/07/15.
 */
@Api
public class Ping {

    /**
     * Sends a message to a user session requiring it to stay alive
     * <p>
     * Returns true if the session is alive, false otherwise
     */
    @POST
    public PingResponse ping(HttpServletRequest request, HttpServletResponse response, PingRequest pingRequest) {
        PingResponse pingResponse = new PingResponse();

        if (Root.zebedee.getSessions().get() != null) {
            pingResponse.hasSession = true;
        }

        return pingResponse;
    }
}