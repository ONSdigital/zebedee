package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionContentReader;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.reader.api.endpoint.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by thomasridd on 1/16/16.
 */
public class DataPublicationFinder {
    public List<DataPublication> findPublications(CollectionContentReader collectionReader, Collection collection) throws IOException, ZebedeeException {

        List<DataPublication> results = new ArrayList<>();

        // Loop through the uri's in the collection
        for(String reviewedUri: collection.reviewedUris()) {

            // Ignoring previous versions loop through the pages
            if (!reviewedUri.toLowerCase().contains("/previous/") && reviewedUri.toLowerCase().endsWith("data.json")) {

                // Find all timeseries_datasets
                Page page = collectionReader.getContent(reviewedUri);
                if (page.getType() == PageType.timeseries_dataset) {
                    DataPublication newPublication = new DataPublication(collectionReader, reviewedUri);
                    results.add(newPublication);
                }
            }
        }
        return results;
    }
}
