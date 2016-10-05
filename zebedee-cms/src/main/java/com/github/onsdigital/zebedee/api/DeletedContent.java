package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.persistence.model.DeletedContentEvent;
import com.github.onsdigital.zebedee.util.URIUtils;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Api
public class DeletedContent {

    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();

    @GET
    public List<DeletedContentEvent> getDeletedContent(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Session session = zebedeeCmsService.getSession(request);

        if (!zebedeeCmsService.getPermissions().isPublisher(session) && !zebedeeCmsService.getPermissions().isAdministrator(session)) {
            throw new UnauthorizedException("You must be a publisher or admin to view deleted content.");
        }

        List<DeletedContentEvent> deletedContentEvents = new ArrayList<>();

        DeletedContentEvent deletedContentEvent = new DeletedContentEvent("collection1", "collectionName1", new Date(), "admin@whatever.com", "/about", "About us");
        deletedContentEvent.setId(1);
        deletedContentEvents.add(deletedContentEvent);

        DeletedContentEvent deletedContentEvent1 = new DeletedContentEvent("collection2", "collectionName2", new Date(), "admin@whatever.com", "/economy/someawsomepage", "The page of awesome");
        deletedContentEvent1.setId(2);
        deletedContentEvents.add(deletedContentEvent1);

        return deletedContentEvents;
    }

    @POST
    public String restoreDeletedContent(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException, IOException {

        Session session = zebedeeCmsService.getSession(request);
        if (!zebedeeCmsService.getPermissions().isPublisher(session) && !zebedeeCmsService.getPermissions().isAdministrator(session)) {
            throw new UnauthorizedException("You must be a publisher or admin to restore deleted content.");
        }

        long deletedContentId;
        try {
            deletedContentId = Long.parseLong(URIUtils.getLastSegment(request.getRequestURI()));
        } catch (NumberFormatException e) {
            throw new BadRequestException("Failed to parse deleted content id from the request URL.");
        }

        String collectionid = request.getParameter("collectionid");
        if (StringUtils.isEmpty(collectionid)) {
            throw new BadRequestException("Failed to parse the collection id from the query parameters.");
        }
        com.github.onsdigital.zebedee.model.Collection collection = zebedeeCmsService.getCollection(collectionid);

        return "Restored deleted content with ID " + deletedContentId + " to collection " + collection.getDescription().name;
    }
}
