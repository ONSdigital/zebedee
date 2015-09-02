package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.Session;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;

@Api
public class Collection {

    /**
     * Retrieves a CollectionDescription object at the endpoint /Collection/[CollectionName]
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If no collection exists:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                 </ul>
     * @return the CollectionDescription.
     * @throws IOException
     */
    @GET
    public CollectionDescription get(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        com.github.onsdigital.zebedee.model.Collection collection = Collections
                .getCollection(request);

        // Check whether we found the collection:
        if (collection == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return null;
        }

        // Check whether we have access
        Session session = Root.zebedee.sessions.get(request);
        if (Root.zebedee.permissions.canView(session.email, collection.description) == false) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return null;
        }

        // Collate the result:
        CollectionDescription result = new CollectionDescription();
        result.id = collection.description.id;
        result.name = collection.description.name;
        result.publishDate = collection.description.publishDate;
        result.inProgressUris = collection.inProgressUris();
        result.completeUris = collection.completeUris();
        result.reviewedUris = collection.reviewedUris();
        result.eventsByUri = collection.description.eventsByUri;
        result.approvedStatus = collection.description.approvedStatus;
        result.type = collection.description.type;
        return result;
    }

    /**
     * Creates or updates collection details the endpoint /Collection/
     * <p/>
     * Checks if a collection exists using {@link CollectionDescription#name}
     *
     * @param request               This should contain a X-Florence-Token header for the current session
     * @param response              <ul>
     *                              <li>If no name has been passed:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                              <li>If user cannot create collections:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                              <li>If collection with name already exists:  {@link HttpStatus#CONFLICT_409}</li>
     *                              </ul>
     * @param collectionDescription
     * @return
     * @throws IOException
     */
    @POST
    public CollectionDescription create(HttpServletRequest request,
                                        HttpServletResponse response,
                                        CollectionDescription collectionDescription) throws IOException {

        if (StringUtils.isBlank(collectionDescription.name)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return null;
        }

        Session session = Root.zebedee.sessions.get(request);
        if (Root.zebedee.permissions.canEdit(session.email) == false) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return null;
        }

        collectionDescription.name = StringUtils.trim(collectionDescription.name);
        if (Root.zebedee.collections.list().hasCollection(
                collectionDescription.name)) {
            response.setStatus(HttpStatus.CONFLICT_409);
            return null;
        }

        com.github.onsdigital.zebedee.model.Collection collection = com.github.onsdigital.zebedee.model.Collection.create(
                collectionDescription, Root.zebedee, session.email);

        Root.schedulePublish(collection);

        return collection.description;
    }

    /**
     * Deletes the collection details at the endpoint /Collection/[CollectionName]
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If the collection doesn't exist:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                 <li>If user is not authorised to delete this collection:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                 <li>If the collection has contents preventing deletion:  {@link HttpStatus#CONFLICT_409}</li>
     *                 </ul>
     * @return
     * @throws IOException
     */
    @DELETE
    public boolean deleteCollection(HttpServletRequest request, HttpServletResponse response) throws IOException, UnauthorizedException, BadRequestException, NotFoundException {

        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        Session session = Root.zebedee.sessions.get(request);

        Root.zebedee.collections.delete(collection, session);

        Root.cancelPublish(collection);

        return true;
    }
}
