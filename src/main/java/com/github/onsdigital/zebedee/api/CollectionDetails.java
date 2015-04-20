package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.CollectionDetail;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

@Api
public class CollectionDetails {

    /**
     * Retrieves a CollectionDetail object at the endpoint /CollectionDetails/[CollectionName]
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If no collection exists:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                 </ul>
     * @return the CollectionDetail.
     * @throws IOException
     */
    @GET
    public CollectionDetail get(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        com.github.onsdigital.zebedee.model.Collection collection = Collections
                .getCollection(request);

        if (collection == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return null;
        }

        CollectionDetail result = new CollectionDetail();
        result.id = collection.description.id;
        result.name = collection.description.name;
        result.publishDate = collection.description.publishDate;
        result.inProgress = collection.inProgress.details();
        result.complete = collection.complete.details();
        result.reviewed = collection.reviewed.details();
        result.approvedStatus = collection.description.approvedStatus;
        return result;
    }
}
