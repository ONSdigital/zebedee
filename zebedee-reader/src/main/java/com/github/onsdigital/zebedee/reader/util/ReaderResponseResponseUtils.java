package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.mertics.service.MetricsService;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringReader;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logInfo;

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

        if (content instanceof Page) {
            Page page = (Page) content;
            response.setHeader("ONS-Page-Type", page.getType().toString());
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


    public static void sendNotFound(NotFoundException exception, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logInfo(exception.getMessage())
                .uri(request.getRequestURI() + "?" + request.getQueryString())
                .addParameter("statusCode", exception.statusCode)
                .log();
        response.setStatus(exception.statusCode);
        IOUtils.copy(new StringReader(exception.getMessage()), response.getOutputStream());
    }
}
