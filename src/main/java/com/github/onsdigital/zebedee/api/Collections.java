package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.onsdigital.zebedee.Collection;
import com.github.onsdigital.zebedee.json.CollectionDescription;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Api
public class Collections {

    public static Collection getCollection(HttpServletRequest request)
            throws IOException {

        Path path = Path.newInstance(request);
        List<String> segments = path.segments();

        String collectionName = "";
        if (segments.size() > 1) {
            collectionName = segments.get(1);
        }

        return Root.zebedee.getCollections().getCollection(collectionName);
    }

    @GET
    public Object get(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        List<CollectionDescription> result = new ArrayList<>();

        List<Collection> collections = Root.zebedee.getCollections();
        for (Collection collection : collections) {
            result.add(collection.description);
        }

        return result;
    }


}
