package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.page.APIDatasetLandingPageCreationHook;
import com.github.onsdigital.zebedee.content.page.APIDatasetLandingPageDeletionHook;
import com.github.onsdigital.zebedee.content.page.PageTypeUpdateHook;
import com.github.onsdigital.zebedee.content.page.PageUpdateHook;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import dp.api.dataset.DatasetAPIClient;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@Api
public class Page {

    private ZebedeeCmsService zebedeeCmsService;
    private PageUpdateHook pageCreationHook;
    private PageUpdateHook pageDeletionHook;

    static final String zebedeeFileSuffix = "/data.json";

    /**
     * Default constructor used instantiates dependencies itself.
     */
    public Page() throws URISyntaxException {

        zebedeeCmsService = ZebedeeCmsService.getInstance();

        DatasetAPIClient datasetAPIClient = new DatasetAPIClient(
                Configuration.getDatasetAPIURL(),
                Configuration.getDatasetAPIAuthToken());

        initialisePageCreationHook(datasetAPIClient);
        initialisePageDeletionHooks(datasetAPIClient);
    }

    private void initialisePageDeletionHooks(DatasetAPIClient datasetAPIClient) {
        APIDatasetLandingPageDeletionHook datasetLandingPageDeletionHook =
                new APIDatasetLandingPageDeletionHook(datasetAPIClient);

        Map<PageType, PageUpdateHook> deletionHooks = new HashMap<>();
        deletionHooks.put(PageType.api_dataset_landing_page, datasetLandingPageDeletionHook);

        pageDeletionHook = new PageTypeUpdateHook(deletionHooks);
    }

    private void initialisePageCreationHook(DatasetAPIClient datasetAPIClient) {
        APIDatasetLandingPageCreationHook datasetLandingPageCreationHook =
                new APIDatasetLandingPageCreationHook(datasetAPIClient);

        Map<PageType, PageUpdateHook> creationHooks = new HashMap<>();
        creationHooks.put(PageType.api_dataset_landing_page, datasetLandingPageCreationHook);

        pageCreationHook = new PageTypeUpdateHook(creationHooks);
    }

    /**
     * Constructor allowing dependencies to be injected.
     *
     * @param zebedeeCmsService
     * @param pageCreationHook
     * @param pageDeletionHook
     */
    public Page(ZebedeeCmsService zebedeeCmsService, PageUpdateHook pageCreationHook, PageUpdateHook pageDeletionHook) {
        this.zebedeeCmsService = zebedeeCmsService;
        this.pageCreationHook = pageCreationHook;
        this.pageDeletionHook = pageDeletionHook;
    }

    /**
     * create a new page from the endpoint <code>/Content/[CollectionName]/?uri=[uri]</code>
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response Returns true or false according to whether the URI was deleted.
     * @return
     * @throws IOException           If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     * @throws BadRequestException   If the request cannot be completed because of a problem with request parameters
     * @throws NotFoundException     If the requested URI does not exist in the collection.
     * @throws UnauthorizedException If the user does not have publisher permission.
     * @throws ConflictException     If the URI is being edited in another collection
     */
    @POST
    public void createPage(HttpServletRequest request, HttpServletResponse response) throws IOException, ZebedeeException, FileUploadException {

        // We have to get the request InputStream before reading any request parameters
        // otherwise the call to get a request parameter will actually consume the body:
        try (InputStream requestBody = request.getInputStream()) {

            Session session = zebedeeCmsService.getSession(request);
            com.github.onsdigital.zebedee.model.Collection collection = zebedeeCmsService.getCollection(request);
            String uri = request.getParameter("uri");
            uri = trimZebedeeFileSuffix(uri);

            byte[] bytes = IOUtils.toByteArray(requestBody);
            com.github.onsdigital.zebedee.content.page.base.Page page;

            try (ByteArrayInputStream pageInputStream = new ByteArrayInputStream(bytes)) {
                page = ContentUtil.deserialiseContent(pageInputStream);
            } catch (Exception e) {
                throw new BadRequestException("request is not a valid page object");
            }

            if (pageCreationHook != null)
                pageCreationHook.onPageUpdated(page, uri);

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                zebedeeCmsService.getZebedee().getCollections().createContent(
                        collection,
                        uri + zebedeeFileSuffix,
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

    /**
     * Deletes page content from the endpoint <code>/Content/[CollectionName]/?uri=[uri]</code>
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response Returns true or false according to whether the URI was deleted.
     * @return
     * @throws IOException           If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     * @throws BadRequestException   If the request cannot be completed because of a problem with request parameters
     * @throws NotFoundException     If the requested URI does not exist in the collection.
     * @throws UnauthorizedException If the user does not have publisher permission.
     * @throws ConflictException     If the URI is being edited in another collection
     */
    @DELETE
    public boolean deletePage(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ZebedeeException {

        Session session = zebedeeCmsService.getSession(request);

        Collection collection = zebedeeCmsService.getCollection(request);
        String uri = request.getParameter("uri");
        uri = trimZebedeeFileSuffix(uri);

        CollectionReader collectionReader = zebedeeCmsService.getZebedeeCollectionReader(collection, session);

        com.github.onsdigital.zebedee.content.page.base.Page page;

        try {
            page = collectionReader.getContent(uri);
        } catch (NotFoundException ex) {
            return true; // idempotent
        }

        if (pageDeletionHook != null)
            pageDeletionHook.onPageUpdated(page, uri);

        boolean result = zebedeeCmsService.getZebedee().getCollections().deleteContent(
                collection,
                uri + zebedeeFileSuffix,
                session);

        if (result) {
            Audit.Event.CONTENT_DELETED
                    .parameters()
                    .host(request)
                    .collection(collection)
                    .content(uri)
                    .user(session.getEmail())
                    .log();
        }

        return result;
    }

    static String trimZebedeeFileSuffix(String uri) {

        if (uri.endsWith(zebedeeFileSuffix))
            return uri.substring(0, uri.length() - zebedeeFileSuffix.length());

        return uri;
    }
}
