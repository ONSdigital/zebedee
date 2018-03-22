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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;

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

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

@Api
public class Page {

    private final ZebedeeCmsService zebedeeCmsService;
    private final PageUpdateHook pageCreationHook;
    private final PageUpdateHook pageDeletionHook;

    static final String zebedeeFileSuffix = "/data.json";

    /**
     * Default constructor used instantiates dependencies itself.
     */
    public Page() throws URISyntaxException {

        zebedeeCmsService = ZebedeeCmsService.getInstance();

        DatasetAPIClient datasetAPIClient = new DatasetAPIClient(
                Configuration.getDatasetAPIURL(),
                Configuration.getDatasetAPIAuthToken());

        Map<PageType, PageUpdateHook> creationHooks = initialisePageCreationHooks(datasetAPIClient);
        Map<PageType, PageUpdateHook> deletionHooks = initialisePageDeletionHooks(datasetAPIClient);

        pageDeletionHook = new PageTypeUpdateHook(deletionHooks);
        pageCreationHook = new PageTypeUpdateHook(creationHooks);
    }

    private Map<PageType, PageUpdateHook> initialisePageDeletionHooks(DatasetAPIClient datasetAPIClient) {
        APIDatasetLandingPageDeletionHook datasetLandingPageDeletionHook =
                new APIDatasetLandingPageDeletionHook(datasetAPIClient);

        Map<PageType, PageUpdateHook> deletionHooks = new HashMap<>();
        deletionHooks.put(PageType.api_dataset_landing_page, datasetLandingPageDeletionHook);

        return deletionHooks;
    }

