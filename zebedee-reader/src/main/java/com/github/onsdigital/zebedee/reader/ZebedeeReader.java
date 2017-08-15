package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ResourceDirectoryNotFileException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.data.filter.DataFilter;
import com.github.onsdigital.zebedee.reader.data.filter.FilterUtil;
import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.github.onsdigital.zebedee.logging.ZebedeeReaderLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getConfiguration;

/**
 * Created by bren on 29/07/15.
 * <p>
 * Service to read published content and contents going through process in collections
 */
public class ZebedeeReader {

    private static final String VISUALISATION_DIR_MSG = "Requested resource is a visualisation directory. Attempting to find index.html";
    private static final String VISUALISATIONS_PATH = "/visualisations/";
    private static final String INDEX_HTML = "index.html";

    /**
     * If Zebedee Reader is running standalone, no reader factory registered, thus no collection reads are allowed
     */
    private static CollectionReaderFactory collectionReaderFactory;
    private ContentReader publishedContentReader;
    private ContentLanguage language;

    public ZebedeeReader() {
        this(null);
    }

    public ZebedeeReader(ContentLanguage language) {
        publishedContentReader = new FileSystemContentReader(getConfiguration().getContentDir());
        publishedContentReader.setLanguage(language);
        this.language = language;
    }

    public ZebedeeReader(String rootFolder, ContentLanguage language) {
        publishedContentReader = new FileSystemContentReader(rootFolder);
        this.language = language;
    }

    public static CollectionReaderFactory getCollectionReaderFactory() {
        return collectionReaderFactory;
    }

    public static void setCollectionReaderFactory(CollectionReaderFactory collectionReaderFactory) {
        ZebedeeReader.collectionReaderFactory = collectionReaderFactory;
    }

    /**
     * @param path path can start with / or not, Zebedee reader will evaluate the path relative to published contents root
     * @return Requested published content
     * @throws ZebedeeException
     * @throws IOException
     */
    public Page getPublishedContent(String path) throws ZebedeeException, IOException {
        return publishedContentReader.getContent(path);
    }

    /**
     * @param path   path can start with / or not, Zebedee reader will evaluate the path relative to published contents root
     * @param filter data filter to be applied
     * @return Requested published content
     * @throws ZebedeeException
     * @throws IOException
     */
    public Content getPublishedContent(String path, DataFilter filter) throws ZebedeeException, IOException {
        Content content = getPublishedContent(path);
        return FilterUtil.filterPageData(content, filter);
    }

    /**
     * Finds requested content under given collection. If content not found under given collection it will not return published content, but will throw NotFoundException.
     * Use getPublishedContent to get published content.
     * <p>
     * Zebedee reader does not make any authentication check, make sure requesting client is authorized to read from given collection
     *
     * @param collectionId Id of the collection to find requested content in
     * @param path         path can start with / or not, Zebedee reader will evaluate the path relative to collection contents root
     * @return
     * @throws ZebedeeException
     * @throws com.github.onsdigital.zebedee.exceptions.NotFoundException
     * @throws IOException
     */
    public Content getCollectionContent(String collectionId, String sessionId, String path) throws ZebedeeException, IOException {
        assertId(collectionId);
        return createCollectionReader(collectionId, sessionId).getContent(path);
    }

    /**
     * Finds requested content under given collection. If content not found under given collection it will not return published content, but will throw NotFoundException.
     * Use getPublishedContent to get published content.
     * <p>
     * Zebedee reader does not make any authentication check, make sure requesting client is authorized to read from given collection
     *
     * @param collectionId Id of the collection to find requested content in
     * @param path         path can start with / or not, Zebedee reader will evaluate the path relative to collection contents root
     * @param filter       data filter to be applied
     * @return
     * @throws ZebedeeException
     * @throws com.github.onsdigital.zebedee.exceptions.NotFoundException
     * @throws IOException
     */
    public Content getCollectionContent(String collectionId, String sessionId, String path, DataFilter filter) throws ZebedeeException, IOException {
        Content collectionContent = getCollectionContent(collectionId, sessionId, path);
        return FilterUtil.filterPageData(collectionContent, filter);
    }

