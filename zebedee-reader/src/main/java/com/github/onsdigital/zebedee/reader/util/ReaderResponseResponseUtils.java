package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.reader.Resource;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringReader;

/**
 * Created by bren on 31/07/15.
 */
public class ReaderResponseResponseUtils {

    public static void sendResponse(Object content, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON);
        IOUtils.copy(new StringReader(ContentUtil.serialise(content)), response.getOutputStream());
    }

    public static void sendResponse(Resource resource, HttpServletResponse response, String encoding) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(resource.getMimeType());
        response.setContentLengthLong(resource.getSize());
        if (encoding != null) {
            response.setCharacterEncoding(encoding);
        }
        response.setHeader("Content-Disposition", "inline; filename=\"" + resource.getName() + "\"");
        IOUtils.copy(resource.getData(), response.getOutputStream());
    }


    public static void sendResponse(Resource resource, HttpServletResponse response) throws IOException {
        sendResponse(resource, response, null);
    }
}
