package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataPublicationFinder {

    /**
     * Find all pages in a collection that need to be published using the data publisher
     *
     * @param publishedContentReader a content reader for published content
     * @param reviewedContentReader a content reader for the collection reviewed content
     *
     * @return a list of DataPublication objects
     * @throws IOException
     * @throws ZebedeeException
     */
    public List<DataPublication> findPublications(ContentReader publishedContentReader, ContentReader reviewedContentReader) throws IOException, ZebedeeException {

        List<DataPublication> results = new ArrayList<>();

        // Loop through the uri's in the collection
        for (String reviewedUri : reviewedContentReader.listUris()) {

            // Ignoring previous versions loop through the pages
            if (!reviewedUri.toLowerCase().contains("/previous/") && reviewedUri.toLowerCase().endsWith("data.json")) {

                // Strip off data.json
                String pageUri = reviewedUri.substring(0, reviewedUri.length() - "/data.json".length());

                // Find all timeseries_datasets
                Page page = reviewedContentReader.getContent(pageUri);
                if (page != null && page.getType() == PageType.timeseries_dataset) {
                    DataPublication newPublication = new DataPublication(publishedContentReader, reviewedContentReader, pageUri);
                    results.add(newPublication);
                }
            }
        }
        return results;
    }
}
