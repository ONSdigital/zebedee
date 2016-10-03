package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollectionSearchResult;
import com.github.onsdigital.zebedee.search.client.ElasticSearchClient;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;

@Api
public class PublishedCollections {

    @GET
    public PublishedCollectionSearchResult get(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String collectionId = Collections.getCollectionId(request);

        if (StringUtils.isNotEmpty(collectionId)) {

            return Root.zebedee.getPublishedCollections().search(ElasticSearchClient.getClient(), collectionId);

        }

        return Root.zebedee.getPublishedCollections().search(ElasticSearchClient.getClient());
    }
}