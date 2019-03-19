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
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.reader.api.bean.DatasetSummary;
import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;
import com.github.onsdigital.zebedee.reader.util.ReaderResponseWriter;
import com.github.onsdigital.zebedee.reader.util.ResponseWriter;
import dp.api.dataset.DatasetAPIClient;
import dp.api.dataset.exception.DatasetAPIException;
import dp.api.dataset.model.Dataset;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.ReaderFeatureFlags.readerFeatureFlags;
import static com.github.onsdigital.zebedee.logging.ReaderLogger.error;
import static com.github.onsdigital.zebedee.logging.ReaderLogger.info;
import static com.github.onsdigital.zebedee.logging.ReaderLogger.warn;
import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.extractFilter;
import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.getRequestedLanguage;

@Api
public class ResolveDatasets {

    private DatasetAPIClient datasetAPIClient;
    private ResponseWriter responseWriter;
    private Function<ContentLanguage, ReadRequestHandler> handlerSupplier;

    public ResolveDatasets() {
        this.handlerSupplier = (lang) -> new ReadRequestHandler(lang);
        this.responseWriter = new ReaderResponseWriter();
        try {
            // TODO
            this.datasetAPIClient = new DatasetAPIClient(
                    "http://localhost:22000",
                    "FD0108EA-825D-411C-9B1D-41EF7727F465",
                    "Bearer fc4089e2e12937861377629b0cd96cf79298a4c5d329a2ebb96664c88df77b67"
            );
        } catch (Exception e) {
            // TODO
        }
    }

    ResolveDatasets(DatasetAPIClient datasetAPIClient, ResponseWriter responseWriter, Function<ContentLanguage, ReadRequestHandler> handlerSupplier) {
        this.datasetAPIClient = datasetAPIClient;
        this.responseWriter = responseWriter;
        this.handlerSupplier = handlerSupplier;
    }

    @GET
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ZebedeeException, DatasetAPIException {
        try {
            String pageURI = request.getParameter("uri");
            List<DatasetSummary> summaries = getDatasetSummaries(request, pageURI);
            info().data("page_uri", pageURI)
                    .data("dataset_uris", summaries.stream()
                            .map(s -> s.getUri())
                            .collect(Collectors.toList()))
                    .log("resoloved dataset summaries for uri");

            responseWriter.sendResponse(summaries, response);
        } catch (BadRequestException ex) {
            responseWriter.sendBadRequest(ex, request, response);
        } catch (NotFoundException ex) {
            responseWriter.sendNotFound(ex, request, response);
        } catch (Exception ex) {
            throw ex;
        }
    }

    private List<DatasetSummary> getDatasetSummaries(HttpServletRequest request, String pageURI) throws IOException,
            ZebedeeException {
        ReadRequestHandler handler = handlerSupplier.apply(getRequestedLanguage(request));
        Content c = handler.findContent(request, extractFilter(request));

        Page p = (Page) c;
        if (p.getType() != PageType.product_page) {
            throw new BadRequestException("invalid page type for getDatasetSummaries datasets");
        }

        ProductPage productPage = (ProductPage) p;
        info().data("uri", pageURI).log("resolving dataset summaries for dataset landing page");

        if (productPage.getDatasets() == null || productPage.getDatasets().isEmpty()) {
            info().log("page does not contain any dataset links returning empty response");
            return new ArrayList<>();
        }
        return resolveDatasetLinks(productPage, handler, request, pageURI);
    }

    private boolean isCMDLink(Link dataset) {
        return dataset != null && dataset.getUri().toString().startsWith("/datasets/");
    }

    private List<DatasetSummary> resolveDatasetLinks(ProductPage productPage, ReadRequestHandler handler,
                                                     HttpServletRequest request, String pageURI) {
        return productPage.getDatasets()
                .stream()
                .map(dataset -> mapToDatasetSummary(dataset, handler, request, pageURI))
                .filter(summary -> summary != null)
                .collect(Collectors.toList());
    }

    private DatasetSummary mapToDatasetSummary(Link dataset, ReadRequestHandler handler, HttpServletRequest request,
                                               String pageURI) {
        if (isCMDLink(dataset)) {
            if (!readerFeatureFlags().isEnableDatasetImport()) {
                warn().data("dataset_uri", dataset.getUri().toString())
                        .data("page_uri", pageURI)
                        .log("Omitting CMD dataset summary from resolve datasets request as CMD feature flag is disabled");
                return null;
            }
            return resolveCMDDataset(dataset, pageURI);
        }
        return resolveLegacyDatset(handler, request, dataset);
    }

    /**
     * Get a dataset summary for a old world legacy dataset.
     */
    private DatasetSummary resolveLegacyDatset(ReadRequestHandler handler, HttpServletRequest request, Link dataset) {
        DatasetSummary summary = null;
        try {
            DatasetLandingPage dlp = (DatasetLandingPage) handler.getContent(dataset.getUri().toString(), request);
            summary = new DatasetSummary(dlp);
        } catch (ZebedeeException | IOException e) {
            error().exception(e).log("error resolving legacy dataset");
        }
        return summary;
    }

    /**
     * Get a dataset summary for a new world CMD dataset.
     */
    private DatasetSummary resolveCMDDataset(Link dataset, String pageURI) {
        DatasetSummary summary = null;

        String uri = dataset.getUri().toString();
        String[] sections = uri.split("/");
        String datasetID = sections[2];
        try {
            Dataset d = datasetAPIClient.getDataset(datasetID);
            summary = new DatasetSummary(d);
        } catch (IOException | DatasetAPIException e) {
            error().exception(e)
                    .data("page_uri", pageURI)
                    .data("dataset_uri", uri)
                    .log("could not resolve CMD dataset for page dataset API returned an error");
        }
        return summary;
    }
}
