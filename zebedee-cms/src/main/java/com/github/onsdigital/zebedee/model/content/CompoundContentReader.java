package com.github.onsdigital.zebedee.model.content;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a layered content reader that will read from a list of content readers
 *
 * The final layer added is the first layer checked
 */
public class CompoundContentReader {

    List<ContentReader> contentReaderList = new ArrayList<>();

    public CompoundContentReader() {

    }

    /**
     * Initialise with single contentReader
     *
     * @param contentReader a content reader
     */
    public CompoundContentReader(ContentReader contentReader) {
        contentReaderList.add(contentReader);
    }

    /**
     * Initialise with a collection reader layered over a content reader
     *
     * @param contentReader any base contentReader
     * @param collectionReader a collectionReader to layer over the top
     */
    public CompoundContentReader(ContentReader contentReader, CollectionReader collectionReader) {
        contentReaderList.add(contentReader);
        contentReaderList.add(collectionReader.getInProgress());
        contentReaderList.add(collectionReader.getComplete());
        contentReaderList.add(collectionReader.getReviewed());
    }

    /**
     * Add a ContentReader on the top of our stack
     *
     * @param contentReader any content reader
     * @return this
     */
    public CompoundContentReader add(ContentReader contentReader) {
        contentReaderList.add(0, contentReader);
        return this;
    }

    /**
     * Add all ContentReaders from a CollectionReader to the stack
     *
     * @param collectionReader a collection reader
     * @return this
     */
    public CompoundContentReader add(CollectionReader collectionReader) {
        contentReaderList.add(0, collectionReader.getReviewed());
        contentReaderList.add(0, collectionReader.getComplete());
        contentReaderList.add(0, collectionReader.getInProgress());
        return this;
    }

    /**
     * Get page content using a list of possible content readers
     *
     * @param path a page uri (no /data.json on the end)
     * @return a page
     * @throws NotFoundException if no content is found
     */
    public Page getContent(String path) throws NotFoundException {
        for (ContentReader contentReader: contentReaderList) {
            try {
                return contentReader.getContent(path);
            } catch (ZebedeeException | IOException e) {

            }
        }
        throw new NotFoundException("Content not found");
    }

    /**
     * Get a file resource using a list of possible content readers
     *
     * @param path a uri (includes file name)
     * @return a resource
     * @throws NotFoundException if no content is found
     */
    public Resource getResource(String path) throws NotFoundException {
        for (ContentReader contentReader: contentReaderList) {
            try {
                return contentReader.getResource(path);
            } catch (ZebedeeException | IOException e) {

            }
        }
        throw new NotFoundException("Content not found");
    }
}
