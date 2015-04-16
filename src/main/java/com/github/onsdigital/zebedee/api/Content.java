package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by david on 10/03/2015.
 */

@Api
public class Content {

    @GET
    public void read(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String uri = request.getParameter("uri");
        if (StringUtils.isBlank(uri)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return;
        }

        java.nio.file.Path path = null;
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        if (collection != null) {
            Session session = Root.zebedee.sessions.get(request);
            path = collection.find(session.email, uri);
        }

        if (path == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return;
        }

        // Check we're requesting a file:
        if (!java.nio.file.Files.exists(path)) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return;
        }

        // Check we're requesting a file:
        if (java.nio.file.Files.isDirectory(path)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return;
        }

        // Guess the MIME type
        String contentType = Files.probeContentType(path);
        if(contentType!=null) { response.setContentType(contentType); }

        // Write the file to the response
        try (InputStream input = java.nio.file.Files.newInputStream(path)) {
            org.apache.commons.io.IOUtils.copy(input, response.getOutputStream());
        }
    }

    @POST
    public boolean write(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // We have to get the request InputStream before reading any request parameters
        // otherwise the call to get a request parameter will actually consume the body:
        InputStream requestBody = request.getInputStream();

        String uri = request.getParameter("uri");

        if (StringUtils.isBlank(uri))
            uri = "/";

        // Check the user has edit permission
        Session session = Root.zebedee.sessions.get(request);
        if (!Root.zebedee.permissions.canEdit(session.email)) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return false;
        }

        // Find the collection if it exists
        java.nio.file.Path path = null;
        Collection collection = Collections.getCollection(request);
        if (collection != null) {
            path = collection.find(session.email, uri); // see if the file exists anywhere.
        } else {
            // attempting to access non-existent collection
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return false;
        }

        if (path == null) {
            // create the file
            boolean result = collection.create(session.email, uri);
            if (!result) {
                // file is being edited in a different collection
                response.setStatus(HttpStatus.CONFLICT_409);
                return false;
            }
        } else {
            // edit the file
            boolean result = collection.edit(session.email, uri);
            if (!result) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
                return false;
            }
        }

        if (collection != null) {
            collection.save();
            path = collection.getInProgressPath(uri);
        }

        if (!java.nio.file.Files.exists(path)) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return false;
        }

        // Check we're requesting a file:
        if (java.nio.file.Files.isDirectory(path)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return false;
        }


        // Detect whether this is a multipart request
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if(isMultipart) { // If it is we are going to do an xls/csv file upload
            try {
                postDataFile(request, response, path);
            } catch (Exception e) {

            }
        } else { // If it isn't we are going to be doing a straightforward content update
            try (OutputStream output = java.nio.file.Files.newOutputStream(path)) {
                org.apache.commons.io.IOUtils.copy(requestBody, output);
            }
        }
        return true;
    }

    void postDataFile(HttpServletRequest request, HttpServletResponse response, Path path) throws Exception {

        // Set up the objects that do all the heavy lifting
        //PrintWriter out = response.getWriter();
        DiskFileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);

        // Set up a progress listener that we can use to power a progress bar
        ProgressListener progressListener = new ProgressListener(){
            private long megaBytes = -1;
            public void update(long pBytesRead, long pContentLength, int pItems) {
                long mBytes = pBytesRead / 1000000;
                if (megaBytes == mBytes) {
                    return;
                }
                megaBytes = mBytes;
            }
        };
        upload.setProgressListener(progressListener);

        // Read the items - this will save the values to temp files
        List<FileItem> items = upload.parseRequest(request);

        // Process the items
        for(FileItem item: items) {
            item.write(path.toFile());
        }
    }

    @DELETE
    public void delete(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String uri = request.getParameter("uri");
        if (StringUtils.isBlank(uri)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return;
        }

        // Get the collection
        java.nio.file.Path path = null;
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        if (collection != null) {
            Session session = Root.zebedee.sessions.get(request);
            path = collection.find(session.email, uri);
        }

        // Check the user has access to the given file
        if (path == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return;
        }

        // Check the file we are requesting exists:
        if (!collection.isInCollection(uri)) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return;
        }

        // Delete the file
        if( collection.deleteContent(uri) ) {
            response.setStatus(HttpStatus.OK_200);
        } else {
            response.setStatus(HttpStatus.EXPECTATION_FAILED_417);
        }
        return;
    }
}
