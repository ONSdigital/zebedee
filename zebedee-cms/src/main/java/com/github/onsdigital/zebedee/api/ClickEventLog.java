package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.logging.click.event.ClickEventLogFactory;
import com.github.onsdigital.zebedee.model.ClickEvent;
import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * API endpoint for logging user click events.
 */
@Api
public class ClickEventLog {

    private static ClickEventLogFactory clickEventLogFactory = ClickEventLogFactory.getInstance();

    /**
     * Log a click event.
     */
    @POST
    public void logEvent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        clickEventLogFactory.log(clickEventDetails(request));
    }

    private ClickEvent clickEventDetails(HttpServletRequest request) throws
            IOException {
        return new ObjectMapper().readValue(IOUtils.toByteArray(request.getInputStream()), ClickEvent.class);
    }
}
