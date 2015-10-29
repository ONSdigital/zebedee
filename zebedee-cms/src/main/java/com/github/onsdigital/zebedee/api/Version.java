package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.content.item.ContentItemVersion;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import java.io.IOException;
import java.net.URI;

@Api
public class Version {

    /**
     * Create a new version of a URI: <code>/Version/[CollectionName]/?uri=[uri]</code>
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response Returns the uri of the version created
     *                 <p>
     *                 Returns HTTP 200 if the version was created. The body of the response will contain the uri to the version.
     *                 Returns HTTP 404 if the content does not already exist as published content to create a version from.
     *                 Returns HTTP 409 if a version has already been created for this URI in the collection
     *                 Returns HTTP 401 if the user is not authorised to edit content.
     */
    @POST
    public URI create(HttpServletRequest request, HttpServletResponse response) throws IOException, NotFoundException, UnauthorizedException, ConflictException {

        Session session = Root.zebedee.sessions.get(request);
        if (session == null || !Root.zebedee.permissions.canEdit(session.email)) {
            throw new UnauthorizedException("You are not authorised to edit content.");
        }

        Collection collection = Collections.getCollection(request);
        String uri = request.getParameter("uri");

        ContentItemVersion version = collection.version(session.email, uri);
        return version.getUri();
    }

    /**
     * Delete the version at the given URI added to a file as part of a collection.
     * <code>/Version/[CollectionName]/?uri=[uri]</code>
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response True if successfully deleted
     *                 Returns HTTP 200 if the version was deleted.
     *                 Returns HTTP 404 if the version specified does not exist as part of the collection
     *                 Returns HTTP 401 if the user is not authorised to edit content.
     */
    @DELETE
    public boolean delete(HttpServletRequest request, HttpServletResponse response) throws IOException, BadRequestException, NotFoundException, UnauthorizedException {

        Session session = Root.zebedee.sessions.get(request);
        if (session == null || !Root.zebedee.permissions.canEdit(session.email)) {
            throw new UnauthorizedException("You are not authorised to edit content.");
        }

        Collection collection = Collections.getCollection(request);
        String uri = request.getParameter("uri");

        collection.deleteVersion(uri);
        return true;
    }
}
