package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.PingRequest;
import com.github.onsdigital.zebedee.json.PingResponse;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.session.service.Sessions;
import com.github.onsdigital.zebedee.util.mertics.service.MetricsService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ReaderLogger.error;

/**
 * Created by thomasridd on 14/07/15.
 */
@Api
public class Ping {

    private static MetricsService metricsService = MetricsService.getInstance();

    /**
     * Sends a message to a user session requiring it to stay alive
     * <p>
     * Returns true if the session is alive, false otherwise
     */
    @POST
    public PingResponse ping(HttpServletRequest request, HttpServletResponse response, PingRequest pingRequest) throws IOException {
        if (pingRequest.lastPingTime != null && pingRequest.lastPingTime > 0) {
            metricsService.capturePing(pingRequest.lastPingTime);
        }

        PingResponse pingResponse = new PingResponse();

        setSessionDetails(request, pingResponse);

        return pingResponse;
    }

    private void setSessionDetails(HttpServletRequest request, PingResponse pingResponse) {
        String token = "";
        try {
            Sessions sessions = Root.zebedee.getSessions();
            token = RequestUtils.getSessionId(request);
            if (sessions.exists(token)) {
                Session session = sessions.get(token);
                if (session != null && !sessions.expired(session)) {
                    pingResponse.hasSession = true;
                    pingResponse.sessionExpiryDate = sessions.getExpiryDate(session);
                }
            }
        } catch (IOException e) {
            error().exception(e).log("error setting session details");
        }
    }
}