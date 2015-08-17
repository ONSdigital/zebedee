package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.CollectionNotFoundException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.data.filter.DataFilter;
import com.github.onsdigital.zebedee.reader.data.filter.FilterUtil;
import com.github.onsdigital.zebedee.reader.util.CollectionContentReader;
import com.github.onsdigital.zebedee.reader.util.ContentReader;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getConfiguration;

/**
 * Created by bren on 29/07/15.
 * <p>
 * Service to read published content and contents going through process in collections
 */
public class ZebedeeReader {
    private static ZebedeeReader instance;
    private static ContentReader publishedContentReader;


    //Singleton
    private ZebedeeReader() {
        publishedContentReader = new ContentReader(getConfiguration().getContentDir());
    }

    public static ZebedeeReader getInstance() {
        if (instance == null) {
            synchronized (ZebedeeReader.class) {
                if (instance == null) {
                    instance = new ZebedeeReader();
                }
            }
        }
        return instance;
    }


    /**
     * @param path path can start with / or not, Zebedee reader will evaluate the path relative to published contents root
     * @return Requested published content
     * @throws ZebedeeException
     * @throws IOException
     */
    public Content getPublishedContent(String path) throws ZebedeeException, IOException {
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
    public Content getCollectionContent(String collectionId, String path) throws ZebedeeException, IOException {
        assertId(collectionId);
        return createCollectionReader(collectionId).getContent(path);
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
    public Content getCollectionContent(String collectionId, String path, DataFilter filter) throws ZebedeeException, IOException {
        Content collectionContent = getCollectionContent(collectionId, path);
        return FilterUtil.filterPageData(collectionContent, filter);
    }

    /**
     * @param path path can start with / or not, Zebedee reader will evaluate the path relative to published contents root
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    public Resource getPublishedResource(String path) throws ZebedeeException, IOException {
        return publishedContentReader.getResource(path);
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
    public Resource getCollectionResource(String collectionId, String path) throws ZebedeeException, IOException {
        assertId(collectionId);
        return createCollectionReader(collectionId).getResource(path);
    }

    public Map<URI, ContentNode> getPublishedContentChildren(String path) throws ZebedeeException, IOException {
        try {
            return publishedContentReader.getChildren(path);
        } catch (NotFoundException e) {
            //If requested path is not available in published content return an empty list
            return Collections.emptyMap();
        }
    }


    public Map<URI, ContentNode> getCollectionContentChildren(String collectionId, String path) throws ZebedeeException, IOException {
        return createCollectionReader(collectionId).getChildren(path);
    }

    public Map<URI, ContentNode> getPublishedContentParents(String path) throws ZebedeeException, IOException {
        return publishedContentReader.getParents(path);
    }


    public Map<URI, ContentNode> getCollectionContentParents(String collectionId, String path) throws IOException, ZebedeeException {
        return createCollectionReader(collectionId).getParents(path);
    }


    public Content getLatestPublishedContent(String uri) throws ZebedeeException, IOException {
        return getLatestPublishedContent(uri, null);
    }

    public Content getLatestPublishedContent(String uri, DataFilter dataFilter) throws ZebedeeException, IOException {
        Page content = publishedContentReader.getLatestContent(uri);
        return FilterUtil.filterPageData(content, dataFilter);
    }

    public Content getLatestCollectionContent(String collectionId, String uri) throws IOException, ZebedeeException {
        return getLatestCollectionContent(collectionId, uri, null);
    }

    public Content getLatestCollectionContent(String collectionId, String uri, DataFilter dataFilter) throws IOException, ZebedeeException {
        Page content = createCollectionReader(collectionId).getLatestContent(uri);
        return FilterUtil.filterPageData(content, dataFilter);
    }

    private void assertId(String collectionId) throws BadRequestException {
        if (collectionId == null) {
            throw new BadRequestException("Collection Id must be supplied");
        }
    }


    private CollectionContentReader createCollectionReader(String collectionId) throws NotFoundException, IOException, CollectionNotFoundException {
        return new CollectionContentReader(getConfiguration().getCollectionsFolder(), collectionId);
    }

}
