package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;
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
    public boolean MoveContent(HttpServletRequest request, HttpServletResponse response) throws IOException, ZebedeeException {

        Session session = Root.zebedee.getSessionsService().get(request);
        Collection collection = Collections.getCollection(request);

        String uri = request.getParameter("uri");
        String toUri = request.getParameter("toUri");

        Root.zebedee.getCollections().moveContent(session, collection, uri, toUri);

        Audit.Event.CONTENT_MOVED
                .parameters()
                .host(request)
                .collection(collection)
                .fromTo(uri, toUri)
                .actionedBy(session.getEmail())
                .log();
        return true;
    }
}
