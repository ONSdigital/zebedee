package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.dataset.api.exception.DatasetAPIException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDatasetVersion;

import java.io.IOException;

/**
 * Provides high level dataset functionality
 */
public interface DatasetService {

    /**
     * Update a dataset for the given ID to the collection for the given collectionID.
     */
    CollectionDataset updateDatasetInCollection(String collectionID, String datasetID, CollectionDataset updatedDataset) throws ZebedeeException, IOException, DatasetAPIException;

    /**
     * Update the dataset version to the collection for the collectionID.
     */
    CollectionDatasetVersion updateDatasetVersionInCollection(String collectionID, String datasetID, String edition, String version, CollectionDatasetVersion updatedVersion) throws ZebedeeException, IOException, DatasetAPIException;

    /**
     * Remove the dataset for the given ID from the collection for the given collectionID.
     */
    void removeDatasetFromCollection(String collectionID, String datasetID) throws ZebedeeException, IOException;

    /**
     * Remove the instance for the given datasetID from the collection for the collectionID.
     */
    void removeDatasetVersionFromCollection(String collectionID, String datasetID, String edition, String version) throws ZebedeeException, IOException;
}
