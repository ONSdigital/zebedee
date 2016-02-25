package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
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
    public ResultMessage review(HttpServletRequest request, HttpServletResponse response) throws IOException, BadRequestException, UnauthorizedException, NotFoundException {

        // Collate parameters
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        if(collection == null) {
            throw new NotFoundException("Collection not found");
        }

        Session session = Root.zebedee.sessions.get(request);
        String uri = request.getParameter("uri");

        // Run the review
        collection.review(session, uri);
        collection.save();

        Audit.log(request, "Collection %s reviewed by %s", collection.path, session.email);

        return new ResultMessage("URI reviewed.");
    }

}
