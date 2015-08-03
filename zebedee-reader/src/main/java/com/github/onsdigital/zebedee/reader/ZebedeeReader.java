package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.reader.util.CollectionContentReader;
import com.github.onsdigital.zebedee.reader.util.ContentReader;

import java.io.IOException;

import static com.github.onsdigital.zebedee.util.URIUtils.*;

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
        publishedContentReader = new ContentReader(ReaderConfiguration.getInstance().getContentDir());
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
    public Page getPublishedContent(String path) throws ZebedeeException, IOException {
        return publishedContentReader.getContent(removeLeadingSlash(path));
    }



    /**
     * Finds requested content under given collection. If content not found under given collection it will not return published content, but will throw NotFoundException.
     * Use getPublishedContent to get published content.
     * <p>
     * Zebedee reader does not make any authentication check, make sure requesting client is authorized to read from given collection
     *
     * @param collectionId Id of the collection to find requested content in
     * @param path           path can start with / or not, Zebedee reader will evaluate the path relative to collection contents root
     * @return
     * @throws ZebedeeException
     * @throws com.github.onsdigital.zebedee.exceptions.NotFoundException
     * @throws IOException
     */
    public Page getCollectionContent(String collectionId, String path) throws ZebedeeException, IOException {
        CollectionContentReader collectionReader = createCollectionReader(collectionId);
        return collectionReader.getContent(removeLeadingSlash(path));
    }

    /**
     * @param path path can start with / or not, Zebedee reader will evaluate the path relative to published contents root
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    public Resource getPublishedResource(String path) throws ZebedeeException, IOException {
        return publishedContentReader.getResource(removeLeadingSlash(path));
    }

    /**
     * Finds requested resource under given collection. If content not found under given collection it will not return published content, but will throw NotFoundException.
     * Use getPublishedContent to get published content.
     * <p>
     * Zebedee reader does not make any authentication check, make sure requesting client is authorized to read from given collection
     *
     * @param collectionId
     * @param path           path can start with / or not, Zebedee reader will evaluate the path relative to collection contents root
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    public Resource getCollectionResource(String collectionId, String path) throws ZebedeeException, IOException {
        CollectionContentReader collectionReader = createCollectionReader(collectionId);
        return collectionReader.getResource(removeLeadingSlash(path));
    }

    private CollectionContentReader createCollectionReader(String collectionId) throws BadRequestException {
        if (collectionId == null) {
            throw new BadRequestException("Collection Id must be supplied");
        }
        return new CollectionContentReader(ReaderConfiguration.getInstance().getCollectionsFolder(), collectionId);
    }

}
