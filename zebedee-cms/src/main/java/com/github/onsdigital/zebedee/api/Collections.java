package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.onsdigital.zebedee.dataset.api.DatasetAPIClient;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionDescriptions;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionOwner;
import com.github.onsdigital.zebedee.service.DatasetService;
import com.github.onsdigital.zebedee.service.ZebedeeDatasetService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

@Api
public class Collections {

    private ZebedeeCmsService zebedeeCmsService;
    private DatasetService datasetService;

    /**
     * Default constructor used instantiates dependencies itself.
     */
    public Collections() {
        zebedeeCmsService = ZebedeeCmsService.getInstance();
        datasetService = new ZebedeeDatasetService(
                DatasetAPIClient.getInstance(),
                zebedeeCmsService.getInstance());
    }

    /**
     * Constructor allowing dependencies to be injected.
     *
     * @param zebedeeCmsService
     * @param datasetService
     */
    public Collections(ZebedeeCmsService zebedeeCmsService, DatasetService datasetService) {
        this.zebedeeCmsService = zebedeeCmsService;
        this.datasetService = datasetService;
    }

    /**
     * Retrieves current {@link CollectionDescription} objects
     *
     * @param request
     * @param response
     * @return a List of {@link Collection#description}.
     * @throws IOException
     */
    @GET
    public CollectionDescriptions get(HttpServletRequest request, HttpServletResponse response)
            throws ZebedeeException {
        try {
            Session session = Root.zebedee.getSessionsService().get(request);
            CollectionDescriptions result = new CollectionDescriptions();
            List<Collection> collections = Root.zebedee.getCollections().list();
            CollectionOwner collectionOwner = zebedeeCmsService.getPublisherType(session.getEmail());

            for (Collection collection : collections) {
                if (Root.zebedee.getPermissionsService().canView(session, collection.description)
                        && (collection.description.collectionOwner.equals(collectionOwner))) {

                    CollectionDescription description = new CollectionDescription();
                    description.id = collection.description.id;
                    description.name = collection.description.name;
                    description.publishDate = collection.description.publishDate;
                    description.approvalStatus = collection.description.approvalStatus;
                    description.type = collection.description.type;
                    description.teams = collection.description.teams;
                    result.add(description);
                }
            }

            // sort the collections alphabetically by name.
            java.util.Collections.sort(result, Comparator.comparing(o -> o.name));

            return result;
        } catch (IOException e) {
            logError(e, "Unexpected error while attempting to get collections")
                    .logAndThrow(UnexpectedErrorException.class);
        }
        return null;
    }

    /**
     * Put endpoints for /collections.
     * This supports only /collections/{collection_id}/instances/{instance_id}
     */
    @PUT
    public void put(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException, IOException {

        Session session = zebedeeCmsService.getSession(request);
        if (!zebedeeCmsService.getPermissions().canEdit(session)) {
            logInfo("Forbidden request made to the collection endpoint").log();
            response.setStatus(HttpStatus.SC_FORBIDDEN);
            return;
        }

        Path path = Path.newInstance(request);
        List<String> segments = path.segments();

        // /collections/{collection_id}/instances/{instance_id}
        if (segments.size() < 4 ||
                !segments.get(2).equalsIgnoreCase("instances")) {

            logInfo("Endpoint for colletions not found").addParameter("path", path).log();
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            return;
        }

        String collectionID = segments.get(1);
        String instanceID = segments.get(3);

        logInfo("PUT called on /collections/{collection_id}/instances/{instance_id} endpoint")
                .addParameter("collectionID", collectionID)
                .addParameter("instanceID", instanceID)
                .log();

        datasetService.addInstanceToCollection(collectionID, instanceID);
    }

    /**
     * Delete endpoints for /collections.
     * This supports only /collections/{collection_id}/instances/{instance_id}
     */
    @DELETE
    public void delete(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException, IOException {

        Session session = zebedeeCmsService.getSession(request);
        if (!zebedeeCmsService.getPermissions().canEdit(session)) {
            logInfo("Forbidden request made to the collection endpoint").log();
            response.setStatus(HttpStatus.SC_FORBIDDEN);
            return;
        }

        Path path = Path.newInstance(request);
        List<String> segments = path.segments();

        // /collections/{collection_id}/instances/{instance_id}
        if (segments.size() < 4 ||
                !segments.get(2).equalsIgnoreCase("instances")) {

            logInfo("Endpoint for colletions not found").addParameter("path", path).log();
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            return;
        }

        String collectionID = segments.get(1);
        String instanceID = segments.get(3);

        logInfo("DELETE called on /collections/{collection_id}/instances/{instance_id} endpoint")
                .addParameter("collectionID", collectionID)
                .addParameter("instanceID", instanceID)
                .log();

        datasetService.deleteInstanceFromCollection(collectionID, instanceID);
    }

    /**
     * Get the collection defined by the given HttpServletRequest
     *
     * @param request the request containing the id of the collection to get.
     * @return
     * @throws IOException
     */
    public static Collection getCollection(HttpServletRequest request)
            throws IOException {
        String collectionId = getCollectionId(request);
        return Root.zebedee.getCollections().getCollection(collectionId);
    }

    public static String getCollectionId(HttpServletRequest request) {
        Path path = Path.newInstance(request);
        List<String> segments = path.segments();

        String collectionId = "";
        if (segments.size() > 1) {
            collectionId = segments.get(1);
        }
        return collectionId;
    }

}
