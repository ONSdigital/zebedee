package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.page.APIDatasetLandingPageCreationHook;
import com.github.onsdigital.zebedee.content.page.PageTypeUpdateHook;
import com.github.onsdigital.zebedee.content.page.PageUpdateHook;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import dp.api.dataset.DatasetAPIClient;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Api
public class Page {

    private ZebedeeCmsService zebedeeCmsService;
    private PageUpdateHook pageCreationHook;

    private static final URI websiteURI = URI.create(Configuration.getBabbageUrl());

    /**
     * Default constructor used instantiates dependencies itself.
     */
    public Page() throws URISyntaxException {

        zebedeeCmsService = ZebedeeCmsService.getInstance();

        DatasetAPIClient datasetAPIClient = new DatasetAPIClient(
                Configuration.getDatasetAPIURL(),
                Configuration.getDatasetAPIAuthToken());

        APIDatasetLandingPageCreationHook datasetLandingPageCreationHook =
                new APIDatasetLandingPageCreationHook(datasetAPIClient, websiteURI);

        Map<PageType, PageUpdateHook> creationHooks = new HashMap<>();
        creationHooks.put(PageType.api_dataset_landing_page, datasetLandingPageCreationHook);

        pageCreationHook = new PageTypeUpdateHook(creationHooks);
    }

    /**
     * Constructor allowing dependencies to be injected.
     * @param zebedeeCmsService
     * @param pageCreationHook
     */
    public Page(ZebedeeCmsService zebedeeCmsService, PageUpdateHook pageCreationHook) {
        this.zebedeeCmsService = zebedeeCmsService;
        this.pageCreationHook = pageCreationHook;
    }

    @POST
    public void createPage(HttpServletRequest request, HttpServletResponse response) throws IOException, ZebedeeException, FileUploadException {

        // We have to get the request InputStream before reading any request parameters
        // otherwise the call to get a request parameter will actually consume the body:
        try (InputStream requestBody = request.getInputStream()) {

            Session session = zebedeeCmsService.getSession(request);
            com.github.onsdigital.zebedee.model.Collection collection = zebedeeCmsService.getCollection(request);
            String uri = request.getParameter("uri");

            byte[] bytes = IOUtils.toByteArray(requestBody);
            com.github.onsdigital.zebedee.content.page.base.Page page;

            try (ByteArrayInputStream pageInputStream = new ByteArrayInputStream(bytes)) {
                page = ContentUtil.deserialiseContent(pageInputStream);
            } catch (Exception e) {
                throw new BadRequestException("request is not a valid page object");
            }

            pageCreationHook.onPageUpdated(page, uri);

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                zebedeeCmsService.getZebedee().getCollections().createContent(
                        collection,
                        uri,
                        session,
                        request,
                        inputStream,
                        CollectionEventType.COLLECTION_PAGE_SAVED,
                        false);
            }

            Audit.Event.CONTENT_SAVED
                    .parameters()
                    .host(request)
                    .collection(collection)
                    .content(uri)
                    .user(session.getEmail())
                    .log();

        }
    }
}
