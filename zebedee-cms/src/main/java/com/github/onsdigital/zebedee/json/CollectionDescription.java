package com.github.onsdigital.zebedee.json;

import com.github.onsdigital.zebedee.json.publishing.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This cd ..
 *
 * @author david
 */
public class CollectionDescription extends CollectionBase {

    public List<String> inProgressUris;
    public List<String> completeUris;
    public List<String> reviewedUris;
    public ApprovalStatus approvalStatus = ApprovalStatus.NOT_STARTED;
    public boolean publishComplete;
    private Map<String, String> publishTransactionIds;
    public Date publishStartDate; // The date the publish process was actually started
    public Date publishEndDate; // The date the publish process ended.
    public boolean isEncrypted;
    private List<PendingDelete> pendingDeletes;
    private Set<CollectionDataset> datasets;
    private Set<CollectionDatasetVersion> datasetVersions;

    public List<String> timeseriesImportFiles = new ArrayList<>();

    /**
     * events related to this collection
     */
    public Events events;

    /**
     * A List of {@link Event} for each uri in the collection.
     */
    public Map<String, Events> eventsByUri;

    /**
     * A list of {@link com.github.onsdigital.zebedee.json.publishing.Result} for
     * each attempt at publishing this collection.
     */
    public List<Result> publishResults;

    /**
     * Default constuructor for serialisation.
     */
    public CollectionDescription() {
        // No action.
    }

    /**
     * Convenience constructor for instantiating with a name.
     *
     * @param name The value for the name.
     */
    public CollectionDescription(String name) {
        this.name = name;
    }


    /**
     * Convenience constructor for instantiating with a name
     * and publish date.
     *
     * @param name
     * @param publishDate
     */
    public CollectionDescription(String name, Date publishDate) {
        this.publishDate = publishDate;
        this.name = name;
    }

    /**
     * Add an event to this collection description.
     *
     * @param event
     */
    public void addEvent(Event event) {

        if (events == null)
            events = new Events();

        events.add(event);
    }

    /**
     * Add a {@link Result} to this
     * {@link CollectionDescription}.
     *
     * @param result
     */
    public void AddPublishResult(Result result) {
        if (publishResults == null) {
            publishResults = new ArrayList<>();
        }

        publishResults.add(result);
    }

    public List<PendingDelete> getPendingDeletes() {
        if (this.pendingDeletes == null) {
            this.pendingDeletes = new ArrayList<>();
        }
        return pendingDeletes;
    }

    public void cancelPendingDelete(String uri) {
        setPendingDeletes(getPendingDeletes()
                .stream()
                .filter(pd -> !pd.getRoot().contentPath.equals(uri))
                .collect(Collectors.toList())
        );
    }

    public void setPendingDeletes(List<PendingDelete> pendingDeletes) {
        this.pendingDeletes = pendingDeletes;
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    /**
     * Get an immutable set of the datasets in this collection.
     */
    public Set<CollectionDataset> getDatasets() {

        if (this.datasets == null) {
            this.datasets = new HashSet<>();
        }

        return Collections.unmodifiableSet(this.datasets);
    }

    /**
     * Get the dataset for the given ID.
     *
     * @param datasetID - the ID of the dataset to get.
     * @return optional containing the dataset if it exists in the set.
     */
    public Optional<CollectionDataset> getDataset(String datasetID) {

        if (this.datasets == null) {
            return Optional.empty();
        }

        return this.datasets.stream()
                .filter(i -> i.getId().equals(datasetID)).findFirst();
    }

    /**
     * Add a dataset to this collection
     *
     * @param dataset the dataset to add to this collection
     */
    public void addDataset(CollectionDataset dataset) {

        if (this.datasets == null) {
            this.datasets = new HashSet<>();
        }

        this.datasets.add(dataset);
    }

    /**
     * delete a dataset from this collection
     *
     * @param dataset
     */
    public void removeDataset(CollectionDataset dataset) {

        if (this.datasets == null) return;

        this.datasets.remove(dataset);
    }


    /**
     * Get an immutable set of the dataset versions in this collection.
     */
    public Set<CollectionDatasetVersion> getDatasetVersions() {

        if (this.datasetVersions == null) {
            this.datasetVersions = new HashSet<>();
        }

        return Collections.unmodifiableSet(this.datasetVersions);
    }

    /**
     * Get the dataset version for the given values
     *
     * @param datasetID - the dataset ID of the version
     * @param edition   - the dataset edition of the version
     * @param version   - the version
     * @return an optional containing the dataset version if it exists.
     */
    public Optional<CollectionDatasetVersion> getDatasetVersion(String datasetID, String edition, String version) {

        if (this.datasetVersions == null) {
            return Optional.empty();
        }

        return this.datasetVersions.stream()
                .filter(i -> i.getId().equals(datasetID)
                        && i.getEdition().equals(edition)
                        && i.getVersion().equals(version))
                .findFirst();
    }

    /**
     * Add a dataset version to this collection.
     *
     * @param version
     */
    public void addDatasetVersion(CollectionDatasetVersion version) {

        if (this.datasetVersions == null) {
            this.datasetVersions = new HashSet<>();
        }

        this.datasetVersions.add(version);
    }

    /**
     * delete a dataset version from this collection
     *
     * @param version
     */
    public void removeDatasetVersion(CollectionDatasetVersion version) {

        if (this.datasetVersions == null) return;

        this.datasetVersions.remove(version);
    }

    public Map<String, String> getPublishTransactionIds() {
        return this.publishTransactionIds;
    }

    public void setPublishTransactionIds(Map<String, String> publishTransactionIds) {
        this.publishTransactionIds = publishTransactionIds;
    }
}
