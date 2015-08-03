package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringReader;

/**
 * Created by bren on 31/07/15.
 */
public class ResponseUtils {

    public static void sendResponse(Page content, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON);
        IOUtils.copy(new StringReader(ContentUtil.serialise(content)), response.getOutputStream());
    }

}
