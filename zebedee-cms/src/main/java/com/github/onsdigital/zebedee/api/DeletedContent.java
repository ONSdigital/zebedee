package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter;
import com.github.onsdigital.zebedee.model.content.deleted.DeletedContentEvent;
import com.github.onsdigital.zebedee.service.DeletedContent.DeletedContentService;
import com.github.onsdigital.zebedee.service.DeletedContent.DeletedContentServiceFactory;
import com.github.onsdigital.zebedee.util.URIUtils;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;
import java.util.List;


@Api
public class DeletedContent {

    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();
    private static DeletedContentService deletedContentService = DeletedContentServiceFactory.createInstance();

    /**
     * Get a list of recently deleted content.
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    @GET
    public List<DeletedContentEvent> listDeletedContent(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Session session = getSession(request, "You must be a publisher or admin to view deleted content.");
        List<DeletedContentEvent> deletedContentEvents = deletedContentService.listDeletedContent();

        return deletedContentEvents;
    }

    /**
     * Restore previously deleted content into a collection.
     * @param request
     * @param response
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    @POST
    public String restoreDeletedContent(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException, IOException {

        Session session = getSession(request, "You must be a publisher or admin to restore deleted content.");

        long deletedContentId = getDeletedContentId(request);
        com.github.onsdigital.zebedee.model.Collection collection = getCollection(request);
        CollectionWriter collectionWriter = new ZebedeeCollectionWriter(zebedeeCmsService.getZebedee(), collection, session);

        deletedContentService.retrieveDeletedContent(deletedContentId, collectionWriter.getInProgress());

        return "Restored deleted content with ID " + deletedContentId + " to collection "
                + collection.getDescription().getName();
    }

    private Session getSession(HttpServletRequest request, String message) throws ZebedeeException, IOException {
        Session session = zebedeeCmsService.getSession(request);
        if (!zebedeeCmsService.getPermissions().isPublisher(session) && !zebedeeCmsService.getPermissions().isAdministrator(session)) {
            throw new UnauthorizedException(message);
        }
        return session;
    }

    private Collection getCollection(HttpServletRequest request) throws ZebedeeException {
        String collectionid = request.getParameter("collectionid");
        if (StringUtils.isEmpty(collectionid)) {
            throw new BadRequestException("Failed to parse the collection id from the query parameters.");
        }
        return zebedeeCmsService.getCollection(collectionid);
    }

    private long getDeletedContentId(HttpServletRequest request) throws BadRequestException {
        long deletedContentId;
        try {
            deletedContentId = Long.parseLong(URIUtils.getLastSegment(request.getRequestURI()));
        } catch (NumberFormatException e) {
            throw new BadRequestException("Failed to parse deleted content id from the request URL.");
        }
        return deletedContentId;
    }
}
