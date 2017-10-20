package com.github.onsdigital.zebedee.dataset.api;

import com.github.onsdigital.zebedee.dataset.api.exception.DatasetAPIException;
import com.github.onsdigital.zebedee.dataset.api.model.Dataset;
import com.github.onsdigital.zebedee.dataset.api.model.DatasetVersion;
import com.github.onsdigital.zebedee.dataset.api.model.Instance;

import java.io.IOException;

public interface DatasetClient {

    /**
     * Get the dataset for the given dataset ID.
     */
    Dataset getDataset(String datasetID)  throws IOException, DatasetAPIException;

    /**
     * Get a particular version of a dataset.
     */
    DatasetVersion getDatasetVersion(String datasetID, String edition, String version) throws IOException, DatasetAPIException;

    /**
     * Get the instance for the given instance ID.
     */
    Instance getInstance(String instanceID)  throws IOException, DatasetAPIException;

    /**
     * Update the dataset for the given dataset ID with the given dataset instance data.
     */
    Dataset updateDataset(String datasetID, Dataset dataset) throws IOException, DatasetAPIException;

    /**
     * Update the dataset version
     */
    Dataset updateDatasetVersion(String datasetID, String edition, String version, DatasetVersion datasetVersion) throws IOException, DatasetAPIException;
}
