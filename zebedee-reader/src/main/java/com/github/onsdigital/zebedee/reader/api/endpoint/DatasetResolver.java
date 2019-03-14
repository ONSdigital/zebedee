package com.github.onsdigital.zebedee.reader.api.endpoint;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import com.github.onsdigital.zebedee.content.page.taxonomy.ProductPage;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.reader.util.ReaderResponseResponseUtils;
import dp.api.dataset.DatasetAPIClient;
import dp.api.dataset.exception.DatasetAPIException;
import dp.api.dataset.model.Dataset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.extractFilter;
import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.getRequestedLanguage;

/**
 * Created by bren on 29/07/15.
 * <p>
 * Endpoint to read content json
 */

@Api
public class DatasetResolver {


    private DatasetAPIClient apiClient;

    public DatasetResolver() {
        try {
            this.apiClient = new DatasetAPIClient(
                    "http://localhost:22000",
                    "FD0108EA-825D-411C-9B1D-41EF7727F465",
                    "Bearer fc4089e2e12937861377629b0cd96cf79298a4c5d329a2ebb96664c88df77b67"
            );
        } catch (Exception e) {
            // TODO
        }
    }

    /**
     * Retrieves content for endpoint <code>/data[collectionId]/?uri=[uri]</code>
     * <p>
     * <p>
     * This endpoint retrieves and serves json from either a collection or published data.
     * <p>
     * It is possible to filter only certain bits of data using filters.
     * <p>
     * e.g. ?uri=/economy/environmentalaccounts/articles/greenhousegasemissions/2015-06-02&title will only return title of the requested content
     *
     * @param request  This should contain a X-Florence-Token header for the current session and the collection id being worked on
     *                 If no collection id is given published contents will be served
     * @param response Servlet response
     * @return
     * @throws IOException           If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     * @throws NotFoundException     If the requested URI does not exist.
     * @throws BadRequestException   IF the request cannot be completed because of a problem with request parameters
     * @throws UnauthorizedException If collection requested but authentication token not available
     * If collection requested but current session does not have view permission
     */

    static class DatasetSummary {
        private String title;
        private String summary;
        private String uri;

        public DatasetSummary() {
        }

        public DatasetSummary(DatasetLandingPage dlp) {
            this.title = dlp.getDescription().getTitle();
            this.summary = dlp.getDescription().getSummary();
            this.uri = dlp.getUri().toString();
        }

        public DatasetSummary(Dataset dataset) {
            this.title = dataset.getTitle();
            this.summary = dataset.getDescription();
            this.uri = dataset.getLinks().getSelf().getHref();
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }


    @GET
    public void resolve(HttpServletRequest request, HttpServletResponse response) throws IOException, ZebedeeException, DatasetAPIException {
        try {
            List<DatasetSummary> resolved = new ArrayList<>();

            ReadRequestHandler handler = new ReadRequestHandler(getRequestedLanguage(request));
            Content c = handler.findContent(request, extractFilter(request));

            Page p = (Page) c;
            if (p.getType() == PageType.product_page) {
                ProductPage productPage = (ProductPage) p;

                if (productPage.getDatasets() != null && !productPage.getDatasets().isEmpty()) {
                    for (Link datasetURI : productPage.getDatasets()) {

                        String uri = datasetURI.getUri().toString();

                        if (uri.startsWith("/datasets")) {

                            String[] sections = uri.split("/");
                            String datasetID = sections[2];
                            Dataset d = apiClient.getDataset(datasetID);
                            resolved.add(new DatasetSummary(d));
                        } else {
                            DatasetLandingPage dlp = (DatasetLandingPage) handler.getContent(datasetURI.getUri().toString(), request);
                            resolved.add(new DatasetSummary(dlp));
                        }
                    }

                    ReaderResponseResponseUtils.sendResponse(resolved, response);
                }
            }
        } catch (NotFoundException exception) {
            ReaderResponseResponseUtils.sendNotFound(exception, request, response);
        }
    }
}
