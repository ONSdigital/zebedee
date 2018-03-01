package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDatasetVersion;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import dp.api.dataset.DatasetClient;
import dp.api.dataset.exception.DatasetAPIException;
import dp.api.dataset.model.Dataset;
import dp.api.dataset.model.DatasetVersion;
import dp.api.dataset.model.State;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.Optional;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

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
    public CollectionDataset updateDatasetInCollection(String collectionID, String datasetID, CollectionDataset updatedDataset) throws ZebedeeException, IOException, DatasetAPIException {

        Collection collection = zebedeeCms.getCollection(collectionID);
        if (collection == null) {
            logInfo("Collection not found").collectionId(collectionID).logAndThrow(NotFoundException.class);
        }

        CollectionDataset collectionDataset;

        Optional<CollectionDataset> existingDataset = collection.getDescription().getDataset(datasetID);
        if (existingDataset.isPresent()) {
            collectionDataset = existingDataset.get();
        } else {
            collectionDataset = new CollectionDataset();
            collectionDataset.setId(datasetID);
        }

        if (updatedDataset != null && updatedDataset.getState() != null) {
            collectionDataset.setState(updatedDataset.getState());
        }

        Dataset dataset = datasetClient.getDataset(datasetID);

        if (dataset != null) {

            collectionDataset.setTitle(dataset.getTitle());

            if (dataset.getLinks() != null && dataset.getLinks().getSelf() != null) {
                collectionDataset.setUri(dataset.getLinks().getSelf().getHref());
            } else {
                logInfo("The dataset URL has not been set on the dataset response.")
                        .addParameter("collectionID", collectionID)
                        .addParameter("datasetID", datasetID)
                        .log();

                throw new InvalidObjectException("The dataset URL has not been set on the dataset response.");
            }

            if (State.CREATED.equals(dataset.getState())) {
                // the dataset has just been added to the collection, so update collection ID on the dataset
                Dataset datasetUpdate = new Dataset();
                datasetUpdate.setCollection_id(collectionID);
                datasetUpdate.setState(State.ASSOCIATED);
                datasetClient.updateDataset(datasetID, datasetUpdate);
            }

            if (dataset.getState().equals(State.ASSOCIATED)
                    && !dataset.getCollection_id().equals(collectionID)) {
                throw new ConflictException("cannot add dataset " + datasetID
                        + " to collection " + collectionID
                        + " it is already in collection " + dataset.getCollection_id());
            }

            collection.getDescription().addDataset(collectionDataset);
            collection.save();
        }

        return collectionDataset;
    }


    /**
     * Add the dataset version to the collection for the collectionID.
     */
    @Override
    public CollectionDatasetVersion updateDatasetVersionInCollection(String collectionID, String datasetID, String edition, String version, CollectionDatasetVersion updatedVersion) throws ZebedeeException, IOException, DatasetAPIException {

        Collection collection = zebedeeCms.getCollection(collectionID);
        if (collection == null) {
            logInfo("Collection not found").collectionId(collectionID).logAndThrow(NotFoundException.class);
        }

        CollectionDatasetVersion collectionDatasetVersion;

        // if its already been added return.
        Optional<CollectionDatasetVersion> existing =
                collection.getDescription().getDatasetVersion(datasetID, edition, version);

        if (existing.isPresent()) {
            collectionDatasetVersion = existing.get();
        } else {
            collectionDatasetVersion = new CollectionDatasetVersion();
            collectionDatasetVersion.setId(datasetID);
            collectionDatasetVersion.setEdition(edition);
            collectionDatasetVersion.setVersion(version);
        }

        if (updatedVersion != null && updatedVersion.getState() != null) {
            collectionDatasetVersion.setState(updatedVersion.getState());
        }

        Dataset dataset = datasetClient.getDataset(datasetID);
        DatasetVersion datasetVersion = datasetClient.getDatasetVersion(datasetID, edition, version);

        if (State.EDITION_CONFIRMED.equals(datasetVersion.getState())) {
            // the dataset version has just been added to the collection, so update collection ID and set state
            DatasetVersion versionUpdate = new DatasetVersion();
            versionUpdate.setCollection_id(collectionID);
            versionUpdate.setState(State.ASSOCIATED);
            datasetClient.updateDatasetVersion(datasetID, edition, version, versionUpdate);
        }

        if (State.ASSOCIATED.equals(datasetVersion.getState())
                && !datasetVersion.getCollection_id().equals(collectionID)) {
            throw new ConflictException("cannot add dataset " + datasetID
                    + " to collection " + collectionID
                    + " it is already in collection " + dataset.getCollection_id());
        }

        if (datasetVersion.getLinks() != null && datasetVersion.getLinks().getSelf() != null) {
            collectionDatasetVersion.setUri(datasetVersion.getLinks().getSelf().getHref());
        } else {
            logInfo("The dataset version URL has not been set on the dataset version response.")
                    .addParameter("collectionID", collectionID)
                    .addParameter("datasetID", datasetID)
                    .log();

            throw new InvalidObjectException("The dataset version URL has not been set on the dataset version response.");
        }

        collectionDatasetVersion.setTitle(dataset.getTitle());
        collection.getDescription().addDatasetVersion(collectionDatasetVersion);
        collection.save();

        return collectionDatasetVersion;
    }

    /**
     * Remove the instance for the given datasetID from the collection for the collectionID.
     */
    @Override
    public void removeDatasetFromCollection(String collectionID, String datasetID) throws ZebedeeException, IOException, DatasetAPIException {

        Collection collection = zebedeeCms.getCollection(collectionID);
        if (collection == null) {
            logInfo("Collection not found").collectionId(collectionID).logAndThrow(NotFoundException.class);
        }

        // if its not in the collection then just return.
        Optional<CollectionDataset> existingDataset = collection.getDescription().getDataset(datasetID);
        if (!existingDataset.isPresent()) {
            return;
        }

        Dataset datasetUpdate = new Dataset();
        datasetUpdate.setCollection_id("");
        datasetUpdate.setState(State.CREATED);
        datasetClient.updateDataset(datasetID, datasetUpdate);

        collection.getDescription().removeDataset(existingDataset.get());
        collection.save();
    }

    /**
     * Remove the instance for the given datasetID from the collection for the collectionID.
     */
    @Override
    public void removeDatasetVersionFromCollection(String collectionID, String datasetID, String edition, String version) throws ZebedeeException, IOException, DatasetAPIException {

        Collection collection = zebedeeCms.getCollection(collectionID);
        if (collection == null) {
            logInfo("Collection not found").collectionId(collectionID).logAndThrow(NotFoundException.class);
        }

        // if its not in the collection then return.
        Optional<CollectionDatasetVersion> existingDataset =
                collection.getDescription().getDatasetVersion(datasetID, edition, version);

        if (!existingDataset.isPresent()) {
            return;
        }

        // update the dataset version in the dataset API with a reverted state and blank collection ID.
        DatasetVersion versionUpdate = new DatasetVersion();
        versionUpdate.setCollection_id("");
        versionUpdate.setState(State.CREATED);
        datasetClient.updateDatasetVersion(datasetID, edition, version, versionUpdate);

        collection.getDescription().removeDatasetVersion(existingDataset.get());
        collection.save();
    }
}
