package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDatasetVersion;
import com.github.onsdigital.zebedee.model.Collection;
import dp.api.dataset.exception.DatasetAPIException;

import java.io.IOException;

/**
 * Provides high level dataset functionality
 */
public interface DatasetService {

    /**
     * Update a dataset for the given ID to the collection for the given collectionID.
     */
    CollectionDataset updateDatasetInCollection(Collection collection, String datasetID, CollectionDataset updatedDataset, String user) throws ZebedeeException, IOException, DatasetAPIException;

    /**
     * Update the dataset version to the collection for the collectionID.
     */
    CollectionDatasetVersion updateDatasetVersionInCollection(Collection collection, String datasetID, String edition, String version, CollectionDatasetVersion updatedVersion, String user) throws ZebedeeException, IOException, DatasetAPIException;

    /**
     * Remove the dataset for the given ID from the collection for the given collectionID.
     */
    void removeDatasetFromCollection(Collection collection, String datasetID) throws ZebedeeException, IOException, DatasetAPIException;

    /**
     * Remove the instance for the given datasetID from the collection for the collectionID.
     */
    void removeDatasetVersionFromCollection(Collection collection, String datasetID, String edition, String version) throws ZebedeeException, IOException, DatasetAPIException;
}
