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
import java.util.Comparator;
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

        return Root.zebedee.collections.list().getCollection(collectionId);
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

        List<Collection> collections = Root.zebedee.collections.list();
        for (Collection collection : collections) {
            CollectionDescription description = new CollectionDescription();
            description.id = collection.description.id;
            description.name = collection.description.name;
            description.publishDate = collection.description.publishDate;
            description.approvedStatus = collection.description.approvedStatus;
            description.type = collection.description.type;
            result.add(description);
        }

        // sort the collections alphabetically by name.
        java.util.Collections.sort(result, new Comparator<CollectionDescription>() {
            @Override
            public int compare(CollectionDescription o1, CollectionDescription o2) {
                return o1.name.compareTo(o2.name);
            }
        });

        return result;
    }
}
