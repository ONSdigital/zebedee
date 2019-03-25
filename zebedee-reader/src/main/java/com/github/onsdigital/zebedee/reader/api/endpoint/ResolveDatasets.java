package com.github.onsdigital.zebedee.reader.api.endpoint;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.taxonomy.ProductPage;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.api.ReadRequestHandler;
import com.github.onsdigital.zebedee.reader.api.bean.DatasetSummary;
import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;
import com.github.onsdigital.zebedee.reader.resolver.DatasetSummaryResolver;
import com.github.onsdigital.zebedee.reader.util.ReaderResponseWriter;
import com.github.onsdigital.zebedee.reader.util.ResponseWriter;
import dp.api.dataset.exception.DatasetAPIException;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.ReaderLogger.info;
import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.extractFilter;
import static com.github.onsdigital.zebedee.reader.util.ReaderRequestUtils.getRequestedLanguage;

@Api
public class ResolveDatasets {

    private static final String URI = "uri";
    private static final String PAGE_URI = "page_uri";

    private ResponseWriter responseWriter;
    private Function<ContentLanguage, ReadRequestHandler> handlerSupplier;
    private DatasetSummaryResolver datasetSummaryResolver;

    public ResolveDatasets() throws ZebedeeException {
        this.handlerSupplier = (lang) -> new ReadRequestHandler(lang);
        this.datasetSummaryResolver = new DatasetSummaryResolver();
        this.responseWriter = new ReaderResponseWriter();
    }

    ResolveDatasets(DatasetSummaryResolver datasetSummaryResolver, ResponseWriter responseWriter,
                    Function<ContentLanguage, ReadRequestHandler> handlerSupplier) {
        this.datasetSummaryResolver = datasetSummaryResolver;
        this.responseWriter = responseWriter;
        this.handlerSupplier = handlerSupplier;
    }

    @GET
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ZebedeeException, DatasetAPIException {
        try {
            ContentLanguage language = getRequestedLanguage(request);
            ReadRequestHandler handler = handlerSupplier.apply(language);
            String pageURI = request.getParameter(URI);

            if (StringUtils.isEmpty(pageURI)) {
                throw new BadRequestException("uri parameter is required but was not specified");
            }

            List<DatasetSummary> summaries = resolve(request, handler);
            info().data(PAGE_URI, pageURI).log("resolve dataset summaries request completed successfully");
            responseWriter.sendResponse(summaries, response);
        } catch (BadRequestException ex) {
            responseWriter.sendBadRequest(ex, request, response);
        } catch (NotFoundException ex) {
            responseWriter.sendNotFound(ex, request, response);
        } catch (Exception ex) {
            throw ex;
        }
    }

    private List<DatasetSummary> resolve(HttpServletRequest request, ReadRequestHandler handler)
            throws ZebedeeException, IOException {
        ProductPage productPage = getPage(request, handler);
        String uri = productPage.getUri().toString();

        if (productPage.getDatasets() == null || productPage.getDatasets().isEmpty()) {
            info().data(PAGE_URI, uri).log("product page does not contain any dataset links");
            return new ArrayList<>();
        }

        info().data(PAGE_URI, uri).log("resolving dataset links for product page");
        return productPage.getDatasets()
                .parallelStream()
                .map(link -> datasetSummaryResolver.resolve(uri, link, request, handler))
                .filter(summary -> summary != null)
                .collect(Collectors.toList());
    }

    private ProductPage getPage(HttpServletRequest request, ReadRequestHandler handler) throws ZebedeeException, IOException {
        Content c = handler.findContent(request, extractFilter(request));

        Page p = (Page) c;
        if (p.getType() != PageType.product_page) {
            throw new BadRequestException("invalid page type for getDatasetSummaries datasets");
        }
        return (ProductPage) p;
    }
}
