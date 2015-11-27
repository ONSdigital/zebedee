package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.util.CollectionReader;

import java.io.IOException;

/**
 * Creates instances of CollectionReader.
 */
public class ZebedeeCollectionReaderFactory {

    private Zebedee zebedee;

    public ZebedeeCollectionReaderFactory(Zebedee zebedee) {
        this.zebedee = zebedee;
    }

    public CollectionReader createCollectionReader(String collectionId) throws NotFoundException, IOException {

        Collection collection = zebedee.collections.list().getCollection(collectionId);
        return new ZebedeeCollectionReader(zebedee, collection);
    }
}
