package com.github.onsdigital.zebedee.json;

import com.github.onsdigital.zebedee.json.publishing.Result;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    public Map<String, String> publishTransactionIds;
    public Date publishStartDate; // The date the publish process was actually started
    public Date publishEndDate; // The date the publish process ended.
    public boolean isEncrypted;
    private List<PendingDelete> pendingDeletes;

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



}
