package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.util.List;

@Api
public class PublishedCollections {

    @GET
    public List<PublishedCollection> get(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        return Root.zebedee.publishedCollections.readFromFile();
    }
}
