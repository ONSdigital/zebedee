package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.CollectionDescription;
import com.github.onsdigital.zebedee.json.DirectoryListing;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by david on 10/03/2015.
 */
@Api
public class Browse {

    /**
     * Retrieves a list of content at the endpoint /Browse/[CollectionName]?uri=[uri]
     *
     * @param request This should contain a X-Florence-Token header for the current session
     * @param response <ul>
     *                      <li>If collection doesn't exist:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                      <li>If user hasn't got view permissions:  {@link HttpStatus#UNAUTHORIZED_401}</li>
     *                      <li>If folder exists:  {@link HttpStatus#NOT_FOUND_404}</li>
     *                      <li>If the uri supplied is not to a folder:  {@link HttpStatus#BAD_REQUEST_400}</li>
     *                 </ul>
     * @return DirectoryListing object for the requested uri.
     * @throws IOException
     */
    @GET
    public DirectoryListing browse(HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {

        // Check collection is not null
        Collection collection = Collections.getCollection(request);
        if (collection == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return null;
        }

        // Check view permissions
        Session session = Root.zebedee.sessions.get(request);
        if (Root.zebedee.permissions.canView(session.email, collection.description) == false) {
            response.setStatus(HttpStatus.UNAUTHORIZED_401);
            return null;
        }

        String uri = request.getParameter("uri");
        if (StringUtils.isBlank(uri))
            uri = "/";

        // Locate the path:
        java.nio.file.Path path = getPath(uri, request, response);
        if (path == null) {
            response.setStatus(HttpStatus.NOT_FOUND_404);
            return null;
        }

        // Check we're requesting a directory:
        if (!Files.isDirectory(path)) {
            response.setStatus(HttpStatus.BAD_REQUEST_400);
            return null;
        }

        return listDirectory(path);
    }

    private Path getPath(String uri, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Path path = null;

        Session session = Root.zebedee.sessions.get(request);
        Collection collection = Collections.getCollection(request);
        if (collection != null) {
            path = collection.find(session.email, uri);
        }
        return path;
    }

    private DirectoryListing listDirectory(java.nio.file.Path path)
            throws IOException {

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
}