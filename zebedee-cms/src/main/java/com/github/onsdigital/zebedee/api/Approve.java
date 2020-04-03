package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;

/**
 * Created by kanemorgan on 01/04/2015.
 */
@Api
public class Approve {

    static final String OVERRIDE_KEY_PARAM = "overrideKey";

    /**
     * Approves the content of a collection at the endpoint /Approve/[CollectionName].
     *
     * @param request
     * @param response <ul>
     *                 <li>If approval succeeds: {@link HttpStatus#OK_200}</li>
     *                 <li>If credentials are not provided:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                 <li>If authentication fails:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                 <li>If the collection doesn't exist:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                 <li>If the collection has incomplete items:  {@link HttpStatus#CONFLICT_409}</li>
     *                 </ul>
     * @return Save successful status of the description.
     * @throws IOException
     */
    @POST
    public boolean approveCollection(HttpServletRequest request, HttpServletResponse response) throws IOException, ZebedeeException {
        info().log("approve endpoint: request received");

        Session session = Root.zebedee.getSessions().get(request);
        if (session == null) {
            warn().log("approve request: request unsuccessful valid user session not found");
            throw new UnauthorizedException("You are not authorized to approve collections");
        }

        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        if (collection == null) {
            warn().log("approve request: request unsuccessful valid user session not found");
            throw new NotFoundException("The collection you are trying to approve was not found.");
        }

        String collectionId = Collections.getCollectionId(request);
        info().data("collectionId", collectionId).data("user", session.getEmail()).log("approve endpoint: submitting approve request");

        Long userOverrideKey = getOverrideKey(request);

        try {
            Root.zebedee.getCollections().approve(collection, session, userOverrideKey);
        } catch (Exception e) {
            error().data("collectionId", collectionId).data("user", session.getEmail()).log("approve endpoint: request unsuccessful error while approving collection");
            throw e;
        }

        Audit.Event.COLLECTION_APPROVED
                .parameters()
                .host(request)
                .collection(collection)
                .actionedBy(session.getEmail())
                .log();

        info().data("user", session.getEmail()).log("approve endpoint: request completed successfully");
        return true;
    }

    /**
     * Get the "overrideKey" request parameter as a {@link Long}.
     *
     * @param request the {@link HttpServletRequest} to get parameter from.
     * @return the value as a {@link Long} if present otherwise return null if value is null/empty or an invalid
     * numeric value.
     */
    Long getOverrideKey(HttpServletRequest request) {
        String overrideKey = request.getParameter(OVERRIDE_KEY_PARAM);
        if (StringUtils.isEmpty(overrideKey)) {
            return null;
        }

        Long key = null;
        try {
            key = Long.valueOf(overrideKey);
        } catch (NumberFormatException ex) {
            info().exception(ex).log("failed to convert override parameter to long");
        }
        return key;
    }
}
