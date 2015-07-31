package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.reader.util.CollectionContentReader;
import com.github.onsdigital.zebedee.reader.util.ContentReader;

import java.io.IOException;

import static com.github.onsdigital.zebedee.reader.util.URIUtils.*;

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
        publishedContentReader = new ContentReader(ReaderConfiguration.getPublishedFolderName());
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
        return publishedContentReader.getContent(removeForwardSlash(path));
    }

    /**
     * Finds requested content under given collection. If content not found under given collection it will not return published content, but will throw NotFoundException.
     * Use getPublishedContent to get published content.
     * <p>
     * Zebedee reader does not make any authentication check, make sure requesting client is authorized to read from given collection
     *
     * @param collectionName Name of the collection to find requested content in
     * @param path           path can start with / or not, Zebedee reader will evaluate the path relative to collection contents root
     * @return
     * @throws ZebedeeException
     * @throws com.github.onsdigital.zebedee.exceptions.NotFoundException
     * @throws IOException
     */
    public Content getCollectionContent(String collectionName, String path) throws ZebedeeException, IOException {
        CollectionContentReader collectionReader = createCollectionReader(collectionName);
        return collectionReader.getContent(removeForwardSlash(path));
    }

    /**
     * @param path path can start with / or not, Zebedee reader will evaluate the path relative to published contents root
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    public Resource getPublishedResource(String path) throws ZebedeeException, IOException {
        return publishedContentReader.getResource(removeForwardSlash(path));
    }

    /**
     * Finds requested resource under given collection. If content not found under given collection it will not return published content, but will throw NotFoundException.
     * Use getPublishedContent to get published content.
     * <p>
     * Zebedee reader does not make any authentication check, make sure requesting client is authorized to read from given collection
     *
     * @param collectionName
     * @param path           path can start with / or not, Zebedee reader will evaluate the path relative to collection contents root
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    public Resource getCollectionResource(String collectionName, String path) throws ZebedeeException, IOException {
        CollectionContentReader collectionReader = createCollectionReader(collectionName);
        return collectionReader.getResource(removeForwardSlash(path));
    }

    private CollectionContentReader createCollectionReader(String collectionName) {
        if (collectionName == null) {
            throw new NullPointerException("Connection name can not be null");
        }
        String path = ReaderConfiguration.getCollectionsFolder() + "/" + collectionName;
        return new CollectionContentReader(removeForwardSlash(path));
    }


}
