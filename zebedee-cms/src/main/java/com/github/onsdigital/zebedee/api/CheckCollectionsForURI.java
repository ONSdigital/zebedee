package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.util.Optional;

@Api
public class CheckCollectionsForURI {
    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();

    /**
     * Checks whether a URI is already in a collection
     *
     * @param request This should contain a X-Florence-Token header for the current session
     * @param response 200 if the URI is being edited, othewise a 204
     * @return The collection name if the URI is being edited, otherwise an empty string
     * @throws IOException If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     * @throws UnauthorizedException If the user does not have editor/admin permission.
     */
    @GET
    public String get(HttpServletRequest request, HttpServletResponse response)
            throws IOException, UnauthorizedException {

        Session session = Root.zebedee.getSessionsService().get(request);
        if (session == null || !zebedeeCmsService.getPermissions().canEdit(session.getEmail())) {
            throw new UnauthorizedException("You are not authorised to check collections for a URI");
        }

        String uri = request.getParameter("uri");

        Optional<com.github.onsdigital.zebedee.model.Collection> collection = Root.zebedee.checkForCollectionBlockingChange(uri);
        if(!collection.isPresent()) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return "";
        }

        return collection.get().getDescription().getName();
    }

}
