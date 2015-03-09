package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.helpers.Path;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.ChangeSet;
import com.github.onsdigital.zebedee.json.DirectoryListing;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Api
public class Browse {

    /**
     * Enables you to browse the site, or a specific changeset.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @GET
    public DirectoryListing browse(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {

        String uri = request.getParameter("path");

        if (StringUtils.isBlank(uri)) {

            Path requestPath = Path.newInstance(request);
            List<String> segments = new ArrayList<>(requestPath.segments());

            // Remove the API name:
            if (segments.size() > 0) {
                segments.remove(0);
            }

            // Remove the changeset ID, if present:
            if (segments.size() > 0 && segments.get(0).matches("[0-9]+")) {
                segments.remove(0);
            }

            // Build the URI:
            StringBuilder uriBuilder = new StringBuilder();
            for (String segment : segments) {
                uriBuilder.append("/");
                uriBuilder.append(segment);
            }
            if (uriBuilder.length() == 0) {
                uriBuilder.append("/");
            }
            uri = uriBuilder.toString();
        }

        // Locate the path:
        java.nio.file.Path path;
        ChangeSet changeSet = ChangeSets.getChangeSet(request);
        if (changeSet != null) {
            path = changeSet.find(uri);
        } else {
            path = Root.zebedee.find(uri);
        }
        if (path == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return null;
        }

        // Do the right thing:
        if (Files.isDirectory(path)) {
            return listDirectory(path);
        } else {
            returnFile(path, response);
            return null;
        }

    }

    private DirectoryListing listDirectory(java.nio.file.Path path) throws IOException {

        // Get the directory listing:
        DirectoryListing listing = new DirectoryListing();
        try (DirectoryStream<java.nio.file.Path> stream = Files
                .newDirectoryStream(path)) {
            for (java.nio.file.Path directory : stream) {
                // Recursively delete directories only:
                if (Files.isDirectory(directory)) {
                    listing.folders.put(directory.getFileName().toString(),
                            directory.toString());
                } else {
                    listing.files.put(directory.getFileName().toString(),
                            directory.toString());
                }
            }
        }
        Serialiser.getBuilder().setPrettyPrinting();
        return listing;
    }

    private void returnFile(java.nio.file.Path path, HttpServletResponse response)
            throws IOException {
        response.setContentType(Files.probeContentType(path));
        try (InputStream input = Files.newInputStream(path);
             OutputStream output = response.getOutputStream()) {
            IOUtils.copy(input, output);
        }
    }
}
