package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDetail;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

@Api
public class PublishingQueueCollectionDetails {

    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();

    /**
     * Retrieves a CollectionDetail object at the endpoint /PublishingQueueCollectionDetails/[CollectionName]
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
            throws IOException, ZebedeeException {

        Collection collection = Collections
                .getCollection(request);

        if (collection == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return null;
        }

        Session session = zebedeeCmsService.getSession(request);
        if (!zebedeeCmsService.getPermissions().canView(session.getEmail(), collection.getDescription())) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return null;
        }

        CollectionDetail result = new CollectionDetail();

        result.setId(collection.getDescription().getId());
        result.setName(collection.getDescription().getName());
        result.setType(collection.getDescription().getType());
        result.setPublishDate(collection.getDescription().getPublishDate());
        result.setReleaseUri(collection.getDescription().getReleaseUri());

        result.approvalStatus = collection.getDescription().approvalStatus;

        return result;
    }
}
