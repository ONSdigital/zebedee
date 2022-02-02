package com.github.onsdigital.zebedee.json;

import com.github.onsdigital.zebedee.json.publishing.Result;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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

    private List<String> inProgressUris;
    private List<String> completeUris;
    private List<String> reviewedUris;
    private List<PendingDelete> pendingDeletes;
    private List<Result> publishResults;
    private List<String> timeseriesImportFiles;

    private Map<String, String> publishTransactionIds;
    private Map<String, Events> eventsByUri;
    private Set<CollectionDataset> datasets;
    private Set<CollectionDatasetVersion> datasetVersions;

    private ApprovalStatus approvalStatus = ApprovalStatus.NOT_STARTED;
    private boolean publishComplete;
    private Date publishStartDate;
    private Date publishEndDate;
    private boolean isEncrypted;
    private Events events;

    /**
     * Default constuructor for serialisation.
     */
    public CollectionDescription() {
        this.inProgressUris = new ArrayList<>();
        this.completeUris = new ArrayList<>();
        this.reviewedUris = new ArrayList<>();
        this.pendingDeletes = new ArrayList<>();
        this.publishResults = new ArrayList<>();
        this.timeseriesImportFiles = new ArrayList<>();
        this.publishTransactionIds = new HashMap<>();
        this.eventsByUri = new HashMap<>();
        this.datasets = new HashSet<>();
        this.datasetVersions = new HashSet<>();
    }

    /**
     * Convenience constructor for instantiating with a name.
     *
     * @param name The value for the name.
     */
    public CollectionDescription(String name) {
        this.name = name;
        this.inProgressUris = new ArrayList<>();
        this.completeUris = new ArrayList<>();
        this.reviewedUris = new ArrayList<>();
        this.pendingDeletes = new ArrayList<>();
        this.publishResults = new ArrayList<>();
        this.timeseriesImportFiles = new ArrayList<>();
        this.publishTransactionIds = new HashMap<>();
        this.eventsByUri = new HashMap<>();
        this.datasets = new HashSet<>();
        this.datasetVersions = new HashSet<>();
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

    public void addEvent(Date date, EventType eventType, Session session) {
        this.addEvent(date, eventType, session, null);
    }

    /**
     * Add an event to the collection history.
     *
     * @param date      the date of the event.
     * @param eventType the type of event to record.
     * @param session   the session of the user who triggered the event.
     */
    public void addEvent(Date date, EventType eventType, Session session, String note) {
        String email = "";
        if (session != null || StringUtils.isNotEmpty(session.getEmail())) {
            email = session.getEmail();
        }

        if (StringUtils.isEmpty(note)) {
            this.addEvent(new Event(date, eventType, email));
        } else {
            this.addEvent(new Event(date, eventType, email, note));
        }
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

    public List<String> getInProgressUris() {
        return this.inProgressUris;
    }

    public void setInProgressUris(final List<String> inProgressUris) {
        this.inProgressUris = inProgressUris;
    }

    public List<String> getCompleteUris() {
        return this.completeUris;
    }

    public void setCompleteUris(final List<String> completeUris) {
        this.completeUris = completeUris;
    }

    public List<String> getReviewedUris() {
        return this.reviewedUris;
    }

    public void setReviewedUris(final List<String> reviewedUris) {
        this.reviewedUris = reviewedUris;
    }

    public boolean isPublishComplete() {
        return this.publishComplete;
    }

    public void setPublishComplete(final boolean publishComplete) {
        this.publishComplete = publishComplete;
    }

    public Date getPublishStartDate() {
        return this.publishStartDate;
    }

    public void setPublishStartDate(final Date publishStartDate) {
        this.publishStartDate = publishStartDate;
    }

    public Date getPublishEndDate() {
        return this.publishEndDate;
    }

    public void setPublishEndDate(final Date publishEndDate) {
        this.publishEndDate = publishEndDate;
    }

    public boolean isEncrypted() {
        return this.isEncrypted;
    }

    public void setEncrypted(final boolean encrypted) {
        this.isEncrypted = encrypted;
    }

    public void setDatasets(final Set<CollectionDataset> datasets) {
        this.datasets = datasets;
    }

    public void setDatasetVersions(final Set<CollectionDatasetVersion> datasetVersions) {
        this.datasetVersions = datasetVersions;
    }

    public List<String> getTimeseriesImportFiles() {
        return this.timeseriesImportFiles;
    }

    public void setTimeseriesImportFiles(final List<String> timeseriesImportFiles) {
        this.timeseriesImportFiles = timeseriesImportFiles;
    }

    public Events getEvents() {
        return this.events;
    }

    public void setEvents(final Events events) {
        this.events = events;
    }

    public Map<String, Events> getEventsByUri() {
        return this.eventsByUri;
    }

    public void setEventsByUri(final Map<String, Events> eventsByUri) {
        this.eventsByUri = eventsByUri;
    }

    public List<Result> getPublishResults() {
        return this.publishResults;
    }

    public void setPublishResults(final List<Result> publishResults) {
        this.publishResults = publishResults;
    }
}
