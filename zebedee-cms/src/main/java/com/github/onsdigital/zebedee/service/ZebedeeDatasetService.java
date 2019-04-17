package com.github.onsdigital.zebedee.service;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.ForbiddenException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.CollectionDataset;
import com.github.onsdigital.zebedee.json.CollectionDatasetVersion;
import com.github.onsdigital.zebedee.json.ContentStatus;
import com.github.onsdigital.zebedee.model.Collection;
import dp.api.dataset.DatasetClient;
import dp.api.dataset.exception.DatasetAPIException;
import dp.api.dataset.model.Dataset;
import dp.api.dataset.model.DatasetVersion;
import dp.api.dataset.model.State;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.Objects;
import java.util.Optional;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

/**
 * Dataset related services
 */
public class ZebedeeDatasetService implements DatasetService {

    private DatasetClient datasetClient;

    public ZebedeeDatasetService(DatasetClient datasetClient) {
        this.datasetClient = datasetClient;
    }

    private ContentStatus updatedStateInCollection(ContentStatus currentState, ContentStatus newState, String lastEditedBy, String user) throws ForbiddenException, BadRequestException {

        Objects.requireNonNull(newState);

        if (currentState == null && newState.equals(ContentStatus.Reviewed)) {
            info().data("last edited by", lastEditedBy).data("user", user)
                    .log("Attempt to review a dataset that hasn't been submitted for review");

            throw new BadRequestException("Cannot be reviewed without being submitted for review first");
        }

        // Updating from scratch to 'in progress' or 'complete' state so don't need to perform following checks
        if (currentState == null && (newState.equals(ContentStatus.InProgress) || newState.equals(ContentStatus.Complete))) {
            info().data("user", user)
                    .data("last edited by", lastEditedBy)
                    .data("current state", currentState)
                    .data("new state", newState)
                    .log("Updating dataset state for first time");

            return newState;
        }

        // The same user can't review edits they've submitted for review
        if (!currentState.equals(ContentStatus.Reviewed) && newState.equals(ContentStatus.Reviewed) && lastEditedBy.equalsIgnoreCase(user)) {
            info().data("user", user)
                    .data("last edited by", lastEditedBy)
                    .data("current state", currentState)
                    .data("new state", newState)
                    .log("User attempting to review their own dataset");

            throw new ForbiddenException("User " + user + "doesn't have permission to review a dataset they completed");
        }

        // Any further updates made by the user who submitted the dataset should keep the dataset in the awaiting review state
        if (currentState.equals(ContentStatus.Complete) && lastEditedBy.equalsIgnoreCase(user)) {

            info().data("user", user)
                    .data("last edited by", lastEditedBy)
                    .data("current state", currentState)
                    .data("new state", newState)
                    .log("User making more updates to a dataset whilst it is awaiting review");
            return ContentStatus.Complete;
        }

        // Any updates to a dataset awaiting review by a different user means it moves back to an in progress state
        if (currentState.equals(ContentStatus.Complete) && !newState.equals(ContentStatus.Reviewed) && !lastEditedBy.equalsIgnoreCase(user)) {

            info().data("user", user)
                    .data("last edited by", lastEditedBy)
                    .data("current state", currentState)
                    .data("new state", newState)
                    .log("A different user making updates to a dataset whilst it is awaiting review");
            return ContentStatus.InProgress;
        }

        // Once reviewed any updates can be made to a dataset without the state changing
        if (currentState.equals(ContentStatus.Reviewed)) {

            info().data("user", user)
                    .data("last edited by", lastEditedBy)
                    .data("current state", currentState)
                    .data("new state", newState)
                    .log("Making updates to a review dataset");
            return ContentStatus.Reviewed;
        }

        info().data("user", user)
                .data("last edited by", lastEditedBy)
                .data("current state", currentState)
                .data("new state", newState)
                .log("Updating dataset state");
        return newState;
    }

    /**
     * Publish the datasets / versions contained in the given collection.
     */
    @Override
    public void publishDatasetsInCollection(Collection collection) throws IOException, DatasetAPIException {

        for (CollectionDatasetVersion datasetVersion : collection.getDescription().getDatasetVersions()) {

            info().data("collectionId", collection.getDescription().getId())
                    .data("datasetId", datasetVersion.getId())
                    .data("edition", datasetVersion.getEdition())
                    .data("version", datasetVersion.getVersion())
                    .log("setting dataset api version state to published");

            DatasetVersion versionUpdate = new DatasetVersion();
            versionUpdate.setState(State.PUBLISHED);

            datasetClient.updateDatasetVersion(
                    datasetVersion.getId(),
                    datasetVersion.getEdition(),
                    datasetVersion.getVersion(),
                    versionUpdate);
        }

        for (CollectionDataset dataset : collection.getDescription().getDatasets()) {

            info().data("collectionId", collection.getDescription().getId())
                    .data("datasetId", dataset.getId())
                    .log("setting api dataset state to published");

            Dataset datasetUpdate = new Dataset();
            datasetUpdate.setState(State.PUBLISHED);

            datasetClient.updateDataset(dataset.getId(), datasetUpdate);
        }
    }

