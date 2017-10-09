package com.github.onsdigital.zebedee.dataset.api;

import com.github.onsdigital.zebedee.dataset.api.exception.BadRequestException;
import com.github.onsdigital.zebedee.dataset.api.exception.DatasetNotFoundException;
import com.github.onsdigital.zebedee.dataset.api.exception.UnexpectedResponseException;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
     * Update the dataset for the given dataset ID with the given json content.
     * @param datasetID The ID of the dataset to update
     * @param datasetJson An input stream containing dataset data in json format.
     * @return
     */
    String updateDataset(String datasetID, InputStream datasetJson) throws BadRequestException, IOException, DatasetNotFoundException, UnexpectedResponseException;
}
