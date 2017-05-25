package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.DeleteMarkerJson;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.json.response.DeleteContentResponse;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import com.github.onsdigital.zebedee.service.ContentDeleteService;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static com.github.onsdigital.zebedee.model.DeleteMarker.jsonToMarker;

// TODO 1 THIS NEEDS AUDIT LOGGING AND COLLECTION HISTORY LOGGING.
// TODO 2 check type of content being deleted - dont let them delete the homepage for example.

/**
 * Mark content to be deleted.
 */
@Api
public class DeleteContent {

    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();
    private static ContentDeleteService deleteService = ContentDeleteService.getInstance();

    @POST
    public DeleteContentResponse createDeleteMarker(HttpServletRequest request, HttpServletResponse response,
                                                    DeleteMarkerJson deleteMarkerJson) throws IOException,
            ZebedeeException {

        if (deleteMarkerJson == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return new DeleteContentResponse(HttpStatus.SC_BAD_REQUEST);
        }
        com.github.onsdigital.zebedee.model.Collection collection = zebedeeCmsService.getCollection(
                deleteMarkerJson.getCollectionId());
        Session session = zebedeeCmsService.getSession(request);

        if (!zebedeeCmsService.getPermissions().canView(session.getEmail(), collection.description)) {
            return new DeleteContentResponse(HttpStatus.SC_UNAUTHORIZED);
        }

        deleteService.addDeleteMarkerToCollection(session, collection, jsonToMarker(deleteMarkerJson));
        return new DeleteContentResponse(HttpStatus.SC_CREATED);
    }

    @GET
    public DeleteContentResponse getDeleteMarkers(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ZebedeeException {

        // TODO if collection ID scope = collection. Otherwise all delete items.
        com.github.onsdigital.zebedee.model.Collection collection = zebedeeCmsService.getCollection(request);
        Session session = zebedeeCmsService.getSession(request);

        if (zebedeeCmsService.getPermissions().canView(session.getEmail(), collection.description)) {
            return new DeleteContentResponse(HttpStatus.SC_UNAUTHORIZED);
        }

        InputStream inputStream = zebedeeCmsService.objectAsInputStream(
                deleteService.getDeleteItemsByCollection(collection));
        IOUtils.copy(inputStream, response.getOutputStream());

        return new DeleteContentResponse(HttpStatus.SC_OK);
    }

    /**
     * Remove a delete marker from the requested resource.
     *
     * @param request
     * @param response
     * @throws IOException
     * @throws ZebedeeException
     */
    @DELETE
    public DeleteContentResponse removeDeleteMarker(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ZebedeeException {
        com.github.onsdigital.zebedee.model.Collection collection = zebedeeCmsService.getCollection(request);
        Session session = zebedeeCmsService.getSession(request);

        if (!zebedeeCmsService.getPermissions().canView(session.getEmail(), collection.description)) {
            return new DeleteContentResponse(HttpStatus.SC_UNAUTHORIZED);
        }

        Optional<String> contentUri = RequestUtils.getURIParameter(request);
        if (!contentUri.isPresent()) {
            return new DeleteContentResponse(HttpStatus.SC_BAD_REQUEST);
        }

        deleteService.cancelPendingDelete(collection, session, contentUri.get());
        return new DeleteContentResponse(HttpStatus.SC_OK);
    }
}
