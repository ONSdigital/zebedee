package com.github.onsdigital.zebedee.data;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.data.processing.DataPublication;
import com.github.onsdigital.zebedee.data.processing.DataPublicationFinder;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionContentReader;
import com.github.onsdigital.zebedee.model.CollectionContentWriter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class DataPublisherReloaded {

    /**
     * Process a collection ready for data publication
     *
     * @param collectionReader
     * @param collectionWriter
     * @param zebedee
     * @param collection
     * @param session
     * @throws IOException
     * @throws ZebedeeException
     * @throws URISyntaxException
     */
    public void preprocessCollection(CollectionContentReader collectionReader, CollectionContentWriter collectionWriter, Zebedee zebedee, Collection collection, Session session) throws IOException, ZebedeeException, URISyntaxException {

        // Find all files that need  in the collection
        List<DataPublication> dataPublications = new DataPublicationFinder().findPublications(collectionReader, collection);

        // For each file in this collection
        for(DataPublication dataPublication: dataPublications) {
            dataPublication.process(collectionReader, collectionWriter);
        }
    }
}
