package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDeleteMarker;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import com.github.onsdigital.zebedee.service.ContentDeleteService;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 * Mark content to be deleted.
 */
@Api
public class DeleteContent {

    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();
    private static final String JSON_FILE_EXT = ".json";
    private static ContentDeleteService deleteService = ContentDeleteService.getInstance();

    @POST
    public void createDeleteMarker(HttpServletRequest request, HttpServletResponse response,
                                   ContentDeleteMarker deleteMarker) throws IOException, ZebedeeException {
        if (deleteMarker == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        com.github.onsdigital.zebedee.model.Collection collection = zebedeeCmsService.getCollection(
                deleteMarker.getCollectionId());
        Session session = zebedeeCmsService.getSession(request);

        if (!validCollectionAndPermissions(response, session, collection)) {
            return;
        }

        deleteService.addDeleteMarkerToCollection(collection, deleteMarker);

        // TODO AUDIT and Collection History event logging
        logDebug("Content marked for delete").addParameter("target", deleteMarker).log();
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @GET
    public void getDeleteMarkers(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ZebedeeException {
        // TODO take param for scope i.e. collection or all.
        com.github.onsdigital.zebedee.model.Collection collection = zebedeeCmsService.getCollection(request);
        Session session = zebedeeCmsService.getSession(request);

        if (!validCollectionAndPermissions(response, session, collection)) {
            return;
        }

        InputStream inputStream = zebedeeCmsService.objectAsInputStream(
                deleteService.getDeleteItemsByCollection(collection, session));
        IOUtils.copy(inputStream, response.getOutputStream());
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
    public void removeDeleteMarker(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ZebedeeException {
        com.github.onsdigital.zebedee.model.Collection collection = zebedeeCmsService.getCollection(request);
        Session session = zebedeeCmsService.getSession(request);

        if (!validCollectionAndPermissions(response, session, collection)) {
            return;
        }

        Optional<String> contentUri = RequestUtils.getURIParameter(request);
        if (!contentUri.isPresent()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (deleteService.removeMarker(collection, contentUri.get())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Check the collection is valid and the user has the required permission for this action.
     *
     * @param response
     * @param session
     * @param collection
     * @return
     * @throws IOException
     */
    private boolean validCollectionAndPermissions(HttpServletResponse response, Session session,
                                                  com.github.onsdigital.zebedee.model.Collection collection) throws IOException {
        if (collection == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }
        if (zebedeeCmsService.getPermissions().canView(session.email, collection.description)) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    private Path manifestPath(com.github.onsdigital.zebedee.model.Collection collection) {
        return Paths.get(collection.path.toString() + JSON_FILE_EXT);
    }
}
