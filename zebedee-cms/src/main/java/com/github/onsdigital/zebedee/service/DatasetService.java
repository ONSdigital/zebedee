package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.dataset.api.exception.BadRequestException;
import com.github.onsdigital.zebedee.dataset.api.exception.DatasetNotFoundException;
import com.github.onsdigital.zebedee.dataset.api.exception.UnexpectedResponseException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionInstance;

import javax.management.InstanceNotFoundException;
import java.io.IOException;

public interface DatasetService {

    /**
     * Add an instance for the given instanceID to the collection for the collectionID.
     * @param collectionID
     * @param instanceID
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    CollectionInstance addInstanceToCollection(String collectionID, String instanceID) throws ZebedeeException, IOException, UnexpectedResponseException, InstanceNotFoundException, BadRequestException, DatasetNotFoundException;

    /**
     * Delete the instance for the given instanceID from the collection for the collectionID.
     * @param collectionID
     * @param instanceID
     * @throws ZebedeeException
     * @throws IOException
     */
    void deleteInstanceFromCollection(String collectionID, String instanceID) throws ZebedeeException, IOException;
}
