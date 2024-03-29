package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.content.base.ContentLanguage;
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
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import dp.api.dataset.DatasetClient;
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
import java.util.Optional;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.zebedee.configuration.CMSFeatureFlags.cmsFeatureFlags;

@Api
public class Page {

    private final ZebedeeCmsService zebedeeCmsService;
    private final Optional<PageUpdateHook> pageCreationHook;
    private final Optional<PageUpdateHook> pageDeletionHook;

    static final String zebedeeFileSuffix = "/data.json";

    /**
     * Default constructor used instantiates dependencies itself.
     */
    public Page() throws URISyntaxException {
        this(cmsFeatureFlags().isEnableDatasetImport());
    }

    /**
     * Constructor allowing you to specifying if the dataset import feature should be enabled.
     */
    public Page(boolean datasetImportEnabled) throws URISyntaxException {
        this.zebedeeCmsService = ZebedeeCmsService.getInstance();

        if (datasetImportEnabled) {
            info().data("hooks", "pageDeletionHook, pageCreationHook")
                    .log("page endpoint: feature EnableDatasetImport enabled, creating Page hooks");

            DatasetClient datasetAPIClient = zebedeeCmsService.getDatasetClient();

            Map<PageType, PageUpdateHook> creationHooks = initialisePageCreationHooks(datasetAPIClient);
            Map<PageType, PageUpdateHook> deletionHooks = initialisePageDeletionHooks(datasetAPIClient);

            this.pageDeletionHook = Optional.of(new PageTypeUpdateHook(deletionHooks));
            this.pageCreationHook = Optional.of(new PageTypeUpdateHook(creationHooks));
        } else {
            info().log("page endpoint: feature EnableDatasetImport disabled, Page hooks will not be created");
            this.pageCreationHook = Optional.empty();
            this.pageDeletionHook = Optional.empty();
        }
    }

    /**
     * Constructor allowing dependencies to be injected.
     *
     * @param zebedeeCmsService
     * @param pageCreationHook
     * @param pageDeletionHook
     */
    Page(ZebedeeCmsService zebedeeCmsService, PageUpdateHook pageCreationHook, PageUpdateHook pageDeletionHook) {
        this.zebedeeCmsService = zebedeeCmsService;
        this.pageCreationHook = Optional.ofNullable(pageCreationHook);
        this.pageDeletionHook = Optional.ofNullable(pageDeletionHook);
    }

    private Map<PageType, PageUpdateHook> initialisePageDeletionHooks(DatasetClient datasetAPIClient) {
        APIDatasetLandingPageDeletionHook datasetLandingPageDeletionHook =
                new APIDatasetLandingPageDeletionHook(datasetAPIClient);

        Map<PageType, PageUpdateHook> deletionHooks = new HashMap<>();
        deletionHooks.put(PageType.API_DATASET_LANDING_PAGE, datasetLandingPageDeletionHook);

        return deletionHooks;
    }

    private Map<PageType, PageUpdateHook> initialisePageCreationHooks(DatasetClient datasetAPIClient) {
        APIDatasetLandingPageCreationHook datasetLandingPageCreationHook =
                new APIDatasetLandingPageCreationHook(datasetAPIClient);

        Map<PageType, PageUpdateHook> creationHooks = new HashMap<>();
        creationHooks.put(PageType.API_DATASET_LANDING_PAGE, datasetLandingPageCreationHook);

        return creationHooks;
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
    public com.github.onsdigital.zebedee.content.page.base.Page createPage(HttpServletRequest request, HttpServletResponse response) throws UnauthorizedException {
        com.github.onsdigital.zebedee.content.page.base.Page page = null;

        String uri = request.getParameter("uri");
        if (StringUtils.isEmpty(uri)) {
            error().logException(new BadRequestException("uri is empty"), "page get endpoint: uri is empty");
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return null;
        }
        uri = trimZebedeeFileSuffix(uri);

        Session session;
        try {
            session = zebedeeCmsService.getSession();
        } catch (ZebedeeException e) {
            error().data("path", uri).logException(e, "page create endpoint: failed to get session");
            response.setStatus(e.statusCode);
            return null;
        }

        Collection collection;
        try {
            collection = zebedeeCmsService.getCollection(request);
        } catch (ZebedeeException e) {
            error().data("user", session.getEmail()).data("path", uri)
                    .logException(e, "page create endpoint: failed to get collection");
            response.setStatus(e.statusCode);
            return null;
        }

        // We have to get the request InputStream before reading any request parameters
        // otherwise the call to get a request parameter will actually consume the body:
        byte[] requestBodyBytes = null;
        try (InputStream requestBody = request.getInputStream()) {
            requestBodyBytes = IOUtils.toByteArray(requestBody);
        } catch (Exception e) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            error().logException(e, "page create endpoint: failed to deserialise page from the request body");
            return null;
        }

        try (ByteArrayInputStream pageInputStream = new ByteArrayInputStream(requestBodyBytes)) {
            page = ContentUtil.deserialiseContent(pageInputStream);
        } catch (Exception e) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            error().logException(e, "page create endpoint: failed to deserialise page from the request body");
            return null;
        }

        if (pageCreationHook.isPresent()) {
            boolean success = execCreationHook(page, uri, collection, session);
            if (!success) {
                response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                return null;
            }
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(requestBodyBytes)) {
            zebedeeCmsService.getZebedee().getCollections().createContent(
                    collection,
                    uri + zebedeeFileSuffix,
                    session,
                    request,
                    inputStream,
                    false);

        } catch (ZebedeeException e) {
            handleZebdeeException("page get endpoint: failed to create content", e, response, uri, session, collection);
        } catch (IOException | FileUploadException e) {
            error().data("collection_id", collection.getDescription().getId())
                    .data("user", session.getEmail())
                    .data("path", uri)
                    .logException(e, "page create endpoint: exception when calling collections.createContent");
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }

