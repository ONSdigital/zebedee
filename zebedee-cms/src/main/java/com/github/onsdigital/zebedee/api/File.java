package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.configuration.Configuration.getUnauthorizedMessage;

/**
 * Created by bren on 01/07/15.
 * <p>
 * Starts download for requested file in content directory
 */
@Api
public class File {

    @GET
    public Object post(@Context HttpServletRequest request, @Context HttpServletResponse response) throws IOException, NotFoundException, UnauthorizedException, BadRequestException {
        String uri = request.getParameter("uri");
        if (StringUtils.isEmpty(uri)) {
            throw new IllegalArgumentException("File uri not supplied");
        }
        Path file = getFile(uri, request, response);
        response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getFileName() + "\"");
        response.setContentType(Files.probeContentType(file));
        IOUtils.copy(Files.newInputStream(file), response.getOutputStream());
        return null;
    }

    private Path getFile(String uriString, HttpServletRequest request, HttpServletResponse response)
            throws IOException, NotFoundException, UnauthorizedException, BadRequestException {
        // Standardise the path:
        String uriPath = StringUtils.removeStart(uriString, "/");
        System.out.println("Reading file under" + uriPath);

        Collection collection = Collections.getCollection(request);
        Session session = Root.zebedee.sessions.get(request);

        // Authorisation
        if (session == null
                || !Root.zebedee.permissions.canView(session.email,
                collection.description)) {
            throw new UnauthorizedException(getUnauthorizedMessage(session));
        }

        // Path
        Path path = collection.find(uriPath);
        if (path == null) {
            throw new NotFoundException("URI not found in collection: " + uriPath);
        }

        // Check we're requesting a file:
        if (Files.isDirectory(path)) {
            throw new BadRequestException("URI does not specify a file");
        }

        return path;
    }
}