    /**
     * Add the dataset for the given datasetID to the collection for the collectionID.
     */
    @Override
    public CollectionDataset updateDatasetInCollection(Collection collection, String datasetID, CollectionDataset updatedDataset, String user) throws ZebedeeException, IOException, DatasetAPIException {

        CollectionDataset collectionDataset;

        Optional<CollectionDataset> existingDataset = collection.getDescription().getDataset(datasetID);
        if (existingDataset.isPresent()) {
            collectionDataset = existingDataset.get();
        } else {
            collectionDataset = new CollectionDataset();
            collectionDataset.setId(datasetID);
        }

        if (updatedDataset != null && updatedDataset.getState() != null) {
            collectionDataset.setState(updatedStateInCollection(collectionDataset.getState(), updatedDataset.getState(), collectionDataset.getLastEditedBy(), user));
        } else {
            collectionDataset.setState(ContentStatus.InProgress);
        }

        collectionDataset.setLastEditedBy(user);

        Dataset dataset = datasetClient.getDataset(datasetID);

        if (dataset != null) {

            collectionDataset.setTitle(dataset.getTitle());

            if (dataset.getLinks() != null && dataset.getLinks().getSelf() != null) {
                collectionDataset.setUri(dataset.getLinks().getSelf().getHref());
            } else {
                info().data("collectionId", collection.getDescription().getId())
                        .data("datasetId", datasetID)
                        .log("The dataset URL has not been set on the dataset response.");

                throw new InvalidObjectException("The dataset URL has not been set on the dataset response.");
            }

            if (State.CREATED.equals(dataset.getState())) {
                // the dataset has just been added to the collection, so update collection ID on the dataset
                Dataset datasetUpdate = new Dataset();
                datasetUpdate.setCollection_id(collection.getId());
                datasetUpdate.setState(State.ASSOCIATED);
                datasetClient.updateDataset(datasetID, datasetUpdate);
            }

            if (dataset.getState().equals(State.ASSOCIATED)
                    && !dataset.getCollection_id().equals(collection.getId())) {
                throw new ConflictException("cannot add dataset " + datasetID
                        + " to collection " + collection.getId()
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
    public CollectionDatasetVersion updateDatasetVersionInCollection(Collection collection, String datasetID, String edition, String version, CollectionDatasetVersion updatedVersion, String user) throws ZebedeeException, IOException, DatasetAPIException {

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
            collectionDatasetVersion.setState(updatedStateInCollection(collectionDatasetVersion.getState(), updatedVersion.getState(), collectionDatasetVersion.getLastEditedBy(), user));
        } else {
            collectionDatasetVersion.setState(ContentStatus.InProgress);
        }

        collectionDatasetVersion.setLastEditedBy(user);

        Dataset dataset = datasetClient.getDataset(datasetID);
        DatasetVersion datasetVersion = datasetClient.getDatasetVersion(datasetID, edition, version);

        if (State.EDITION_CONFIRMED.equals(datasetVersion.getState())) {
            // the dataset version has just been added to the collection, so update collection ID and set state
            DatasetVersion versionUpdate = new DatasetVersion();
            versionUpdate.setCollection_id(collection.getId());
            versionUpdate.setState(State.ASSOCIATED);
            datasetClient.updateDatasetVersion(datasetID, edition, version, versionUpdate);
        }

        if (State.ASSOCIATED.equals(datasetVersion.getState())
                && !datasetVersion.getCollection_id().equals(collection.getId())) {
            throw new ConflictException("cannot add dataset " + datasetID
                    + " to collection " + collection.getId()
                    + " it is already in collection " + dataset.getCollection_id());
        }

        if (datasetVersion.getLinks() != null && datasetVersion.getLinks().getSelf() != null) {
            collectionDatasetVersion.setUri(datasetVersion.getLinks().getSelf().getHref());
        } else {
            info().data("collectionId", collection.getId())
                    .data("datasetId", datasetID)
                    .log("The dataset version URL has not been set on the dataset version response.");

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
    public void removeDatasetFromCollection(Collection collection, String datasetID) throws IOException, DatasetAPIException {

        // if its not in the collection then just return.
        Optional<CollectionDataset> existingDataset = collection.getDescription().getDataset(datasetID);
        if (!existingDataset.isPresent()) {
            return;
        }

        try {
            datasetClient.deleteDataset(datasetID);
        } catch (dp.api.dataset.exception.ForbiddenException e) {
            // fall through - ForbiddenException is expected where the dataset has previous published versions.
        } catch (Exception e) {
            throw e;
        }

        collection.getDescription().removeDataset(existingDataset.get());
        collection.save();
    }

    /**
     * Remove the version for the given edition and datasetID from the collection for the collectionID.
     */
    @Override
    public void removeDatasetVersionFromCollection(Collection collection, String datasetID, String edition, String version) throws IOException, DatasetAPIException {

        // if its not in the collection then return.
        Optional<CollectionDatasetVersion> existingDataset =
                collection.getDescription().getDatasetVersion(datasetID, edition, version);

        if (!existingDataset.isPresent()) {
            return;
        }

        datasetClient.detachVersion(datasetID, edition, version);
        collection.getDescription().removeDatasetVersion(existingDataset.get());
        collection.save();
    }
}
