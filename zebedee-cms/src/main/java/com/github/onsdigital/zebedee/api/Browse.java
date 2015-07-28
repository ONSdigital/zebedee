package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.content.DirectoryListing;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

/**
 * Created by david on 10/03/2015.
 */
@Api
public class Browse {

    /**
     * Retrieves a list of content at the endpoint /Browse/[CollectionName]?uri=[uri]
     *
     * @param request  This should contain a X-Florence-Token header for the current session and a {@code uri} GET parameter.
     * @param response A {@link DirectoryListing} of the specified directory.
     * @return DirectoryListing object for the requested uri.
     * @throws IOException
     * @throws UnauthorizedException If the user doesn't have view permissions.
     * @throws BadRequestException   If the URI denotes a file rather than a directory.
     * @throws NotFoundException     If the collection doesn't exist or if the URI cannot be found in the collection.
     */
    @GET
    public DirectoryListing browse(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException, UnauthorizedException, BadRequestException, NotFoundException {

        Collection collection = Collections.getCollection(request);
        Session session = Root.zebedee.sessions.get(request);

        String uri = request.getParameter("uri");
        if (StringUtils.isBlank(uri))
            uri = "/";

        return Root.zebedee.collections.listDirectory(collection, uri, session);
    }

}