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
     *
     * @param publishedContentReader reader
     * @param reviewedContentReader
     * @param collectionContentWriter
     * @param collection
     * @throws IOException
     * @throws ZebedeeException
     * @throws URISyntaxException
     */
    public void preprocessCollection(CollectionContentReader publishedContentReader, CollectionContentReader reviewedContentReader, CollectionContentWriter collectionContentWriter, Collection collection) throws IOException, ZebedeeException, URISyntaxException {

        // Find all files that need data preprocessing
        List<DataPublication> dataPublications = new DataPublicationFinder().findPublications(publishedContentReader, reviewedContentReader, collection);

        // For each file in this collection
        for(DataPublication dataPublication: dataPublications) {
            dataPublication.process(publishedContentReader, reviewedContentReader, collectionContentWriter);
        }
    }
}
