package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.content.page.visualisation.Visualisation;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.ZebedeeApiHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Endpoint for unzipping an uploaded data visualisation zip file.
 */
@Api
public class DataVisualisationZip {

    private static final String ZIP_PATH = "zipPath";
    private static final String COLLECTION_RES_ERROR_MSG = "Could not find the requested collection Resource";
    private static final String UNZIPPING_ERROR_MSG = "Error while trying to unzip Data Visualisation file";
    private static final String NO_ZIP_PATH_ERROR_MSG = "Please specify the zip file path.";
    private static final String HTML_EXT = ".html";
    private static ZebedeeApiHelper zebedeeApiHelper = ZebedeeApiHelper.getInstance();
    /**
     * Custom {@link IOFileFilter} implementation for .html files.
     */
    private static IOFileFilter htmlFileFilter = new IOFileFilter() {
        @Override
        public boolean accept(File file) {
            if (file == null) {
                return false;
            }
            return file.getName().toLowerCase().endsWith(HTML_EXT);
        }

        @Override
        public boolean accept(File dir, String name) {
            return StringUtils.isNotEmpty(name) && name.toLowerCase().endsWith(HTML_EXT);
        }
    };

    public static BiFunction<Path, Path, Set<String>> extractHtmlFilenames = (zipEntries, contentRoot) ->
            FileUtils.listFiles(zipEntries.toFile(), htmlFileFilter, TrueFileFilter.TRUE)
                    .stream()
                    .map(file -> zipEntries.relativize(file.toPath()).toString())
                    .collect(Collectors.toSet());

    /**
     * Delete Data Vis content and zip file if it exists in the specified collection.
     */
    @DELETE
    public void deleteZipAndContent(HttpServletRequest request, HttpServletResponse response) throws ZebedeeException {
        String zipPath = request.getParameter(ZIP_PATH);

        if (StringUtils.isEmpty(zipPath)) {
            throw new BadRequestException(NO_ZIP_PATH_ERROR_MSG);
        }

        logDebug("Deleting data visualisation zip").path(zipPath).log();

        Session session = zebedeeApiHelper.getSession(request);
        com.github.onsdigital.zebedee.model.Collection collection = zebedeeApiHelper.getCollection(request);

        try {
            collection.deleteDataVisContent(session.email, Paths.get(zipPath));
        } catch (IOException e) {
            logError(e, "Unexpected error while attempting to delete existing data vis zip content.")
                    .path(zipPath)
                    .logAndThrow(UnexpectedErrorException.class);
        }
    }

    @POST
    public void unpackDataVisualizationZip(HttpServletRequest request, HttpServletResponse response)
            throws ZebedeeException {
        String zipPath = request.getParameter(ZIP_PATH);

        if (StringUtils.isEmpty(zipPath)) {
            throw new BadRequestException(NO_ZIP_PATH_ERROR_MSG);
        }

        logDebug("Unpacking data visualisation zip").path(zipPath).log();

        Session session = zebedeeApiHelper.getSession(request);
        com.github.onsdigital.zebedee.model.Collection collection = zebedeeApiHelper.getCollection(request);
        CollectionReader collectionReader = zebedeeApiHelper.getZebedeeCollectionReader(collection, session);
        CollectionWriter collectionWriter = zebedeeApiHelper.getZebedeeCollectionWriter(collection, session);

        Resource zipRes;
        try {
            zipRes = collectionReader.getResource(zipPath);
        } catch (IOException e) {
            logError(e, COLLECTION_RES_ERROR_MSG).path(zipPath).log();
            throw new NotFoundException(COLLECTION_RES_ERROR_MSG);
        }

        unzipContent(collectionWriter.getInProgress(), zipRes, zipPath);
        updatePageJson(collection, collectionReader, collectionWriter, Paths.get(zipPath), session);

        //TODO Dave to replace - from Crispin
        try {
            Map<String, String> successResponse = new HashMap<>();
            successResponse.put("success", "true");

            IOUtils.copy(zebedeeApiHelper.objectAsInputStream(successResponse), response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unzip the specified resource.
     */
    private void unzipContent(ContentWriter contentWriter, Resource zipRes, String zipPath) throws ZebedeeException {
        Path zipDir = Paths.get(zipPath);
        zipDir = Paths.get(zipDir.getParent().toString());

        try (ZipInputStream zipInputStream = new ZipInputStream(zipRes.getData())) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            Path filePath;

            while (zipEntry != null) {
                filePath = zipDir.resolve(zipEntry.getName());

                if (!zipEntry.isDirectory()) {
                    contentWriter.write(zipInputStream, filePath.toString());

                    logDebug("Successfully unzipped data viz file.")
                            .addParameter("file", filePath.toString())
                            .log();
                }

                zipEntry = zipInputStream.getNextEntry();
            }

        } catch (IOException e) {
            logError(e, UNZIPPING_ERROR_MSG).path(zipPath).log();
            throw new UnexpectedErrorException(UNZIPPING_ERROR_MSG, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    private void updatePageJson(Collection collection, CollectionReader collectionReader, CollectionWriter collectionWriter,
                                Path zipPath, Session session) throws ZebedeeException {
        try {
            String dataJsonPath = zipPath.getParent().getParent().toString();
            Visualisation pageJson = (Visualisation) collectionReader.getContent(dataJsonPath);

            Path zipEntries = Paths.get(collection.getInProgress().getPath().toString() + zipPath.getParent().toString());
            //Path contentRoot = zipPath.getParent();

            pageJson.setFilenames(extractHtmlFilenames.apply(zipEntries, zipPath));

            collectionWriter.getInProgress().writeObject(pageJson, dataJsonPath + "/data.json");

        } catch (IOException e) {
            logError(e, "Unexpected error while updating data visualisation data.json")
                    .user(session.email)
                    .path(zipPath.toString())
                    .logAndThrow(UnexpectedErrorException.class);
        }
    }
}
