package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionDescriptions;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

@Api
public class Collections {

    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();

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

            for (Collection collection : collections) {
                if (Root.zebedee.getPermissionsService().canView(session, collection.getDescription())) {
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
            java.util.Collections.sort(result, new Comparator<CollectionDescription>() {
                @Override
                public int compare(CollectionDescription o1, CollectionDescription o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            return result;
        } catch (IOException e) {
            logError(e, "Unexpected error while attempting to get collections")
                    .logAndThrow(UnexpectedErrorException.class);
        }
        return null;
    }
}
