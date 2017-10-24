package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDetail;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.Events;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.service.ContentDeleteService;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.teams.model.Team;
import com.github.onsdigital.zebedee.util.ContentDetailUtil;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@Api
public class CollectionDetails {

    private static ContentDeleteService contentDeleteService = ContentDeleteService.getInstance();

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

        Session session = Root.zebedee.getSessionsService().get(request);
        if (!Root.zebedee.getPermissionsService().canView(session.getEmail(), collection.getDescription())) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return null;
        }

        CollectionReader collectionReader = new ZebedeeCollectionReader(Root.zebedee, collection, session);

        CollectionDetail result = new CollectionDetail();
        result.id = collection.getDescription().id;
        result.name = collection.getDescription().name;
        result.type = collection.getDescription().type;
        result.publishDate = collection.getDescription().publishDate;
        result.teams = collection.getDescription().teams;
        result.releaseUri = collection.getDescription().releaseUri;
        result.collectionOwner = collection.getDescription().collectionOwner;
        result.pendingDeletes = contentDeleteService.getDeleteItemsByCollection(collection);

        result.inProgress = ContentDetailUtil.resolveDetails(collection.inProgress, collectionReader.getInProgress());
        result.complete = ContentDetailUtil.resolveDetails(collection.complete, collectionReader.getComplete());
        result.reviewed = ContentDetailUtil.resolveDetails(collection.reviewed, collectionReader.getReviewed());

        result.approvalStatus = collection.getDescription().approvalStatus;
        result.events = collection.getDescription().events;
        result.timeseriesImportFiles = collection.getDescription().timeseriesImportFiles;

        addEventsForDetails(result.inProgress, collection);
        addEventsForDetails(result.complete, collection);
        addEventsForDetails(result.reviewed, collection);

        Set<Integer> teamIds = Root.zebedee.getPermissionsService().listViewerTeams(collection.getDescription(), session);
        List<Team> teams = Root.zebedee.getTeamsService().resolveTeams(teamIds);
        teams.forEach(team -> collection.getDescription().teams.add(team.getName()));

        result.datasets = collection.getDescription().getDatasets();
        result.datasetVersions = collection.getDescription().getDatasetVersions();

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
