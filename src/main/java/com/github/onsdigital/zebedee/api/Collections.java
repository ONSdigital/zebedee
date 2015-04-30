package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.CollectionDescriptions;
import com.github.onsdigital.zebedee.model.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.util.List;

@Api
public class Collections {

    public static Collection getCollection(HttpServletRequest request)
            throws IOException {

        Path path = Path.newInstance(request);
        List<String> segments = path.segments();

        String collectionId = "";
        if (segments.size() > 1) {
            collectionId = segments.get(1);
        }

        return Root.zebedee.getCollections().getCollection(collectionId);
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
            throws IOException {

        CollectionDescriptions result = new CollectionDescriptions();

        List<Collection> collections = Root.zebedee.getCollections();
        for (Collection collection : collections) {

            CollectionDescription description = new CollectionDescription();
            description.id = collection.description.id;
            description.name = collection.description.name;
            description.publishDate = collection.description.publishDate;
            description.approvedStatus = collection.description.approvedStatus;
            result.add(description);
        }

        return result;
    }
}
