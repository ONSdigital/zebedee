package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.PingRequest;
import com.github.onsdigital.zebedee.util.InfluxDB;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    public boolean ping(HttpServletRequest request, HttpServletResponse response, PingRequest pingRequest) throws IOException {
        if (pingRequest.lastPingTime != null && pingRequest.lastPingTime > 0) {
            InfluxDB.Ping(pingRequest.lastPingTime);
        }

        return true;
    }

}