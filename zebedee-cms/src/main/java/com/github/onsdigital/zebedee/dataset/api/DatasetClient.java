package com.github.onsdigital.zebedee.dataset.api;

import com.github.onsdigital.zebedee.dataset.api.exception.BadRequestException;
import com.github.onsdigital.zebedee.dataset.api.exception.DatasetNotFoundException;
import com.github.onsdigital.zebedee.dataset.api.exception.UnexpectedResponseException;

import javax.management.InstanceNotFoundException;
import java.io.IOException;

public interface DatasetClient {

    /**
     * Get the dataset for the given dataset ID.
     *
     * @param datasetID
     * @return
     */
    Dataset getDataset(String datasetID) throws IOException, DatasetNotFoundException, UnexpectedResponseException, BadRequestException;

    /**
     * Get the instance for the given instance ID.
     *
     * @param instanceID
     * @return
     */
    Instance getInstance(String instanceID) throws IOException, InstanceNotFoundException, UnexpectedResponseException, BadRequestException;

    /**
     * Update the dataset for the given dataset ID with the given dataset instance data.
     * @param datasetID
     * @param dataset
     */
    Dataset updateDataset(String datasetID, Dataset dataset) throws BadRequestException, IOException, DatasetNotFoundException, UnexpectedResponseException;
}
