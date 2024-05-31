package com.github.onsdigital.zebedee.api;

import com.github.davidcarboni.restolino.framework.Api;
import com.github.onsdigital.zebedee.content.page.visualisation.Visualisation;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnexpectedErrorException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.SimpleZebedeeResponse;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.session.model.Session;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.commons.io.FileUtils;
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
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

/**
 * Endpoint for unzipping an uploaded data visualisation zip file.
 */
@Api
public class DataVisualisationZip {

    private static final String ZIP_PATH = "zipPath";
    private static final String HTML_EXT = ".html";
    private static final String DATA_JSON_FILE = "/data.json";
    private static final List<String> MAC_OS_ZIP_IGNORE = Arrays.asList("__MACOSX", ".DS_Store");
    private static final String DELETING_ZIP_DEBUG = "Deleting data visualisation zip";
    private static final String DELETING_ZIP_ERROR_DEBUG = "Unexpected error while attempting to delete existing data vis zip content.";
    private static final String UNZIP_DEBUG = "Unpacking data visualisation zip";
    private static final String UNZIP_SUCCESS_DEBUG = "Successfully unzipped data viz file.";
    private static final String UNZIPPING_ERROR_MSG = "Error while trying to unzip Data Visualisation file";
    private static final String COLLECTION_RES_ERROR_MSG = "Could not find the requested collection Resource";
    private static final String NO_ZIP_PATH_ERROR_MSG = "Please specify the zip file path.";
    private static final String UNPACK_ZIP_SUCCESS_MSG = "Visualisation zip unpacked successfully";
    private static final String DATA_VIS_DELETED_SUCCESS_MSG = "The requested data visualisation was deleted or did not exist.";
    private static final String UPDATE_PAGE_JSON_ERROR_MSG = "Unexpected error while updating data visualisation data.json";

    /**
     * {@link Predicate} determining if a {@link ZipEntry} should be written to the collection content dir when unzipping
     * a data visualisation zip file.
     */
    public static final Predicate<ZipEntry> isValidDataVisContentFile = (zipEntry ->
            (!MAC_OS_ZIP_IGNORE.stream().anyMatch(ignoreItem -> zipEntry.getName().contains(ignoreItem)))
                    && !zipEntry.isDirectory());

    public static final SimpleZebedeeResponse unzipSuccessResponse = new SimpleZebedeeResponse(
            UNPACK_ZIP_SUCCESS_MSG, Response.Status.OK);

    public static final SimpleZebedeeResponse deleteContentSuccessResponse = new SimpleZebedeeResponse(
            DATA_VIS_DELETED_SUCCESS_MSG, Response.Status.OK);


    // Use this wrapper class to access static method (cleaner to test).
    private static ZebedeeCmsService zebedeeCmsService = ZebedeeCmsService.getInstance();
    /**
     * Custom {@link IOFileFilter} implementation to filter out only .html files.
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
    /**
     * Function filters returns a {@link Set}  of Html files paths relative to the root directory of the data
     * visualisation zip.
     */
    public static BiFunction<Path, Path, Set<String>> extractHtmlFilenames = (zipEntries, contentRoot) ->
            FileUtils.listFiles(zipEntries.toFile(), htmlFileFilter, TrueFileFilter.TRUE)
                    .stream()
                    .map(file -> zipEntries.relativize(file.toPath()).toString())
                    .collect(Collectors.toSet());

