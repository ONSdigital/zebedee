package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.encryptedfileupload.EncryptedFileItemFactory;
import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.util.ReaderResponseResponseUtils;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_FILE_SAVED;
import static com.github.onsdigital.zebedee.persistence.CollectionEventType.COLLECTION_PAGE_SAVED;

@Api
public class Content {

    private static final String DATA_JSON = "data.json";

    /**
     * Retrieves file content for the endpoint <code>/Content/[CollectionName]/?uri=[uri]</code>
     * <p/>
     * <p>This may be working content from the collection. Defaults to current website content</p>
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response No respons message.
     * @return
     * @throws IOException           If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     * @throws NotFoundException     If the requested URI does not exist in the collection.
     * @throws BadRequestException   IF the request cannot be completed because of a problem with request parameters
     * @throws UnauthorizedException If the user does not have viewer permission.
     */
    @GET
    public void read(HttpServletRequest request, HttpServletResponse response) throws IOException, ZebedeeException {
        try (Resource resource = RequestUtils.getResource(request)) {
            ReaderResponseResponseUtils.sendResponse(resource, response);
        }
    }


    /**
     * Posts file content to the endpoint <code>/Content/[CollectionName]/?uri=[uri]</code>
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     *                 <ul>Body should contain
     *                 <li>Page content - JSON Serialized content</li>
     *                 <li>File Upload - A multipart content object with part "file" as binary data </li>
     *                 </ul>
     * @param response Returns true or false according to whether the URI was written.
     * @return true/false
     * @throws IOException           If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     * @throws BadRequestException   IF the request cannot be completed because of a problem with request parameters
     * @throws UnauthorizedException If the user does not have publisher permission.
     * @throws ConflictException     If the URI is being edited in another collection
     * @throws NotFoundException     If the file cannot be edited for some other reason
     */
    @POST
    public boolean saveContent(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ZebedeeException {

        // We have to get the request InputStream before reading any request parameters
        // otherwise the call to get a request parameter will actually consume the body:
        InputStream requestBody = request.getInputStream();

        Session session = Root.zebedee.getSessionsService().get(request);

        Collection collection = Collections.getCollection(request);

        String uri = request.getParameter("uri");
        Boolean overwriteExisting = BooleanUtils.toBoolean(StringUtils.defaultIfBlank(request.getParameter("overwriteExisting"), "true"));
        Boolean recursive = BooleanUtils.toBoolean(StringUtils.defaultIfBlank(request.getParameter("recursive"), "false"));
        CollectionEventType eventType = getEventType(Paths.get(uri));
        Boolean validateJson = BooleanUtils.toBoolean(StringUtils.defaultIfBlank(request.getParameter("validateJson"), "true"));

        if (ServletFileUpload.isMultipartContent(request)) {
            return handleMultipartUpload(request, session, collection, uri, recursive, eventType);
        }

        if (validateJson) {
            requestBody = validateJsonStream(requestBody);
        }

        if (overwriteExisting) {
            Root.zebedee.getCollections().writeContent(collection, uri, session, requestBody, recursive, eventType);
            Audit.Event.CONTENT_OVERWRITTEN
                    .parameters()
                    .host(request)
                    .collection(collection)
                    .content(uri)
                    .user(session.getEmail())
                    .log();
        } else {
            Root.zebedee.getCollections().createContent(collection, uri, session, requestBody, eventType);
            Audit.Event.CONTENT_SAVED
                    .parameters()
                    .host(request)
                    .collection(collection)
                    .content(uri)
                    .user(session.getEmail())
                    .log();
        }

        return true;
    }

    private boolean handleMultipartUpload(HttpServletRequest request, Session session, Collection collection, String uri, Boolean recursive, CollectionEventType eventType) throws IOException {
        ServletFileUpload upload = getServletFileUpload();

        try {
            for (FileItem item : upload.parseRequest(request)) {
                try (InputStream inputStream = item.getInputStream()) {
                    Root.zebedee.getCollections().writeContent(collection, uri, session, inputStream, recursive, eventType);
                    Audit.Event.CONTENT_OVERWRITTEN
                            .parameters()
                            .host(request)
                            .collection(collection)
                            .content(uri)
                            .user(session.getEmail())
                            .log();
                }
            }
        } catch (Exception e) {
            throw new IOException("Error processing uploaded file", e);
        }

        return true;
    }

    /**
     * Take an input stream that contains json content and ensure its valid.
     *
     * @param inputStream
     * @return
     */
    public InputStream validateJsonStream(InputStream inputStream) throws BadRequestException {
        try {
            byte[] bytes = IOUtils.toByteArray(inputStream);
            inputStream.close();

            try (ByteArrayInputStream validationInputStream = new ByteArrayInputStream(bytes)) {
                ContentUtil.deserialiseContent(validationInputStream);
            }

            return new ByteArrayInputStream(bytes);
        } catch (Exception e) {
            throw new BadRequestException("Validation of page content failed. Please try again");
        }
    }

    /**
     * get a file upload object with progress listener
     *
     * @return an upload object
     */
    public static ServletFileUpload getServletFileUpload() {
        // Set up the objects that do all the heavy lifting
        // PrintWriter out = response.getWriter();
        EncryptedFileItemFactory factory = new EncryptedFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);

        ProgressListener progressListener = getProgressListener();
        upload.setProgressListener(progressListener);
        return upload;
    }

    /**
     * get a progress listener
     *
     * @return a ProgressListener object
     */
    private static ProgressListener getProgressListener() {
        // Set up a progress listener that we can use to power a progress bar
        return new ProgressListener() {
            private long megaBytes = -1;

            @Override
            public void update(long pBytesRead, long pContentLength, int pItems) {
                long mBytes = pBytesRead / 1000000;
                if (megaBytes == mBytes) {
                    return;
                }
                megaBytes = mBytes;
            }
        };
    }

    /**
     * Deletes file content from the endpoint <code>/Content/[CollectionName]/?uri=[uri]</code>
     *
     * @param request  This should contain a X-Florence-Token header for the current session
     * @param response Returns true or false according to whether the URI was deleted.
     * @return
     * @throws IOException           If an error occurs in processing data, typically to the filesystem, but also on the HTTP connection.
     * @throws BadRequestException   IF the request cannot be completed because of a problem with request parameters
     * @throws NotFoundException     If the requested URI does not exist in the collection.
     * @throws UnauthorizedException If the user does not have publisher permission.
     * @throws ConflictException     If the URI is being edited in another collection
     */
    @DELETE
    public boolean delete(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ZebedeeException {

        Session session = Root.zebedee.getSessionsService().get(request);

        Collection collection = Collections.getCollection(request);
        String uri = request.getParameter("uri");

        boolean result = Root.zebedee.getCollections().deleteContent(collection, uri, session);
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

    private CollectionEventType getEventType(Path uri) {
        return DATA_JSON.equals(uri.getFileName().toString()) ? COLLECTION_PAGE_SAVED : COLLECTION_FILE_SAVED;
    }
}
