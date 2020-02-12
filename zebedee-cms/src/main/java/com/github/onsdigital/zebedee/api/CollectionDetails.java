package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.configuration.CMSFeatureFlags;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDetail;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.Events;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReaderSupplier;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.service.ContentDeleteService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ContentDetailUtil;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.util.Set;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

@Api
public class CollectionDetails {

    private static ContentDeleteService contentDeleteService = ContentDeleteService.getInstance();
    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();

    private final boolean datasetImportEnabled;

    // Slighly convoluted but wrapping the creation of a new ZebedeeCollectionReader in a "supplier" means we can
    // replace it with a mock in our tests and avoid all the unnecessary pain and agro associated with creating
    // complex object.
    private ZebedeeCollectionReaderSupplier zebedeeCollectionReaderSupplier = (z, c, s) -> new ZebedeeCollectionReader(z, c, s);

    public CollectionDetails() {
        this.datasetImportEnabled = CMSFeatureFlags.cmsFeatureFlags().isEnableDatasetImport();
    }

    /**
     * Constructor to allow dependencies to be injected.
     */
    CollectionDetails(ContentDeleteService deleteService, ZebedeeCollectionReaderSupplier zebedeeCollectionReaderSupplier,
                      ZebedeeCmsService cmsService, boolean datasetImportEnabled) {
        this.datasetImportEnabled = datasetImportEnabled;
        this.zebedeeCollectionReaderSupplier = zebedeeCollectionReaderSupplier;

        contentDeleteService = deleteService;
        zebedeeCmsService = cmsService;
    }

    /**
     * Retrieves a CollectionDetail object at the endpoint /CollectionDetails/[CollectionName]
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                 <li>If no collection exists:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                 </ul>
     * @return the CollectionDetail.
     * @throws IOException
     */
    @GET
    public CollectionDetail get(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ZebedeeException {

        com.github.onsdigital.zebedee.model.Collection collection = Collections
                .getCollection(request);

        if (collection == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return null;
        }

        Session session = zebedeeCmsService.getSession(request);
        if (!zebedeeCmsService.getPermissions().canView(session.getEmail(), collection.getDescription())) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return null;
        }

        //CollectionReader collectionReader = new ZebedeeCollectionReader(Root.zebedee, collection, session);
        CollectionReader collectionReader = zebedeeCollectionReaderSupplier.get(
                zebedeeCmsService.getZebedee(), collection, session);

        CollectionDetail result = new CollectionDetail();

        result.setId(collection.getDescription().getId());
        result.setName(collection.getDescription().getName());
        result.setType(collection.getDescription().getType());
        result.setPublishDate(collection.getDescription().getPublishDate());
        result.setTeams(collection.getDescription().getTeams());
        result.setReleaseUri(collection.getDescription().getReleaseUri());

        result.pendingDeletes = contentDeleteService.getDeleteItemsByCollection(collection);

        result.inProgress = ContentDetailUtil.resolveDetails(collection.getInProgress(), collectionReader.getInProgress());
        result.complete = ContentDetailUtil.resolveDetails(collection.getComplete(), collectionReader.getComplete());
        result.reviewed = ContentDetailUtil.resolveDetails(collection.getReviewed(), collectionReader.getReviewed());

        result.approvalStatus = collection.getDescription().approvalStatus;
        result.events = collection.getDescription().events;
        result.timeseriesImportFiles = collection.getDescription().timeseriesImportFiles;

        addEventsForDetails(result.inProgress, collection);
        addEventsForDetails(result.complete, collection);
        addEventsForDetails(result.reviewed, collection);

        Set<Integer> teamIds = zebedeeCmsService.getPermissions().listViewerTeams(collection.description,
                session);
        result.teamsDetails = zebedeeCmsService.getZebedee().getTeamsService().resolveTeamDetails(teamIds);
        result.teamsDetails.forEach(team -> collection.getDescription().getTeams().add(team.getName()));

        String collectionId = Collections.getCollectionId(request);

        if (datasetImportEnabled) {
            info().data("collectionId", collectionId).data("user", session.getEmail())
                .log("CollectionDetails GET endpoint: datasetImportEnabled including dataset and dataset version " +
                        "details to response");

            result.datasets = collection.getDescription().getDatasets();
            result.datasetVersions = collection.getDescription().getDatasetVersions();
        }

        return result;
    }

    private void addEventsForDetails(
            Iterable<ContentDetail> detailsToAddEventsFor,
            com.github.onsdigital.zebedee.model.Collection collection
    ) {

        for (ContentDetail contentDetail : detailsToAddEventsFor) {
            String language = contentDetail.description.language;
            if (language == null) {
                language = "";
            } else {
                language = "_" + contentDetail.description.language;
            }
            if (collection.getDescription().eventsByUri != null) {
                Events eventsForFile = collection.getDescription().eventsByUri.get(contentDetail.uri + "/data" + language + ".json");
                contentDetail.events = eventsForFile;
            } else {
                contentDetail.events = new Events();
            }
        }
    }
}
