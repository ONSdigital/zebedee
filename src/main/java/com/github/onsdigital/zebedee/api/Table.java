package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.util.XlsToHtmlConverter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

@Api
public class Table {
    @POST
    public void createTable(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ParserConfigurationException, TransformerException, BadRequestException, NotFoundException, UnauthorizedException {

        Session session = Root.zebedee.sessions.get(request);
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        String uri = request.getParameter("uri");

        // Collection (null check before authorisation check)
        if (collection == null) {
            throw new BadRequestException("Please specify a collection");
        }

        // Authorisation
        if (session == null
                || !Root.zebedee.permissions.canView(session.email,
                collection.description)) {
            throw new UnauthorizedException(session);
        }

        // Requested path
        if (StringUtils.isBlank(uri)) {
            throw new BadRequestException("Please provide a URI");
        }

        // Path
        Path path = collection.find(session.email, uri);
        if (path == null) {
            throw new NotFoundException("URI not found in collection: " + uri);
        }

        // Check we're requesting a file:
        if (Files.isDirectory(path)) {
            throw new BadRequestException("URI does not specify a file");
        }

        // Guess the MIME type
        if (StringUtils.equalsIgnoreCase("json", FilenameUtils.getExtension(path.toString()))) {
            response.setContentType("application/json");
        } else {
            String contentType = Files.probeContentType(path);
            response.setContentType(contentType);
        }

        File xlsFile = new File(uri);
        Document document = XlsToHtmlConverter.convert(path.toFile());

        // When the toString method is called.
        String output = XlsToHtmlConverter.docToString(document);


        // Write the file to the response
        try (InputStream input = Files.newInputStream(path)) {
            org.apache.commons.io.IOUtils.copy(new StringReader(output),
                    response.getOutputStream());
        }
    }
}
