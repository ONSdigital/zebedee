package com.github.onsdigital.zebedee.reader.api.endpoint;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.reader.util.ReaderResponseResponseUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.getRequestedLanguage;

/**
 * Created by bren on 03/08/15.
 * Endpoint to read resource files
 */
@Api
public class Resource {

    /**
     * Retrieves resource files for  endpoint <code>/resource[collectionId]/?uri=[uri]</code>
     * <p>
     * <p>
     * This endpoint serves resource file from either a collection or published data.
     *
     * @param request  This should contain a X-Florence-Token header for the current session and the collection id being worked on
     *                 If no collection id is given published contents will be served
     * @param response Servlet response
     * @return
     * @throws IOException           If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     * @throws NotFoundException     If the requested URI does not exist.
     * @throws BadRequestException   IF the request cannot be completed because of a problem with request parameters
     * @throws UnauthorizedException If collection requested but authentication token not available
     *                               If collection requested but current session does not have view permission
     */

    @GET
    public void read(HttpServletRequest request, HttpServletResponse response) throws IOException, ZebedeeException {
        try (com.github.onsdigital.zebedee.reader.Resource resource = new ReadRequestHandler(getRequestedLanguage(request)).findResource(request)) {
            ReaderResponseResponseUtils.sendResponse(resource, response);
        }
    }

}
