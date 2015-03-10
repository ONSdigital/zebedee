package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;
import java.util.List;

@Api
public class Collection {

    static com.github.onsdigital.zebedee.Collection getCollection(HttpServletRequest request)
            throws IOException {
        com.github.onsdigital.zebedee.Collection result = null;

        Path path = Path.newInstance(request);
        List<String> segments = path.segments();

        String collectionName = "";
        if (segments.size() > 1) {
            collectionName = segments.get(1);
        }
        for (String segment : path.segments()) {
            System.out.println(" - " + segment);
        }

        if (collectionName.length() > 0) {
            List<com.github.onsdigital.zebedee.Collection> collections = Root.zebedee.getCollections();

            for (com.github.onsdigital.zebedee.Collection collection : collections) {
                if (collection.description.name.equals(collectionName)) {
                    result = collection;
                }
            }
        }

        return result;
    }

    @GET
    public Object get(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        com.github.onsdigital.zebedee.Collection result = getCollection(request);
        if (result == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
        }
        return result.description;
    }

    @POST
    public void create(HttpServletRequest request,
                       HttpServletResponse response,
                       CollectionDescription collectionDescription) throws IOException {

        collectionDescription.name = StringUtils.trim(collectionDescription.name);
        for (com.github.onsdigital.zebedee.Collection collection : Root.zebedee.getCollections()) {
            if (StringUtils.equals(collection.description.name,
                    collectionDescription.name)) {
                response.setStatus(HttpStatus.CONFLICT_409);
                return;
            }
        }
        com.github.onsdigital.zebedee.Collection.create(collectionDescription.name, Root.zebedee);
    }
}
