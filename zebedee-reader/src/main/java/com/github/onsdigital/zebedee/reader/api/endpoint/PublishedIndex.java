package com.github.onsdigital.zebedee.reader.api.endpoint;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.reader.api.bean.PublishedIndexResponse;
import com.github.onsdigital.zebedee.search.indexing.Document;
import com.github.onsdigital.zebedee.search.indexing.FileScanner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.util.List;

@Api
public class PublishedIndex {

    /**
     * Retrieves list of content for endpoint <code>/publishedindex</code>
     * <p>
     * This endpoint returns a list of URIs for all the published content served by zebedee
     *
     * @param request  No authentication headers are required due to this only serving published content
     * @param response Servlet response
     * @return
     * @throws IOException If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     */
    @GET
    public PublishedIndexResponse read(HttpServletRequest request, HttpServletResponse response) throws IOException {

        List<Document> docs = new FileScanner().scan();

        /* TODO paging is not currently implemented (there would be complications involving ensuring that
            duplicates or omissions of documents added or removed between pages) so for now the endpoint always
            returns all published documents and ignores any supplied parameters
        */
        PublishedIndexResponse publishedIndexResponse = new PublishedIndexResponse();
        publishedIndexResponse.addDocuments(docs);
        publishedIndexResponse.setOffset(0);
        publishedIndexResponse.setLimit(docs.size());
        publishedIndexResponse.setTotalCount(docs.size());
        return publishedIndexResponse;

    }
}
