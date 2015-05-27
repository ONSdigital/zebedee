package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.CollectionDetail;
import com.github.onsdigital.zebedee.json.ContentDetail;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

@Api
public class CollectionBrowseTree {

    /**
     * Retrieves a CollectionBrowseTree object at the endpoint /CollectionBrowseTree/[CollectionName]
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If no collection exists:  {@link org.eclipse.jetty.http.HttpStatus#NOT_FOUND_404}</li>
     *                 </ul>
     * @return the CollectionBrowseTree.
     * @throws java.io.IOException
     */
    @GET
    public ContentDetail get(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

//        com.github.onsdigital.zebedee.model.Collection collection = Collections
//                .getCollection(request);
//
//        if (collection == null) {
//            response.setStatus(HttpStatus.NOT_FOUND_404);
//            return null;
//        }

        return Root.zebedee.published.nestedDetails();
    }
}
