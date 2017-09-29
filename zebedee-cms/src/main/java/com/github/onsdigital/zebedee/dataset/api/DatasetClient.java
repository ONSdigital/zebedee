package com.github.onsdigital.zebedee.dataset.api;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;

import java.io.IOException;

public interface DatasetClient {

    /**
     * Get the dataset for the given dataset ID.
     *
     * @param datasetID
     * @return
     */
    Dataset getDataset(String datasetID) throws IOException, BadRequestException;

    /**
     * Get the instance for the given instance ID.
     *
     * @param instanceID
     * @return
     */
    Instance getInstance(String instanceID) throws IOException, BadRequestException;
}
