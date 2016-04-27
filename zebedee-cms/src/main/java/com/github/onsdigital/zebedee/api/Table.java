package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.TableModifications;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.XlsToHtmlConverter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

@Api
public class Table {

    @POST
    public void createTable(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ParserConfigurationException, TransformerException, ZebedeeException {

        Session session = Root.zebedee.sessions.get(request);
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        String uri = request.getParameter("uri");
        CollectionReader collectionReader = new ZebedeeCollectionReader(Root.zebedee, collection, session);

        // Requested path
        if (StringUtils.isBlank(uri)) {
            throw new BadRequestException("Please provide a URI");
        }

        // Path
        Path path = collection.find(uri);
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

        Resource resource = collectionReader.getResource(uri);
        Node table = XlsToHtmlConverter.convertToHtmlPageWithModifications(resource.getData(), getTableModifications(request));
        String output = XlsToHtmlConverter.docToString(table);

        // Write the file to the response
        org.apache.commons.io.IOUtils.copy(new StringReader(output),
                response.getOutputStream());

        Audit.Event.COLLECTION_TABLE_CREATED
                .parameters()
                .host(request)
                .collection(collection)
                .user(session.email)
                .log();
    }

    private TableModifications getTableModifications(HttpServletRequest request) throws IOException {
        com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table previewTable =
                ContentUtil.deserialise(request.getInputStream(),
                        com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table.class);

        if (previewTable != null && previewTable.getModifications().modificationsExist()) {
            return previewTable.getModifications();
        }
        return null;
    }
}
