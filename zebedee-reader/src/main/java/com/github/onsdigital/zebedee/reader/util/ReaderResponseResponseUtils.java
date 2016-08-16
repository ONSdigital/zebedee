package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.mertics.service.MetricsService;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringReader;

/**
 * Created by bren on 31/07/15.
 */
public class ReaderResponseResponseUtils {

    private static MetricsService metricsService = MetricsService.getInstance();

    public static void sendResponse(Object content, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON);
        if (content instanceof Content) {
            response.setHeader("Etag", ContentUtil.hash((Content) content));
        }
        IOUtils.copy(new StringReader(ContentUtil.serialise(content)), response.getOutputStream());
        metricsService.captureRequestResponseTimeMetrics();
    }

    public static void sendResponse(Resource resource, HttpServletResponse response, String encoding) throws IOException {
        byte[] bytes = IOUtils.toByteArray(resource.getData());
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(resource.getMimeType());
        if (encoding != null) {
            response.setCharacterEncoding(encoding);
        }
        response.setHeader("Content-Disposition", "inline; filename=\"" + resource.getName() + "\"");
        response.setHeader("Etag", ContentUtil.hash(bytes));
        IOUtils.write(bytes, response.getOutputStream());
        response.setContentLength(bytes.length);
    }


    public static void sendResponse(Resource resource, HttpServletResponse response) throws IOException {
        sendResponse(resource, response, null);
    }

}
