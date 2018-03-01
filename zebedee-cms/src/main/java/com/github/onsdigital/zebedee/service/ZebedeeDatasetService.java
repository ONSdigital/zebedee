package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.dataset.api.DatasetClient;
import com.github.onsdigital.zebedee.dataset.api.exception.DatasetAPIException;
import com.github.onsdigital.zebedee.dataset.api.exception.UnexpectedResponseException;
import com.github.onsdigital.zebedee.dataset.api.model.Dataset;
import com.github.onsdigital.zebedee.dataset.api.model.DatasetVersion;
import com.github.onsdigital.zebedee.dataset.api.model.State;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.ForbiddenException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDatasetVersion;
import com.github.onsdigital.zebedee.json.ContentStatus;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.Content;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;

import java.io.IOException;
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

    private ContentStatus updatedDatasetStateInCollection(ContentStatus currentState, ContentStatus newState, String lastEditedBy, String user) throws ForbiddenException {
        // The same user can't review edits they've submitted for review
        if (!currentState.equals(ContentStatus.Reviewed) && newState.equals(ContentStatus.Reviewed) && lastEditedBy.equalsIgnoreCase(user)) {
            throw new ForbiddenException("User " + user + "doesn't have permission to review a dataset they completed");
        }

        // Any further updates made by the user who submitted the dataset should keep the dataset in the awaiting review state
        if (currentState.equals(ContentStatus.Complete) && lastEditedBy.equalsIgnoreCase(user)) {
            return ContentStatus.Complete;
        }

        // Any updates to a dataset awaiting review by a different user means it moves back to an in progress state
        if (currentState.equals(ContentStatus.Complete) && !newState.equals(ContentStatus.Reviewed) && !lastEditedBy.equalsIgnoreCase(user)) {
            return ContentStatus.InProgress;
        }

        // Once reviewed any updates can be made to a dataset without the state changing
        if (currentState.equals(ContentStatus.Reviewed)) {
            return ContentStatus.Reviewed;
        }

        return newState;
    }

    /**
     * Add the dataset for the given datasetID to the collection for the collectionID.
     */
    @Override
    public CollectionDataset updateDatasetInCollection(String collectionID, String datasetID, CollectionDataset updatedDataset, String user) throws ZebedeeException, IOException, DatasetAPIException, ForbiddenException {

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
            collectionDataset.setState(updatedDatasetStateInCollection(collectionDataset.getState(), updatedDataset.getState(), collectionDataset.getLastEditedBy(), user));
        }

        collectionDataset.setLastEditedBy(user);

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

                throw new UnexpectedResponseException("The dataset URL has not been set on the dataset response.");
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
    public CollectionDatasetVersion updateDatasetVersionInCollection(String collectionID, String datasetID, String edition, String version, CollectionDatasetVersion updatedVersion, String user) throws ZebedeeException, IOException, DatasetAPIException {

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

            throw new UnexpectedResponseException("The dataset version URL has not been set on the dataset version response.");
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