    /**
     * @param path path can start with / or not, Zebedee reader will evaluate the path relative to published contents root
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    public Resource getPublishedResource(String path) throws ZebedeeException, IOException {
        try {
            return publishedContentReader.getResource(path);
        } catch (BadRequestException e) {
            Path res = Paths.get(path);
            if (e instanceof ResourceDirectoryNotFileException && res.startsWith(VISUALISATIONS_PATH)) {
                logInfo(VISUALISATION_DIR_MSG)
                        .uri(path)
                        .log();
                return publishedContentReader.getResource(res.resolve(INDEX_HTML).toString());
            }
            throw e;
        }
    }

    /**
     * Finds requested resource under given collection. If content not found under given collection it will not return published resource, but will throw NotFoundException.
     * Use getPublishedContent to get published content.
     * <p>
     * Zebedee reader does not make any authentication check, make sure requesting client is authorized to read from given collection
     *
     * @param collectionId
     * @param path         path can start with / or not, Zebedee reader will evaluate the path relative to collection contents root
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    public Resource getCollectionResource(String collectionId, String sessionId, String path) throws ZebedeeException, IOException {
        assertId(collectionId);
        return createCollectionReader(collectionId, sessionId).getResource(path);
    }

    public long getCollectionContentLength(String collectionId, String sessionId, String path) throws ZebedeeException, IOException {
        assertId(collectionId);
        return createCollectionReader(collectionId, sessionId).getContentLength(path);
    }

    public long getPublishedContentLength(String path) throws ZebedeeException, IOException {
        return publishedContentReader.getContentLength(path);
    }

    public Map<URI, ContentNode> getPublishedContentChildren(String path) throws ZebedeeException, IOException {
        try {
            return publishedContentReader.getChildren(path);
        } catch (NotFoundException e) {
            //If requested path is not available in published content return an empty list
            return new HashMap<>();
        }
    }


    public Map<URI, ContentNode> getCollectionContentChildren(String collectionId, String sessionId, String path) throws ZebedeeException, IOException {
        return createCollectionReader(collectionId, sessionId).getChildren(path);
    }

    public Map<URI, ContentNode> getPublishedContentParents(String path) throws ZebedeeException, IOException {
        return publishedContentReader.getParents(path);
    }

    public Map<URI, ContentNode> getCollectionContentParents(String collectionId, String sessionId, String path) throws IOException, ZebedeeException {
        return createCollectionReader(collectionId, sessionId).getParents(path);
    }

    public Content getLatestPublishedContent(String uri) throws ZebedeeException, IOException {
        return getLatestPublishedContent(uri, null);
    }

    public Content getLatestPublishedContent(String uri, DataFilter dataFilter) throws ZebedeeException, IOException {
        Page content = publishedContentReader.getLatestContent(uri);
        return FilterUtil.filterPageData(content, dataFilter);
    }

    public Content getLatestCollectionContent(String collectionId, String sessionId, String uri) throws IOException, ZebedeeException {
        return getLatestCollectionContent(collectionId, sessionId, uri, null);
    }

    public Content getLatestCollectionContent(String collectionId, String sessionId, String uri, DataFilter dataFilter) throws IOException, ZebedeeException {
        Page content = createCollectionReader(collectionId, sessionId).getLatestContent(uri);
        return FilterUtil.filterPageData(content, dataFilter);
    }

    private void assertId(String collectionId) throws BadRequestException {
        if (collectionId == null) {
            throw new BadRequestException("Collection Id must be supplied");
        }
    }


    private CollectionReader createCollectionReader(String collectionId, String sessionId) throws NotFoundException, IOException, UnauthorizedException, BadRequestException {
        if (collectionReaderFactory == null) {
            throw new UnauthorizedException("Collection reads are not available");
        }

        CollectionReader collectionContentReader = collectionReaderFactory.createCollectionReader(collectionId, sessionId);
        collectionContentReader.setLanguage(language);
        return collectionContentReader;
    }
}
