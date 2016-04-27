package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.audit.Audit;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.TableModifications;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.TableBuilderException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.util.RequestUtils;
import com.github.onsdigital.zebedee.util.XlsToHtmlConverter;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.onsdigital.zebedee.content.util.ContentUtil.deserialiseContent;
import static com.github.onsdigital.zebedee.exceptions.TableBuilderException.ErrorType.NEGATIVE_ROW_INDEX;
import static com.github.onsdigital.zebedee.exceptions.TableBuilderException.ErrorType.NUMBER_PARSE_ERROR;
import static com.github.onsdigital.zebedee.exceptions.TableBuilderException.ErrorType.UNEXPECTED_ERROR;

/**
 * Created by dave on 4/15/16.
 */
@Api
public class ModifyTable {

    private static final String HTML_FILE_EXT = ".html";
    private static final String JSON_FILE_EXT = ".json";
    private static final String XLS_FILE_EXT = ".xls";
    private static final String CURRENT_URI = "currentUri";
    private static final String NEW_URI = "newUri";

    /**
     * Update/Create XLS table metadata - i.e rows to be excluded from the generated HTML table.
     */
    @POST
    public void modifyTable(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ZebedeeException, ParserConfigurationException, TransformerException, FileUploadException {

        Session session = Root.zebedee.sessions.get(request);
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);
        CollectionReader collectionReader = new ZebedeeCollectionReader(Root.zebedee, collection, session);

        String currentUri = request.getParameter(CURRENT_URI);
        String newUri = request.getParameter(NEW_URI);

        com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table newTable =
                ContentUtil.deserialise(request.getInputStream(),
                        com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table.class);

        validate(response, collection, currentUri + XLS_FILE_EXT);

        com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table tableJson
                = getCurrentTable(request, collectionReader, currentUri + JSON_FILE_EXT);
        tableJson.setModifications(newTable.getModifications());

        String htmlTableStr = generateXlsTable(request, collectionReader, currentUri + XLS_FILE_EXT, tableJson.getModifications());

        writeData(request, session, collectionReader, collection, currentUri, newUri, htmlTableStr, tableJson);
        response.setStatus(Response.Status.CREATED.getStatusCode());
        Audit.Event.COLLECTION_TABLE_METADATA_MODIFIED
                .parameters()
                .host(request)
                .collection(collection)
                .user(session.email)
                .log();
    }

