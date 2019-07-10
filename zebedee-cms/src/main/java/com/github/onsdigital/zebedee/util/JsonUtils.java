package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.httpino.Serialiser;
import com.github.onsdigital.zebedee.json.JSONable;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class JsonUtils {

    private static Gson GSON = new Gson();

    public static boolean isValidJson(InputStream inputStream) {
        try {
            Serialiser.deserialise(inputStream, Object.class);
            return true;
        } catch (IOException | JsonSyntaxException e) {
            return false;
        }
    }

    public static void writeResponseEntity(HttpServletResponse response, JSONable body, int status) throws IOException {
        try {
            response.setStatus(status);
            response.setContentType(APPLICATION_JSON);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(body.toJSON());
        } catch (IOException e) {
            error().logException(e, "error while attempting to write userIdentity to HTTP response");
            throw e;
        }
    }

    public static void writeResponseEntity(HttpServletResponse response, Object body, int status) throws IOException {
        try {
            response.setStatus(status);
            response.setContentType(APPLICATION_JSON);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            if (body != null) {
                response.getWriter().write(GSON.toJson(body));
            }
        } catch (IOException e) {
            error().logException(e, "error while attempting to write userIdentity to HTTP response");
            throw e;
        }
    }
}
