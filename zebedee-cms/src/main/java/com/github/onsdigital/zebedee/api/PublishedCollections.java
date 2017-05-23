package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.service.publishedcollections.PublishedCollection;
import com.github.onsdigital.zebedee.service.publishedcollections.PublishedCollectionException;
import com.github.onsdigital.zebedee.service.publishedcollections.PublishedCollectionService;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;

@Api
public class PublishedCollections {

    private static final PublishedCollectionService reportService = new PublishedCollectionService();

    @GET
    public Object get(HttpServletRequest request, HttpServletResponse response)
            throws PublishedCollectionException {

        String collectionId = Collections.getCollectionId(request);

       if (StringUtils.isNotEmpty(collectionId)) {

            return reportService.getCollection(collectionId);
        }
        return reportService.getList();
    }
}