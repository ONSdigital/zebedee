package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.*;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.ZebedeeApiHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;

/**
 * Endpoint for unzipping an uploaded data visualisation zip file.
 */
@Api
public class UnzipDataVisualisation {

    private static ZebedeeApiHelper zebedeeApiHelper = ZebedeeApiHelper.getInstance();

    private static final String ZIP_PATH = "zipPath";
    private static final String COLLECTION_RES_ERROR_MSG = "Could not find the requested collection Resource";
    private static final String UNZIPPING_ERROR_MSG = "Error while trying to unzip Data Visualisation file";
    private static final String NO_ZIP_PATH_ERROR_MSG = "Please specify the zip file path.";

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

        removeExisting(collection, session, Paths.get(zipPath).getParent());

        Resource zipRes;
        try {
            zipRes = collectionReader.getResource(zipPath);
        } catch (IOException e) {
            logError(e, COLLECTION_RES_ERROR_MSG).path(zipPath).log();
            throw new NotFoundException(COLLECTION_RES_ERROR_MSG);
        }

        unzipContent(collectionWriter.getInProgress(), zipRes, zipPath);
    }

    private void removeExisting(com.github.onsdigital.zebedee.model.Collection collection, Session session, Path zipDir) throws ZebedeeException {
        if (Files.exists(zipDir)) {
            try {
                collection.deleteContent(session.email, zipDir.toString());
            } catch (IOException e) {
                logError(e, "Unexpected error while attempting to delete existing data vis zip content.")
                        .logAndThrow(UnexpectedErrorException.class);
            }
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

                // TODO zipEntry.isDirectory() takes care of this.
                if (FilenameUtils.getExtension(filePath.toString()).length() > 0) {
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
}
