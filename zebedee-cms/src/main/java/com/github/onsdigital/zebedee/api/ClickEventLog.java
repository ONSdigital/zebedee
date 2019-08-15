package com.github.onsdigital.zebedee.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.response.Error;
import com.github.onsdigital.zebedee.model.ClickEvent;
import com.github.onsdigital.zebedee.util.HttpResponseWriter;
import com.github.onsdigital.zebedee.util.JsonUtils;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;
import java.io.InputStream;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

/**
 * API endpoint for logging user click events.
 */
@Api
public class ClickEventLog {

    private static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private HttpResponseWriter responseWriter;

    public ClickEventLog() {
        this.responseWriter = (resp, body, status) -> JsonUtils.writeResponseEntity(resp, body, status);
    }

    /**
     * Log a click event.
     */
    @POST
    public void logEvent(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            ClickEvent event = clickEventDetails(request);
            info().florenceClickEvent(event).log("florence click event");
        } catch (Exception ex) {
            error().exception(ex).log("error deserialising florence click event json");
            writeErrorResponse(response, new Error("internal server error"), 500);
        }
    }

    private ClickEvent clickEventDetails(HttpServletRequest request) throws
            IOException {
        try (InputStream inputStream = request.getInputStream()) {
            byte[] bodyBytes = IOUtils.toByteArray(request.getInputStream());
            return OBJECT_MAPPER.readValue(bodyBytes, ClickEvent.class);
        }
    }

    private void writeErrorResponse(HttpServletResponse response, Error error, int status) {
        try {
            responseWriter.writeJSONResponse(response, new Error("internal server error"), 500);
        } catch (Exception ex) {
            error().exception(ex).log("error attempting to write entity to response body");
            response.setStatus(500);
        }
    }
}
