package com.github.onsdigital.zebedee.reader.api.endpoint;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.util.RequestUtils;
import com.github.onsdigital.zebedee.util.ResponseUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

import static com.github.onsdigital.zebedee.util.RequestUtils.getRequestedLanguage;

/**
 * Created by bren on 03/08/15.
 */
@Api
public class Children {

    /**
     * Retrieves content endpoint <code>/children[collectionId]/?uri=[uri]</code>
     * <p>
     * This endpoint serves child content list for given uri.
     * It is possible to get children of child elements using depth parameter.
     * <p>
     * e.g. ?uri=/economy/environmentalaccounts/articles/greenhousegasemissions&depth=2 will serve children of greenhousegasemissions and children of all its children
     * <p>
     * Depth is 1 by default, meaning only immediate children will be retrieved.
     * <p>
     * Contents on the same level are sorted alphabetically on title field
     *
     * @param request  This should contain a X-Florence-Token header for the current session and the collection id being worked on
     *                 If no collection id is given published contents will be served
     * @param response
     * @throws IOException
     * @throws ZebedeeException
     */

    @GET
    public void get(HttpServletRequest request, HttpServletResponse response) throws IOException, ZebedeeException {
        ResponseUtils.sendResponse(new ReadRequestHandler(getRequestedLanguage(request)).listChildren(request,getDepth(request)), response);
    }

    private int getDepth(HttpServletRequest request) throws BadRequestException {
        String depth = request.getParameter("depth");
        String errorMessage = "Depth must be a positive value";
        if (StringUtils.isEmpty(depth)) {
            return 1;
        }
        try {
            Integer integer = Integer.valueOf(depth);
            if (integer < 0) {
                throw new BadRequestException(errorMessage);
            }
            return integer;
        } catch (NumberFormatException nfe) {
            throw new BadRequestException(errorMessage);
        }

    }


}
