package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.dataset.api.Dataset;
import com.github.onsdigital.zebedee.dataset.api.DatasetClient;
import com.github.onsdigital.zebedee.dataset.api.exception.BadRequestException;
import com.github.onsdigital.zebedee.dataset.api.exception.DatasetNotFoundException;
import com.github.onsdigital.zebedee.dataset.api.exception.UnexpectedResponseException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDatasetVersion;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import org.apache.commons.lang3.StringUtils;

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
     * Add the dataset for the given datasetID to the collection for the collectionID.
     */
    @Override
    public CollectionDataset updateDatasetInCollection(String collectionID, String datasetID, CollectionDataset updatedDataset) throws ZebedeeException, IOException, UnexpectedResponseException, DatasetNotFoundException, BadRequestException {

        Collection collection = zebedeeCms.getCollection(collectionID);

        CollectionDataset collectionDataset;

        Optional<CollectionDataset> existingDataset = collection.getDescription().getDataset(datasetID);
        if (existingDataset.isPresent()) {
            collectionDataset = existingDataset.get();
        } else {
            collectionDataset = new CollectionDataset();
            collectionDataset.setId(datasetID);
        }

        if (updatedDataset != null && StringUtils.isNotEmpty(updatedDataset.getState())) {
            collectionDataset.setState(updatedDataset.getState());
        }

        Dataset dataset = datasetClient.getDataset(datasetID);
        collectionDataset.setTitle(dataset.getTitle());

        collection.getDescription().addDataset(collectionDataset);
        collection.save();

        return collectionDataset;
    }


    /**
     * Add the dataset version to the collection for the collectionID.
     */
    @Override
    public CollectionDatasetVersion updateDatasetVersionInCollection(String collectionID, String datasetID, String edition, String version, CollectionDatasetVersion updatedVersion) throws ZebedeeException, IOException, UnexpectedResponseException, DatasetNotFoundException, BadRequestException {

        Collection collection = zebedeeCms.getCollection(collectionID);
        CollectionDatasetVersion datasetVersion;

        // if its already been added return.
        Optional<CollectionDatasetVersion> existing =
                collection.getDescription().getDatasetVersion(datasetID, edition, version);

        if (existing.isPresent()) {
            datasetVersion = existing.get();
        } else {
            datasetVersion = new CollectionDatasetVersion();
            datasetVersion.setId(datasetID);
            datasetVersion.setEdition(edition);
            datasetVersion.setVersion(version);
        }

        if (updatedVersion != null && StringUtils.isNotEmpty(updatedVersion.getState())) {
            datasetVersion.setState(updatedVersion.getState());
        }

        Dataset dataset = datasetClient.getDataset(datasetID);
        datasetVersion.setTitle(dataset.getTitle());

        collection.getDescription().addDatasetVersion(datasetVersion);
        collection.save();

        return datasetVersion;
    }

    /**
     * Remove the instance for the given datasetID from the collection for the collectionID.
     */
    @Override
    public void removeDatasetFromCollection(String collectionID, String datasetID) throws ZebedeeException, IOException {

        Collection collection = zebedeeCms.getCollection(collectionID);

        // if its already been added return.
        Optional<CollectionDataset> existingDataset = collection.getDescription().getDataset(datasetID);
        if (!existingDataset.isPresent()) {
            return;
        }

        collection.getDescription().removeDataset(existingDataset.get());
        collection.save();
    }

    /**
     * Remove the instance for the given datasetID from the collection for the collectionID.
     */
    @Override
    public void removeDatasetVersionFromCollection(String collectionID, String datasetID, String edition, String version) throws ZebedeeException, IOException {

        Collection collection = zebedeeCms.getCollection(collectionID);

        // if its already been added return.
        Optional<CollectionDatasetVersion> existingDataset =
                collection.getDescription().getDatasetVersion(datasetID, edition, version);

        if (!existingDataset.isPresent()) {
            return;
        }

        collection.getDescription().removeDatasetVersion(existingDataset.get());
        collection.save();
    }
}
