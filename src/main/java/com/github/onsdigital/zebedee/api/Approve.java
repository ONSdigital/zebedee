package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.ResultMessage;
import com.github.onsdigital.zebedee.json.Session;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

/**
 * Created by david on 10/03/2015.
 */
@Api
public class Approve {

    @POST
    public ResultMessage approve(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Locate the collection:
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        if (collection == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return new ResultMessage("Collection not found.");
        }

        // Locate the path:
        String uri = request.getParameter("uri");

        java.nio.file.Path path = collection.getInProgressPath(uri);
        if (path == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return new ResultMessage("URI not in progress.");
        }

        // Check we're requesting a file:
        if (java.nio.file.Files.isDirectory(path)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return new ResultMessage("URI does not represent a file.");
        }

        // Attempt to approve:
        Session session = Root.zebedee.sessions.get(request);
        if (!collection.approve(session.email, uri)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return new ResultMessage("URI was not approved.");
        }

        return new ResultMessage("URI approved.");
    }

}
