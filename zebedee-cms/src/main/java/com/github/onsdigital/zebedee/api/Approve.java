package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;

/**
 * Created by kanemorgan on 01/04/2015.
 */
@Api
public class Approve {

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
        logInfo("approve endpoint: request received").log();

        Session session = Root.zebedee.getSessionsService().get(request);
        if (session == null) {
            logWarn("approve request: request unsuccessful valid user session not found").log();
            throw new UnauthorizedException("You are not authorized to approve collections");
        }

        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        if (collection == null) {
            logWarn("approve endpoint: request unsuccessful collection not found")
                    .log();
        }

        logInfo("approve endpoint: submitting approve request")
                .collectionId(collection)
                .user(session.getEmail())
                .log();
        try {
            Root.zebedee.getCollections().approve(collection, session);
        } catch (Exception e) {
            logError(e, "approve endpoint: request unsuccessful error while approving collection")
                    .collectionId(collection)
                    .user(session)
                    .log();
            throw e;
        }

        Audit.Event.COLLECTION_APPROVED
                .parameters()
                .host(request)
                .collection(collection)
                .actionedBy(session.getEmail())
                .log();

        logInfo("approve endpoint: request completed successfully").collectionId(collection).user(session.getEmail()).log();
        return true;
    }
}
