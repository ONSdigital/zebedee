package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.json.JSONable;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@FunctionalInterface
public interface HttpResponseWriter {

    void writeJSONResponse(HttpServletResponse response, Object body, int status) throws IOException;
}
