package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.reader.Resource;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

/**
 * Created by bren on 31/07/15.
 */
public class ResponseUtils {

    public static void sendResponse(Content content, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON);
        IOUtils.copy(new StringReader(ContentUtil.serialise(content)), response.getOutputStream());
    }

    public static void sendResponse(Set<ContentNode> list, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON);
        IOUtils.copy(new StringReader(ContentUtil.serialise(list)), response.getOutputStream());
    }

    public static void sendResponse(Resource resource, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(resource.getMimeType());
        IOUtils.copy(resource.getData(), response.getOutputStream());
    }

}
