package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ApprovalStatus;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.publishing.PostPublisher;
import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.github.onsdigital.zebedee.persistence.model.CollectionHistoryEvent;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.commons.lang3.BooleanUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.IOException;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_MANUAL_PUBLISHED_FAILURE;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_MANUAL_PUBLISHED_SUCCESS;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_MANUAL_PUBLISHED_TRIGGERED;
import static com.github.onsdigital.zebedee.persistence.dao.CollectionHistoryDaoFactory.getCollectionHistoryDao;

@Api
public class Publish {

    private final ZebedeeCmsService zebedeeCmsService;

    /**
     * Default constructor used instantiates dependencies itself.
     */
    public Publish() {

        zebedeeCmsService = ZebedeeCmsService.getInstance();
    }

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
        Session session = zebedeeCmsService.getSession(request);

        getCollectionHistoryDao().saveCollectionHistoryEvent(collection, session, COLLECTION_MANUAL_PUBLISHED_TRIGGERED);

        String breakBeforePublish = request.getParameter("breakbeforefiletransfer");
        String skipVerification = request.getParameter("skipVerification");

        boolean doBreakBeforeFileTransfer = BooleanUtils.toBoolean(breakBeforePublish);
        boolean doSkipVerification = BooleanUtils.toBoolean(skipVerification);

        try {
            boolean result = publish(collection, session, doBreakBeforeFileTransfer,
                    doSkipVerification);
            logPublishResult(request, collection, session, result, null);
            return result;
        } catch (ZebedeeException | IOException ex) {
            logPublishResult(request, collection, session, false, ex);
            throw ex;
        }
    }

    /**
     * Manual Publish the files in a collection
     *
     * @param collection       the collection to publish
     * @param session          a session with editor priviledges
     * @param skipVerification
     * @return success
     * @throws IOException
     * @throws UnauthorizedException
     * @throws BadRequestException
     * @throws ConflictException     - If there
     */
    public boolean publish(Collection collection,
                           Session session,
                           boolean breakBeforePublish,
                           boolean skipVerification)
            throws IOException, UnauthorizedException, BadRequestException,
            ConflictException, NotFoundException {

        // Collection exists
        if (collection == null) {
            throw new BadRequestException("Please provide a valid collection.");
        }

        // User has permission
        if (session == null || !zebedeeCmsService.getPermissions().canEdit(session.getEmail())) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // Check approval status
        if (collection.description.approvalStatus != ApprovalStatus.COMPLETE) {
            throw new ConflictException("This collection cannot be published because it is not approved");
        }

        // Break before transfer allows us to run tests on the prepublish-hook without messing up the content
        if (breakBeforePublish) {
            logInfo("Breaking before publish").log();
            return true;
        }
        logInfo("Going ahead with publish").log();

        Keyring keyring = zebedeeCmsService.getZebedee().getKeyringCache().get(session);
        if (keyring == null) throw new UnauthorizedException("No keyring is available for " + session.getEmail());

        ZebedeeCollectionReader collectionReader = new ZebedeeCollectionReader(zebedeeCmsService.getZebedee(), collection, session);
        long publishStart = System.currentTimeMillis();
        boolean publishComplete = zebedeeCmsService.getPublisher().DoFullPublish(collection, session.getEmail(), collectionReader);

        if (publishComplete) {
            long onPublishCompleteStart = System.currentTimeMillis();

            new PublishNotification(collection).sendNotification(EventType.PUBLISHED);

            PostPublisher.postPublish(zebedeeCmsService.getZebedee(), collection, skipVerification, collectionReader);

            logInfo("Collection postPublish process finished")
                    .collectionName(collection)
                    .timeTaken((System.currentTimeMillis() - onPublishCompleteStart))
                    .log();
            logInfo("Collection publish complete.")
                    .collectionName(collection)
                    .timeTaken((System.currentTimeMillis() - publishStart))
                    .log();
        }

        return publishComplete;
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
