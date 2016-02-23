package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

@Api
public class ContentMove {
    /**
     * Move or rename content in a collection.
     * <p>
     * This operation could technically be done using a separate delete and post method for content, but this method
     * does not involve re-uploading the content so is more efficient in terms of network IO
     */
    @POST
    public boolean MoveContent(HttpServletRequest request, HttpServletResponse response) throws IOException, BadRequestException, UnauthorizedException {

        Session session = Root.zebedee.sessions.get(request);
        Collection collection = Collections.getCollection(request);

        String uri = request.getParameter("uri");
        String toUri = request.getParameter("toUri");

        Root.zebedee.collections.moveContent(session, collection, uri, toUri);

        Audit.log(request, "Collection %s content %s moved to %s by %s", collection.path, uri, toUri, session.email);

        return true;
    }
}
