package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;
import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_MANUAL_PUBLISHED_FAILURE;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_MANUAL_PUBLISHED_SUCCESS;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_MANUAL_PUBLISHED_TRIGGERED;
import static com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory.getCollectionHistoryDao;

@Api
public class Publish {

    /**
     * @param request  the file request
     * @param response <ul>
     *                 <li>If publish succeeds: {@link HttpStatus#OK_200}</li>
     *                 <li>If credentials are not provided:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                 <li>If authentication fails:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                 <li>If the collection doesn't exist:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                 <li>If the collection is not approved:  {@link HttpStatus#CONFLICT_409}</li>
     *                 </ul>
     * @return success value
     * @throws IOException
     * @throws NotFoundException
     * @throws BadRequestException
     * @throws UnauthorizedException
     * @throws ConflictException
     */
    @POST
    public boolean publish(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ZebedeeException {

        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        Session session = Root.zebedee.getSessionsService().get(request);

        getCollectionHistoryDao().saveCollectionHistoryEvent(collection, session, COLLECTION_MANUAL_PUBLISHED_TRIGGERED);

        String breakBeforePublish = request.getParameter("breakbeforefiletransfer");
        String skipVerification = request.getParameter("skipVerification");

        boolean doBreakBeforeFileTransfer = BooleanUtils.toBoolean(breakBeforePublish);
        boolean doSkipVerification = BooleanUtils.toBoolean(skipVerification);

        try {
            boolean result = Root.zebedee.getCollections().publish(collection, session, doBreakBeforeFileTransfer,
                    doSkipVerification);
            logPublishResult(request, collection, session, result, null);
            return result;
        } catch (ZebedeeException | IOException ex) {
            logPublishResult(request, collection, session, false, ex);
            throw ex;
        }
    }

    private void logPublishResult(HttpServletRequest request, com.github.onsdigital.zebedee.model.Collection collection,
                                  Session session, boolean result, Exception ex) {
        Audit.Event auditEvent;
        CollectionHistoryEvent historyEvent;

        if (result) {
            auditEvent = Audit.Event.COLLECTION_PUBLISHED;
            historyEvent = new CollectionHistoryEvent(collection, session, COLLECTION_MANUAL_PUBLISHED_SUCCESS);
        } else {
            auditEvent = Audit.Event.COLLECTION_PUBLISH_UNSUCCESSFUL;
            historyEvent = new CollectionHistoryEvent(collection, session, COLLECTION_MANUAL_PUBLISHED_FAILURE);
        }

        if (ex != null) {
            historyEvent.exceptionText(ex.getMessage());
        }

        auditEvent.parameters().host(request).collection(collection).actionedBy(session.getEmail()).log();
        getCollectionHistoryDao().saveCollectionHistoryEvent(historyEvent);
    }
}
