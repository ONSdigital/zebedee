package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.persistence.CollectionEventType;
import com.github.onsdigital.zebedee.service.MathjaxEquationService;
import org.apache.commons.fileupload.FileUploadException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Given some Tex equation input, return the equation in SVG format.
 */
@Api
public class Equation {

    @POST
    public boolean renderEquation(
            HttpServletRequest request,
            HttpServletResponse response,
            com.github.onsdigital.zebedee.content.page.statistics.document.figure.equation.Equation equation
    ) throws IOException, ZebedeeException, FileUploadException {

        InputStream requestBody = request.getInputStream();
        Session session = Root.zebedee.sessions.get(request);
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        String uri = request.getParameter("uri");

        // write the equation json to the collection
        String serialisedEquation = ContentUtil.serialise(equation);
        try (InputStream inputStream = new ByteArrayInputStream(serialisedEquation.getBytes())) {
            boolean validateJson = true;
            Root.zebedee.collections.writeContent(collection, uri, session, request, inputStream, false, CollectionEventType.COLLECTION_FILE_SAVED, validateJson);
            Audit.Event.CONTENT_OVERWRITTEN
                    .parameters()
                    .host(request)
                    .collection(collection)
                    .content(uri)
                    .user(session.email).log();
        }

        // determine the SVG path based on the json path.
        Path path = Paths.get(uri);
        String svgUri = path.getParent().resolve(equation.getFilename() + ".svg").toString();
        System.out.println("svgUri = " + svgUri);

        String svgOuput = MathjaxEquationService.render(equation.getContent());
        try (InputStream inputStream = new ByteArrayInputStream(svgOuput.getBytes())) {
            boolean validateJson = false;
            Root.zebedee.collections.writeContent(collection, svgUri, session, request, inputStream, false, CollectionEventType.COLLECTION_FILE_SAVED, validateJson);
        }

        return true;
    }
}