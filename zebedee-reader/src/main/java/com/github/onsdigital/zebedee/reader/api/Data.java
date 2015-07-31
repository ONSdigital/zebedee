package com.github.onsdigital.zebedee.reader.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ZebedeeReader;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

/**
 * Created by bren on 29/07/15.
 * <p>
 * Endpoint to read published data
 */

@Api
public class Data {

    /**
     * Retrieves content or content resource files for the endpoint <code>/data/?uri=[uri]</code>
     * <p>
     * <p>
     * This endpoint retrieves content or file from either a collection or published data.
     *
     * @param request  This should contain a X-Florence-Token header for the current session and the collection name being worked on
     * @param response Servlet response
     * @return
     * @throws IOException           If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     * @throws NotFoundException     If the requested URI does not exist.
     * @throws BadRequestException   IF the request cannot be completed because of a problem with request parameters
     * @throws UnauthorizedException If collection requested but authentication token not available
     *                               If collection requested but current session does not have view permission
     */

    @GET
    public String get(HttpServletRequest request, HttpServletResponse response) throws IOException, ZebedeeException {
        String uri = request.getParameter("uri");
        Content content = ZebedeeReader.getInstance().getPublishedContent(uri);
        return ContentUtil.serialise(content);
    }


}
