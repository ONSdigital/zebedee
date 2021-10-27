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

import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.extractFilter;
import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.getRequestedLanguage;

@Api
public class PublishedData {

    /**
     * Retrieves content for endpoint <code>/publisheddata?uri=[uri]</code>
     * <p>
     * <p>
     * This endpoint retrieves and serves json from published data only.
     * <p>
     * It is possible to filter only certain bits of data using filters.
     * <p>
     * e.g. ?uri=/economy/environmentalaccounts/articles/greenhousegasemissions/2015-06-02&title will only return title of the requested content
     *
     * @param request  No authentication headers are required due to this only serving published content
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
        try {
            ReaderResponseResponseUtils.sendResponse(
                    new ReadRequestHandler(getRequestedLanguage(request))
                            .findPublishedContent(request, extractFilter(request)), response);
        } catch (NotFoundException exception) {
            ReaderResponseResponseUtils.sendNotFound(exception, request, response);
        }
    }
}