        Audit.Event.CONTENT_SAVED
                .parameters()
                .host(request)
                .collection(collection)
                .content(uri)
                .user(session.getEmail())
                .log();

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
    public void deletePage(HttpServletRequest request, HttpServletResponse response) throws UnauthorizedException {

        String uri = request.getParameter("uri");
        if (StringUtils.isEmpty(uri)) {
            error().logException(new BadRequestException("uri is empty"),"page delete endpoint: uri is empty");
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        uri = trimZebedeeFileSuffix(uri);

        Session session;
        try {
            session = zebedeeCmsService.getSession();
        } catch (ZebedeeException e) {
            error().data("path", uri).logException(e, "page delete endpoint: failed to get session");
            response.setStatus(e.statusCode);
            return;
        }

        Collection collection;
        try {
            collection = zebedeeCmsService.getCollection(request);
        } catch (ZebedeeException e) {
            error().data("user", session.getEmail())
                    .data("path", uri).logException(e, "page delete endpoint: failed to get collection");
            response.setStatus(e.statusCode);
            return;
        }

        if (pageDeletionHook.isPresent()) {
            CollectionReader collectionReader;
            try {
                collectionReader = zebedeeCmsService.getZebedeeCollectionReader(collection, session);
            } catch (ZebedeeException e) {
                handleZebdeeException("page delete endpoint: failed to get collection reader", e, response, uri, session, collection);
                return;
            }

            com.github.onsdigital.zebedee.content.page.base.Page page;

            try {
                page = collectionReader.getContentQuiet(uri);
                if (page == null) {
                    // Couldn't find the content in English, try Welsh
                    collectionReader.setLanguage(ContentLanguage.WELSH);
                    page = collectionReader.getContent(uri);
                }
            } catch (NotFoundException ex) {
                info().data("path", uri).data("collection_id", collection.getId())
                        .log("page delete endpoint: page is already deleted");
                response.setStatus(HttpStatus.SC_NO_CONTENT);
                return; // idempotent
            } catch (ZebedeeException e) {
                handleZebdeeException("page delete endpoint: exception when getting collection content", e, response, uri, session, collection);
                return;
            } catch (IOException e) {
                error().data("collection_id", collection.getId()).data("user", session.getEmail()).data("path", uri)
                        .logException(e, "page delete endpoint: exception when attempting to get collection content");
                response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            boolean success = execDeletionHook(page, uri, collection, session);
            if (!success) {
                response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }

        try {
            boolean deleted = zebedeeCmsService.getZebedee().getCollections().deleteContent(
                        collection, uri, session);
            if (deleted) {
                Audit.Event.CONTENT_DELETED
                    .parameters()
                    .host(request)
                    .collection(collection)
                    .content(uri)
                    .user(session.getEmail())
                    .log();
                response.setStatus(HttpStatus.SC_NO_CONTENT);
            } else {
                error().data("collection_id", collection.getDescription().getId())
                    .data("user", session.getEmail())
                    .data("path", uri)
                    .log("page delete endpoint: couldn't delete content");
                response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            }
        } catch (IOException e) {
            logException(e, "page delete endpoint: exception when deleting content", uri, session, collection);
            response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        } catch (ZebedeeException e) {
            handleZebdeeException("page delete endpoint: exception when deleting content", e, response, uri, session, collection);
        }
    }
    
    private void logException(Exception e, String message, String path, Session session, Collection collection) {
        error().data("collection_id", collection.getDescription().getId())
            .data("user", session.getEmail())
            .data("path", path)
            .logException(e, message);
    }

    private void handleZebdeeException(
            String message,
            ZebedeeException e,
            HttpServletResponse response,
            String uri,
            Session session,
            Collection collection) {

        logException(e, message, uri, session, collection);
        response.setStatus(e.statusCode);
    }

    private boolean execCreationHook(com.github.onsdigital.zebedee.content.page.base.Page page, String uri,
                                     Collection collection, Session session) {
        info().data("path", uri).data("collection_id", collection.getDescription().getId()).data("user",
                session.getEmail())
                .log("page create endpoint: executing PageCreationHook");
        try {
            pageCreationHook.get().onPageUpdated(page, uri);
            return true;
        } catch (IOException | RuntimeException e) {
            error().data("collection_id", collection.getDescription().getId())
                    .data("user", session.getEmail())
                    .data("path", uri)
                    .logException(e, "page create endpoint: exception when calling page creation hook");
            return false;
        }
    }

    private boolean execDeletionHook(com.github.onsdigital.zebedee.content.page.base.Page page, String uri,
                                     Collection collection, Session session) {
        info().data("path", uri).data("collection_id", collection.getDescription().getId()).data("user",
                session.getEmail())
                .log("page delete endpoint: executing PageDeletionHook");
        try {
            pageDeletionHook.get().onPageUpdated(page, uri);
            return true;
        } catch (IOException | RuntimeException e) {
            error().data("collection_id", collection.getDescription().getId())
                    .data("user", session.getEmail())
                    .data("path", uri)
                    .logException(e, "page delete endpoint: exception when calling page deletion hook");
            return false;
        }
    }

    static String trimZebedeeFileSuffix(String uri) {

        if (uri.endsWith(zebedeeFileSuffix))
            return uri.substring(0, uri.length() - zebedeeFileSuffix.length());

        return uri;
    }
}
