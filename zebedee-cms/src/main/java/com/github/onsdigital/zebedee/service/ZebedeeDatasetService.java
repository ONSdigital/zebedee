package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.dataset.api.Dataset;
import com.github.onsdigital.zebedee.dataset.api.DatasetClient;
import com.github.onsdigital.zebedee.dataset.api.Instance;
import com.github.onsdigital.zebedee.dataset.api.exception.BadRequestException;
import com.github.onsdigital.zebedee.dataset.api.exception.DatasetNotFoundException;
import com.github.onsdigital.zebedee.dataset.api.exception.UnexpectedResponseException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionInstance;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;

import javax.management.InstanceNotFoundException;
import java.io.IOException;
import java.util.Optional;

/**
 * Dataset related services
 */
public class ZebedeeDatasetService implements DatasetService {

    private DatasetClient datasetClient;
    private ZebedeeCmsService zebedeeCms;

    public ZebedeeDatasetService(DatasetClient datasetClient, ZebedeeCmsService zebedeeCms) {
        this.datasetClient = datasetClient;
        this.zebedeeCms = zebedeeCms;
    }

    /**
     * Add an instance for the given instanceID to the collection for the collectionID.
     *
     * @param collectionID
     * @param instanceID
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    @Override
    public CollectionInstance addInstanceToCollection(String collectionID, String instanceID) throws ZebedeeException, IOException, UnexpectedResponseException, InstanceNotFoundException, BadRequestException, DatasetNotFoundException {

        Collection collection = zebedeeCms.getCollection(collectionID);

        // if its already been added return.
        Optional<CollectionInstance> existingInstance = collection.getInstance(instanceID);
        if (existingInstance.isPresent()) {
            return existingInstance.get();
        }

        // Get the instance only to determine the dataset ID.
        Instance instance = datasetClient.getInstance(instanceID);
        String datasetID = instance.getLinks().dataset.getId();
        Dataset dataset = datasetClient.getDataset(datasetID);

        CollectionInstance collectionInstance = mapToCollectionInstance(instance, dataset);

        collection.addInstance(collectionInstance);
        collection.save();

        return collectionInstance;

    }

    // take the instance and dataset model from the dataset API and map the values onto the CollectionInstance model.
    private CollectionInstance mapToCollectionInstance(Instance instance, Dataset dataset) {

        CollectionInstance collectionInstance =
                new CollectionInstance();

        collectionInstance.setId(instance.getId());
        collectionInstance.setEdition(instance.getEdition());
        collectionInstance.setVersion(instance.getVersion());

        CollectionInstance.CollectionDataset instanceDataset =
                new CollectionInstance.CollectionDataset();

        instanceDataset.id = dataset.getId();
        instanceDataset.title = dataset.getTitle();
        instanceDataset.href = dataset.getUri();

        collectionInstance.setDataset(instanceDataset);
        return collectionInstance;
    }

    /**
     * Delete the instance for the given instanceID from the collection for the collectionID.
     *
     * @param collectionID
     * @param instanceID
     * @throws ZebedeeException
     * @throws IOException
     */
    @Override
    public void deleteInstanceFromCollection(String collectionID, String instanceID) throws ZebedeeException, IOException {

        Collection collection = zebedeeCms.getCollection(collectionID);

        // if its already been added return.
        Optional<CollectionInstance> existingInstance = collection.getInstance(instanceID);
        if (!existingInstance.isPresent()) {
            return;
        }

        collection.deleteInstance(existingInstance.get());
        collection.save();
    }
}
