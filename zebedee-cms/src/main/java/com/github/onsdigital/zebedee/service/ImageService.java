package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.dp.image.api.client.exception.ImageAPIException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDatasetVersion;
import com.github.onsdigital.zebedee.model.Collection;
import dp.api.dataset.exception.DatasetAPIException;

import java.io.IOException;

/**
 * Provides high level images functionality
 */
public interface ImageService {

    /**
     * Publish the images contained in the given collection.
     */
    void publishImagesInCollection(Collection collection) throws IOException, ImageAPIException;

}
