package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.ResultMessage;
import com.github.onsdigital.zebedee.json.Session;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

@Api
public class Review {

    /**
     * Moves files between collections using the endpoint <code>/Review/[CollectionName]?uri=[uri]</code>
     *
     * @param request This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If the collection does not exist:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                 <li>If the content item does not exist:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                 <li>If the uri specified a folder not a file:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                 <li>If user not authorised to review:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                 <li>The review fails for some other reason:  {@link HttpStatus#BAD_REQUEST_400}</li>
     * @return a success status wrapped in a {@link ResultMessage} object
     * @throws IOException
     */
    @POST
    public ResultMessage review(HttpServletRequest request, HttpServletResponse response) throws IOException, BadRequestException, UnauthorizedException {

        // Locate the collection:
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        if (collection == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return new ResultMessage("Collection not found.");
        }

        Session session = Root.zebedee.sessions.get(request);
        if(Root.zebedee.permissions.canEdit(session.email) == false) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return new ResultMessage("Review unauthorized.");
        }

        // Check the uri exists in this collection
        String uri = request.getParameter("uri");
        if(collection.isInCollection(uri) == false) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return new ResultMessage("URI is not complete.");
        }

        // Check we're requesting a file:
        if (java.nio.file.Files.isDirectory(collection.complete.path.resolve(uri))) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return new ResultMessage("URI does not represent a file.");
        }

        // Attempt to review:
        if (!collection.review(session.email, uri)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return new ResultMessage("URI was not reviewed.");
        }

        collection.save();
        return new ResultMessage("URI reviewed.");
    }

}