    /**
     * Data Visualisation Zip API method for deleting DV content folder and the original zip file if they exists in the
     * specified collection.
     */
    @DELETE
    public SimpleZebedeeResponse deleteZipAndContent(HttpServletRequest request, HttpServletResponse response)
            throws ZebedeeException {
        String zipPath = request.getParameter(ZIP_PATH);

        if (StringUtils.isEmpty(zipPath)) {
            throw new BadRequestException(NO_ZIP_PATH_ERROR_MSG);
        }

        info().data("zipPath", zipPath).log(DELETING_ZIP_DEBUG);

        Session session = zebedeeCmsService.getSession();
        com.github.onsdigital.zebedee.model.Collection collection = zebedeeCmsService.getCollection(request);

        try {
            collection.deleteDataVisContent(session, Paths.get(zipPath));
        } catch (IOException e) {
            error().data("zipPath", zipPath).logException(e, DELETING_ZIP_ERROR_DEBUG);
            throw new UnexpectedErrorException(DELETING_ZIP_ERROR_DEBUG, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        return deleteContentSuccessResponse;
    }

    /**
     * Data Visualisation Zip API for unzipping a data visualisation zip file.
     * @throws URISyntaxException 
     */
    @POST
    public SimpleZebedeeResponse unpackDataVisualizationZip(HttpServletRequest request, HttpServletResponse response)
            throws ZebedeeException, URISyntaxException {
        String zipPath = request.getParameter(ZIP_PATH);

        if (StringUtils.isEmpty(zipPath)) {
            throw new BadRequestException(NO_ZIP_PATH_ERROR_MSG);
        }

        info().data("zipPath", zipPath).log(UNZIP_DEBUG);

        Session session = zebedeeCmsService.getSession();
        com.github.onsdigital.zebedee.model.Collection collection = zebedeeCmsService.getCollection(request);
        CollectionReader collectionReader = zebedeeCmsService.getZebedeeCollectionReader(collection, session);
        CollectionWriter collectionWriter = zebedeeCmsService.getZebedeeCollectionWriter(collection, session);
        ContentReader publishedContentReader = zebedeeCmsService.getPublishedContentReader();

        try (
                Resource zipRes = collectionReader.getResource(zipPath)
        ) {
            unzipContent(collectionWriter.getInProgress(), zipRes, zipPath);
            updatePageJson(collection, collectionReader, publishedContentReader, collectionWriter, Paths.get(zipPath), session);
            return unzipSuccessResponse;
        } catch (IOException e) {
            error().data("zipPath", zipPath).logException(e, COLLECTION_RES_ERROR_MSG);
            throw new NotFoundException(COLLECTION_RES_ERROR_MSG);
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

            List<String> filesWritten = new ArrayList<>();
            while (zipEntry != null) {

                if (isValidDataVisContentFile.test(zipEntry)) {
                    filePath = zipDir.resolve(zipEntry.getName());
                    contentWriter.write(zipInputStream, filePath.toString());
                    filesWritten.add(filePath.toString());
                }
                zipEntry = zipInputStream.getNextEntry();
            }

            info().data("files", filesWritten).log(UNZIP_SUCCESS_DEBUG);

        } catch (IOException e) {
            error().data("zipPath", zipPath).logException(e, UNZIPPING_ERROR_MSG);
            throw new UnexpectedErrorException(UNZIPPING_ERROR_MSG, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    private void updatePageJson(
            Collection collection, CollectionReader collectionReader, ContentReader publishedContentReader, CollectionWriter collectionWriter,
            Path zipPath, Session session
    ) throws ZebedeeException, URISyntaxException {
        try {
            String dataJsonPath = zipPath.getParent().getParent().toString();
            Visualisation pageJson;
            try {
                pageJson = (Visualisation) collectionReader.getContent(dataJsonPath);
            } catch (ZebedeeException e) {
                pageJson = (Visualisation) publishedContentReader.getContent(dataJsonPath);
            }

            Path zipEntries = Paths.get(collection.getInProgress().getPath().toString() + zipPath.getParent().toString());

            Set<String> files = extractHtmlFilenames.apply(zipEntries, zipPath);
            pageJson.setFilenames(files);
            pageJson.setZipTitle(zipPath.getFileName().toString());

            collectionWriter.getInProgress().writeObject(pageJson, dataJsonPath + DATA_JSON_FILE);

        } catch (IOException e) {
            error().data("user", session.getEmail()).data("zipPath", zipPath.toString())
                    .logException(e, UPDATE_PAGE_JSON_ERROR_MSG);
            throw new UnexpectedErrorException(UPDATE_PAGE_JSON_ERROR_MSG, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }
}
