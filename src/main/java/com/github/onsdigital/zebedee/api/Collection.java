package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;

@Api
public class Collection {



    @GET
    public Object get(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        com.github.onsdigital.zebedee.Collection collection = Collections.getCollection(request);
        if (collection == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
        }
        return collection.description;
    }

    @POST
    public void update(HttpServletRequest request,
                       HttpServletResponse response,
                       CollectionDescription collectionDescription) throws IOException {

        com.github.onsdigital.zebedee.Collection existingCollection = Collections.getCollection(request);

        collectionDescription.name = StringUtils.trim(collectionDescription.name);

        if (existingCollection == null) {

            if (Root.zebedee.getCollections().hasCollection(collectionDescription.name)) {
                response.setStatus(HttpStatus.CONFLICT_409);
                return;
            }

            com.github.onsdigital.zebedee.Collection.create(collectionDescription.name, Root.zebedee);
        } else {
            if (!StringUtils.equals(existingCollection.description.name, collectionDescription.name)) {
                if (Root.zebedee.getCollections().hasCollection(collectionDescription.name)) {
                    response.setStatus(HttpStatus.CONFLICT_409);
                    return;
                }

                com.github.onsdigital.zebedee.Collection.rename(existingCollection.description.name,
                        collectionDescription.name, Root.zebedee);
            }
        }
    }
}
