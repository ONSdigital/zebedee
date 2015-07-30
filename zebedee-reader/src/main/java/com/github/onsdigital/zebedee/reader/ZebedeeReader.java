package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.util.ContentReader;

import java.io.IOException;

/**
 * Created by bren on 29/07/15.
 */
public class ZebedeeReader {
    private static ZebedeeReader instance;

    private static ContentReader publishedContentReader;

    //Singleton
    private ZebedeeReader() {
        publishedContentReader = new ContentReader(ReaderConfiguration.getPublishedFolder());
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


    public Content getPublishedContent(String path) throws ZebedeeException, IOException {
        return publishedContentReader.getContent(path);
    }

    public Content getCollectionContent(String path) throws ZebedeeException, IOException {
        return null;
    }

    public Resource getPublishedResource(String path) throws ZebedeeException, IOException {
        return null;
    }

}
