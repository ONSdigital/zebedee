package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
        com.github.onsdigital.zebedee.Collection collection = Collections.getCollection(request);
        if (collection != null) {
            path = collection.find(uri);
        }

        if (path == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return;
        }

        // Check we're requesting a file:
        if (java.nio.file.Files.isDirectory(path)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return;
        }

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


        java.nio.file.Path path = null;
        com.github.onsdigital.zebedee.Collection collection = Collections.getCollection(request);
        if (collection != null) {
            path = collection.find(uri); // see if the file exists anywhere.
        }

        if (path == null) {
            // create the file
            boolean result = collection.create(uri);
            if (!result) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
            }
        } else {
            // edit the file
            boolean result = collection.edit(uri);
            if (!result) {
                response.setStatus(HttpStatus.BAD_REQUEST_400);
            }
        }

        if (collection != null) {
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

        try (OutputStream output = java.nio.file.Files.newOutputStream(path)) {
            org.apache.commons.io.IOUtils.copy(requestBody, output);
        }

        return true;
    }
}