    private Map<PageType, PageUpdateHook> initialisePageCreationHooks(DatasetAPIClient datasetAPIClient) {
        APIDatasetLandingPageCreationHook datasetLandingPageCreationHook =
                new APIDatasetLandingPageCreationHook(datasetAPIClient);

        Map<PageType, PageUpdateHook> creationHooks = new HashMap<>();
        creationHooks.put(PageType.api_dataset_landing_page, datasetLandingPageCreationHook);

        return creationHooks;
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
     * @throws IOException           If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     * @throws BadRequestException   If the request cannot be completed because of a problem with request parameters
     * @throws NotFoundException     If the requested URI does not exist in the collection.
     * @throws UnauthorizedException If the user does not have publisher permission.
     * @throws ConflictException     If the URI is being edited in another collection
     */
    @POST
    public com.github.onsdigital.zebedee.content.page.base.Page createPage(HttpServletRequest request, HttpServletResponse response) {

        com.github.onsdigital.zebedee.content.page.base.Page page = null;

        // We have to get the request InputStream before reading any request parameters
        // otherwise the call to get a request parameter will actually consume the body:
        try (InputStream requestBody = request.getInputStream()) {

            String uri = request.getParameter("uri");
            if (StringUtils.isEmpty(uri)) {
                logError(new BadRequestException("uri is empty")).log();
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                return page;
            }
            uri = trimZebedeeFileSuffix(uri);

            Session session;
            try {
                session = zebedeeCmsService.getSession(request);
            } catch (ZebedeeException e) {
                logError(e, "failed to get session")
                        .path(uri).log();
                response.setStatus(e.statusCode);
                return page;
            }

            Collection collection;
            try {
                collection = zebedeeCmsService.getCollection(request);
            } catch (ZebedeeException e) {
                logError(e, "failed to get collection")
                        .user(session.getEmail())
                        .path(uri).log();
                response.setStatus(e.statusCode);
                return page;
            }

            byte[] bytes = IOUtils.toByteArray(requestBody);

            try (ByteArrayInputStream pageInputStream = new ByteArrayInputStream(bytes)) {
                page = ContentUtil.deserialiseContent(pageInputStream);
            } catch (Exception e) {
                response.setStatus(HttpStatus.SC_BAD_REQUEST);
                logError(e, "failed to deserialise page from the request body").log();
                return page;
            }

            if (pageCreationHook != null) {
                try {
                    pageCreationHook.onPageUpdated(page, uri);
                } catch (IOException | RuntimeException e) {
                    logError(e, "exception when calling page creation hook")
                            .collectionId(collection.getDescription().id)
                            .user(session.getEmail())
                            .path(uri).log();
                    response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    return page;
                }
            }

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
                zebedeeCmsService.getZebedee().getCollections().createContent(
                        collection,
                        uri + zebedeeFileSuffix,
                        session,
                        request,
                        inputStream,
                        CollectionEventType.COLLECTION_PAGE_SAVED,
                        false);

            } catch (ZebedeeException e) {
                handleZebdeeException("failed to create content", e, response, uri, session, collection);
            } catch (IOException | FileUploadException e) {
                logError(e, "exception when calling collections.createContent")
                        .collectionId(collection.getDescription().id)
                        .user(session.getEmail())
                        .path(uri).log();
                response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }

            Audit.Event.CONTENT_SAVED
                    .parameters()
                    .host(request)
                    .collection(collection)
                    .content(uri)
                    .user(session.getEmail())
                    .log();

        } catch (IOException e) {
            logError(e, "exception reading request body on create page endpoint").log();
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        response.setStatus(HttpStatus.SC_CREATED);
        return page;
    }

    /**
     * Deletes page content from the endpoint <code>/Content/[CollectionName]/?uri=[uri]</code>
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response Returns true or false according to whether the URI was deleted.
     * @throws IOException           If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     * @throws BadRequestException   If the request cannot be completed because of a problem with request parameters
     * @throws NotFoundException     If the requested URI does not exist in the collection.
     * @throws UnauthorizedException If the user does not have publisher permission.
     * @throws ConflictException     If the URI is being edited in another collection
     */
    @DELETE
    public void deletePage(HttpServletRequest request, HttpServletResponse response) {

        String uri = request.getParameter("uri");
        if (StringUtils.isEmpty(uri)) {
            logError(new BadRequestException("uri is empty")).log();
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        uri = trimZebedeeFileSuffix(uri);

        Session session;
        try {
            session = zebedeeCmsService.getSession(request);
        } catch (ZebedeeException e) {
            logError(e, "failed to get session")
                    .path(uri).log();
            response.setStatus(e.statusCode);
            return;
        }

        Collection collection;
        try {
            collection = zebedeeCmsService.getCollection(request);
        } catch (ZebedeeException e) {
            logError(e, "failed to get collection")
                    .user(session.getEmail())
                    .path(uri).log();
            response.setStatus(e.statusCode);
            return;
        }

        CollectionReader collectionReader;
        try {
            collectionReader = zebedeeCmsService.getZebedeeCollectionReader(collection, session);
        } catch (ZebedeeException e) {
            handleZebdeeException("failed to get collection reader", e, response, uri, session, collection);
            return;
        }

        com.github.onsdigital.zebedee.content.page.base.Page page;

        try {
            page = collectionReader.getContent(uri);
        } catch (NotFoundException ex) {
            logInfo("page is already deleted").path(uri).collectionName(collection).log();
            response.setStatus(HttpStatus.SC_NO_CONTENT);
            return; // idempotent
        } catch (ZebedeeException e) {
            handleZebdeeException("exception when getting collection content", e, response, uri, session, collection);
            return;
        } catch (IOException e) {
            logError(e, "exception when attempting to get collection content")
                    .collectionId(collection)
                    .user(session.getEmail())
                    .path(uri).log();
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        if (pageDeletionHook != null) {
            try {
                pageDeletionHook.onPageUpdated(page, uri);
            } catch (IOException | RuntimeException e) {
                logError(e, "exception when calling page deletion hook")
                        .collectionId(collection.getDescription().id)
                        .user(session.getEmail())
                        .path(uri).log();
                response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }

        try {
            zebedeeCmsService.getZebedee().getCollections().deleteContent(
                    collection,
                    uri + zebedeeFileSuffix,
                    session);
        } catch (IOException e) {
            logError(e, "exception when deleting content")
                    .collectionId(collection.getDescription().id)
                    .user(session.getEmail())
                    .path(uri).log();
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            return;
        } catch (ZebedeeException e) {
            handleZebdeeException("exception when deleting content", e, response, uri, session, collection);
            return;
        }

        Audit.Event.CONTENT_DELETED
                .parameters()
                .host(request)
                .collection(collection)
                .content(uri)
                .user(session.getEmail())
                .log();

        response.setStatus(HttpStatus.SC_NO_CONTENT);
        return;
    }

    private void handleZebdeeException(
            String message,
            ZebedeeException e,
            HttpServletResponse response,
            String uri,
            Session session,
            Collection collection) {

        logError(e, message)
                .collectionId(collection.getDescription().id)
                .user(session.getEmail())
                .path(uri).log();
        response.setStatus(e.statusCode);
    }

    static String trimZebedeeFileSuffix(String uri) {

        if (uri.endsWith(zebedeeFileSuffix))
            return uri.substring(0, uri.length() - zebedeeFileSuffix.length());

        return uri;
    }
}
