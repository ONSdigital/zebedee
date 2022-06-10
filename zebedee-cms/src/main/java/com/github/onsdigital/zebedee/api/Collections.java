package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDatasetVersion;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionDescriptions;
import com.github.onsdigital.zebedee.json.CollectionInteractive;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.service.DatasetService;
import com.github.onsdigital.zebedee.service.InteractivesService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import com.google.gson.JsonSyntaxException;
import dp.api.dataset.exception.DatasetAPIException;
import dp.api.dataset.exception.DatasetNotFoundException;
import dp.api.dataset.exception.UnexpectedResponseException;
import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;

@Api
public class Collections {

    private ZebedeeCmsService zebedeeCmsService;
    private DatasetService datasetService;
    private InteractivesService interactivesService;
    private final boolean datasetImportEnabled;
    private static final String GET_COLLECTIONS_ERROR = "get collections endpoint: unexpected error while attempting to get collections";

    /**
     * Default constructor used instantiates dependencies itself.
     */
    public Collections() throws URISyntaxException {
        this.zebedeeCmsService = ZebedeeCmsService.getInstance();
        this.datasetService = zebedeeCmsService.getDatasetService();
        this.interactivesService = zebedeeCmsService.getInteractivesService();
        this.datasetImportEnabled = cmsFeatureFlags().isEnableDatasetImport();
    }