    /**
     * Get the current Table metadata.
     */
    @GET
    public void getTableMetadata(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ZebedeeException {
        Session session = Root.zebedee.sessions.get(request);
        com.github.onsdigital.zebedee.model.Collection collection = Collections.getCollection(request);

        CollectionReader collectionReader = new ZebedeeCollectionReader(Root.zebedee, collection, session);

        String uri = request.getParameter("uri");
        com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table table = getCurrentTable(request, collectionReader, uri);
        IOUtils.copy(toInputStream(table), response.getOutputStream());
    }

    private void writeData(HttpServletRequest request, Session session, CollectionReader collectionReader, Collection collection,
                           String currentUri, String newUri, String htmlTableStr, Page tableJson) throws TableBuilderException {
        try (
                Resource currentXlsResource = getResource(request, collectionReader, currentUri + XLS_FILE_EXT);
                InputStream htmlInputStream = toInputStream(htmlTableStr);
                InputStream jsonInputStream = toInputStream(tableJson)
        ) {
            String newXlsFilename = Paths.get(newUri + XLS_FILE_EXT).getFileName().toString();
            String currentXlsFilename = Paths.get(currentUri + XLS_FILE_EXT).getFileName().toString();

            if (!StringUtils.equals(newXlsFilename, currentXlsFilename)) {
                Root.zebedee.collections.writeContent(collection, newUri + XLS_FILE_EXT, session, request,
                        currentXlsResource.getData());
            }

            Root.zebedee.collections.writeContent(collection, newUri + HTML_FILE_EXT, session, request, htmlInputStream);
            Root.zebedee.collections.writeContent(collection, newUri + JSON_FILE_EXT, session, request, jsonInputStream);
        } catch (Exception ex) {
            throw new TableBuilderException(UNEXPECTED_ERROR, ex.getMessage());
        }
    }

    private void validate(HttpServletResponse response, Collection collection, String uri) throws BadRequestException,
            NotFoundException, IOException {
        if (StringUtils.isBlank(uri)) {
            throw new BadRequestException("Please provide a URI");
        }

        Path path = collection.find(uri);
        if (path == null) {
            throw new NotFoundException("URI not found in collection: " + uri);
        }

        if (Files.isDirectory(path)) {
            throw new BadRequestException("URI does not specify a file");
        }

        if (StringUtils.equalsIgnoreCase("json", FilenameUtils.getExtension(path.toString()))) {
            response.setContentType("application/json");
        } else {
            String contentType = Files.probeContentType(path);
            response.setContentType(contentType);
        }
    }

    private String generateXlsTable(HttpServletRequest request, CollectionReader collectionReader, String resourceUri,
                                    TableModifications modifications) throws ZebedeeException, IOException,
            TransformerException, ParserConfigurationException {

        try (Resource currentXlsResource = collectionReader.getResource(resourceUri)) {
            return generateModifiedTable(currentXlsResource.getData(), modifications);
        } catch (Exception ex) {
            // Ignore error and try to get published content.
        }

        try (Resource publishedResource = RequestUtils.getZebedeeReader(request).getPublishedResource(resourceUri)) {
            return generateModifiedTable(publishedResource.getData(), modifications);
        }
    }

    private String generateModifiedTable(InputStream inputStream, TableModifications modifications)
            throws ParserConfigurationException, TableBuilderException, IOException, TransformerException {

        if (modifications == null || modifications.getRowsExcluded().isEmpty()
                && modifications.getHeaderColumns().isEmpty() && modifications.getHeaderRows().isEmpty()) {
            modifications = null;
        }
        Node updatedHtmlTable = XlsToHtmlConverter.convertToHtmlPageWithModifications(inputStream, modifications);
        return XlsToHtmlConverter.docToString(updatedHtmlTable);
    }

    private InputStream toInputStream(Page page) {
        return new ByteArrayInputStream(ContentUtil.serialise(page).getBytes());
    }

    private InputStream toInputStream(String value) {
        return new ByteArrayInputStream(value.getBytes());
    }

    private com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table getCurrentTable(
            HttpServletRequest request, CollectionReader collectionReader, String uri)
            throws ZebedeeException, IOException {
        com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table table;

        table = (com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table)
                deserialiseContent(getResource(request, collectionReader, uri).getData());
        return table;
    }

    private Resource getResource(HttpServletRequest request, CollectionReader collectionReader, String resourceUri)
            throws ZebedeeException, IOException {
        try {
            return collectionReader.getResource(resourceUri);
        } catch (ZebedeeException | IOException ex) {
            // Ignore and try to get published content.
        }
        return RequestUtils.getZebedeeReader(request).getPublishedResource(resourceUri);
    }

    private com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table getNewTable(HttpServletRequest request) throws TableBuilderException, IOException {
        return ContentUtil.deserialise(request.getInputStream(), com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table.class);
    }

    private List<Integer> getParam(HttpServletRequest request, String paramName) throws TableBuilderException {
        Set<Integer> exclusionsSet = new HashSet<>();
        String raw = request.getParameter(paramName);

        if (StringUtils.isNotEmpty(raw)) {
            for (String data : raw.split(",")) {
                exclusionsSet.add(parseAndValidate(data));
            }
        }
        List<Integer> exclusionsList = new ArrayList<>(exclusionsSet);
        java.util.Collections.sort(exclusionsList);
        return exclusionsList;
    }

    private int parseAndValidate(String raw) throws TableBuilderException {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < 0) {
                throw new TableBuilderException(NEGATIVE_ROW_INDEX);
            }
            return value;
        } catch (NumberFormatException nfx) {
            throw new TableBuilderException(NUMBER_PARSE_ERROR, raw);
        }
    }
}