    /**
     * Constructor allowing dependencies to be injected.
     */
    public Collections(ZebedeeCmsService zebedeeCmsService,
                       DatasetService datasetService,
                       InteractivesService interactivesService,
                       boolean datasetImportEnabled) {
        this.zebedeeCmsService = zebedeeCmsService;
        this.datasetService = datasetService;
        this.interactivesService = interactivesService;
        this.datasetImportEnabled = datasetImportEnabled;
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
        info().log("get collections endpoint: request received");
        Session session = null;
        try {
            session = Root.zebedee.getSessions().get();
            if (session == null) {
                warn().log("get collections endpoint: valid user session not found");
                throw new UnauthorizedException("You are not authorized to perform get collections requests");
            }

            CollectionDescriptions result = new CollectionDescriptions();
            List<Collection> collections = Root.zebedee.getCollections().list();

            for (Collection collection : collections) {
                if (Root.zebedee.getPermissionsService().canView(session, collection.getDescription().getId())) {
                    CollectionDescription newDesc = new CollectionDescription();
                    newDesc.setId(collection.getDescription().getId());
                    newDesc.setName(collection.getDescription().getName());
                    newDesc.setPublishDate(collection.getDescription().getPublishDate());
                    newDesc.setApprovalStatus(collection.getDescription().getApprovalStatus());
                    newDesc.setType(collection.getDescription().getType());
                    newDesc.setTeams(collection.getDescription().getTeams());
                    result.add(newDesc);
                }
            }

            // sort the collections alphabetically by name.
            java.util.Collections.sort(result, Comparator.comparing(o -> o.getName()));

            List<String> collectionIds = result.stream().map(c -> c.getId()).collect(Collectors.toList());
            info().data("collections", collectionIds).log("user granted canView permission for collections");

            return result;
        } catch (IOException e) {
            error().data("user", session.getEmail()).logException(e, GET_COLLECTIONS_ERROR);
            throw new UnexpectedErrorException(GET_COLLECTIONS_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Put endpoints for /collections.
     * This supports only /collections/{collection_id}/datasets/{dataset_id}
     */
    @PUT
    public void put(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException, IOException, DatasetAPIException {
        if (!datasetImportEnabled) {
            warn().data("responseStatus", SC_NOT_FOUND)
                    .log("collections PUT endpoint is not supported as feature EnableDatasetImport is disabled");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Session session = zebedeeCmsService.getSession();
        if (session == null || !zebedeeCmsService.getPermissions().canEdit(session)) {
            info().log("Forbidden request made to the collection endpoint");
            response.setStatus(HttpStatus.SC_FORBIDDEN);
            return;
        }

        String user = session.getEmail();

        Path path = Path.newInstance(request);
        List<String> pathSegments = path.segments();

        if (!isValidPath(response, path, pathSegments)) return;

        String collectionID = pathSegments.get(1);
        String resourceID = pathSegments.get(3);

        Collection collection = zebedeeCmsService.getCollection(collectionID);
        if (collection == null) {
            info().data("collectionId", collectionID).log("Collection not found");
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            return;
        }

        try {
            String dType = pathSegments.get(2);
            switch (dType) {
                case "datasets":
                    switch (pathSegments.size()) {
                        case 4: // /collections/{collection_id}/datasets/{dataset_id}

                            updateDatasetInCollection(collection, resourceID, request, user);
                            break;

                        case 8: // /collections/{collection_id}/datasets/{dataset_id}/editions/{}/versions/{}

                            String edition = pathSegments.get(5);
                            String version = pathSegments.get(7);

                            updateDatasetVersionInCollection(collection, resourceID, edition, version, request, user);
                            break;

                        default:
                            response.setStatus(HttpStatus.SC_NOT_FOUND);
                            return;
                    }
                    break;
                case "interactives":
                    switch (pathSegments.size()) {
                        case 4: // /collections/{collection_id}/interactives/{interactive_id}

                            updateInteractiveInCollection(collection, resourceID, request, user);
                            break;

                        default:
                            response.setStatus(HttpStatus.SC_NOT_FOUND);
                            return;
                    }
                    break;
                default:
                    response.setStatus(HttpStatus.SC_NOT_FOUND);
                    return;
            }
        } catch (UnexpectedResponseException e) {
            throw new UnexpectedErrorException(e.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } catch (DatasetNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (dp.api.dataset.exception.BadRequestException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    /**
     * Delete endpoints for /collections.
     * This supports only /collections/{collection_id}/datasets/{dataset_ID}
     */
    @DELETE
    public void delete(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException, IOException, DatasetAPIException {
        if (!datasetImportEnabled) {
            warn().data("responseStatus", SC_NOT_FOUND)
                    .log("collections DELETE endpoint is not supported as feature EnableDatasetImport is disabled");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Session session = zebedeeCmsService.getSession();
        if (session == null || !zebedeeCmsService.getPermissions().canEdit(session)) {
            info().log("Forbidden request made to the collection endpoint");
            response.setStatus(HttpStatus.SC_FORBIDDEN);
            return;
        }

        Path path = Path.newInstance(request);
        List<String> pathSegments = path.segments();

        if (!isValidPath(response, path, pathSegments)) return;

        String collectionID = pathSegments.get(1);
        String resourceID = pathSegments.get(3);

        Collection collection = zebedeeCmsService.getCollection(collectionID);
        if (collection == null) {
            info().data("collectionId", collectionID).log("Collection not found");
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            return;
        }

        String dType = pathSegments.get(2);
        switch (dType) {
            case "datasets":
                switch (pathSegments.size()) {
                    case 4: // /collections/{collection_id}/datasets/{dataset_id}
                        removeDatasetFromCollection(collection, resourceID);
                        break;
                    case 8: // /collections/{collection_id}/datasets/{dataset_id}/editions/{}/versions/{}
                        String edition = pathSegments.get(5);
                        String version = pathSegments.get(7);
                        removeDatasetVersionFromCollection(collection, resourceID, edition, version);
                        break;
                    default:
                        response.setStatus(HttpStatus.SC_NOT_FOUND);
                        return;
                }
                break;
            case "interactives":
                switch (pathSegments.size()) {
                    case 4: // /collections/{collection_id}/interactives/{interactive_id}

                        removeInteractiveFromCollection(collection, resourceID);
                        response.setStatus(HttpStatus.SC_NO_CONTENT);
                        break;

                    default:
                        response.setStatus(HttpStatus.SC_NOT_FOUND);
                        return;
                }
                break;
            default:
                response.setStatus(HttpStatus.SC_NOT_FOUND);
        }
    }

    private void updateDatasetVersionInCollection(Collection collection, String datasetID, String edition, String version, HttpServletRequest request, String user) throws ZebedeeException, IOException, DatasetAPIException {

        info().data("collectionId", collection.getId())
                .data("datasetId", datasetID)
                .data("edition", edition)
                .data("version", version)
                .data("user", user)
                .log("PUT called on /collections/{}/datasets/{}/editions/{}/versions/{} endpoint");

        try (InputStream body = request.getInputStream()) {

            CollectionDatasetVersion datasetVersion = ContentUtil.deserialise(body, CollectionDatasetVersion.class);
            datasetService.updateDatasetVersionInCollection(collection, datasetID, edition, version, datasetVersion, user);

        } catch (JsonSyntaxException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    private void updateDatasetInCollection(Collection collection, String datasetID, HttpServletRequest request, String user) throws ZebedeeException, IOException, DatasetAPIException {

        info().data("collectionId", collection.getId())
                .data("datasetId", datasetID)
                .data("user", user)
                .log("PUT called on /collections/{}/datasets/{} endpoint");

        try (InputStream body = request.getInputStream()) {

            CollectionDataset dataset = ContentUtil.deserialise(body, CollectionDataset.class);
            datasetService.updateDatasetInCollection(collection, datasetID, dataset, user);

        } catch (JsonSyntaxException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    private void updateInteractiveInCollection(Collection collection, String interactiveID, HttpServletRequest request, String user) throws ZebedeeException, IOException, DatasetAPIException {

        info().data("collectionId", collection.getId())
                .data("interactiveId", interactiveID)
                .data("user", user)
                .log("PUT called on /collections/{}/interactives/{} endpoint");

        try (InputStream body = request.getInputStream()) {

            CollectionInteractive interactive = ContentUtil.deserialise(body, CollectionInteractive.class);
            interactivesService.updateInteractiveInCollection(collection, interactiveID, interactive, user);

        } catch (JsonSyntaxException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    private void removeDatasetVersionFromCollection(Collection collection, String datasetID, String edition, String version) throws ZebedeeException, IOException, DatasetAPIException {
        info().data("collectionId", collection.getId())
                .data("datasetId", datasetID)
                .data("edition", edition)
                .data("version", version)
                .log("DELETE called on /collections/{collection_id}/datasets/{}/editions/{}/versions/{} endpoint");

        datasetService.removeDatasetVersionFromCollection(collection, datasetID, edition, version);
    }

    private void removeDatasetFromCollection(Collection collection, String datasetID) throws ZebedeeException, IOException, DatasetAPIException {

        info().data("collectionId", collection.getId())
                .data("datasetId", datasetID)
                .log("DELETE called on /collections/{collection_id}/datasets/{} endpoint");

        datasetService.removeDatasetFromCollection(collection, datasetID);
    }

    private void removeInteractiveFromCollection(Collection collection, String interactiveID) throws ZebedeeException, IOException, DatasetAPIException {

        info().data("collectionId", collection.getId())
                .data("interactiveId", interactiveID)
                .log("DELETE called on /collections/{collection_id}/interactiveID/{} endpoint");

        interactivesService.removeInteractiveFromCollection(collection, interactiveID);
    }

    private boolean isValidPath(HttpServletResponse response, Path path, List<String> segments) {

        // /collections/{collection_id}/datasets/{dataset_id}
        // /collections/{collection_id}/datasets/{dataset_id}/editions/{}/versions/{}
        if (segments.size() < 4) {
            info().data("path", path).log("Endpoint for collections not found");
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            return false;
        }

        boolean validResource = segments.get(2).equalsIgnoreCase("datasets") ||
                segments.get(2).equalsIgnoreCase("interactives");
        if(!validResource) {
            info().data("path", path).log("Endpoint for collections not found");
            response.setStatus(HttpStatus.SC_NOT_FOUND);
            return false;
        }

        return true;
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
