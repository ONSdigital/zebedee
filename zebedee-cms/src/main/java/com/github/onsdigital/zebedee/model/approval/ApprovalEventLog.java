package com.github.onsdigital.zebedee.model.approval;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.zebedee.model.approval.ApprovalEventType.ADD_DATASET_DETAILS;
import static com.github.onsdigital.zebedee.model.approval.ApprovalEventType.ADD_DATASET_VERSION_DETAILS;
import static com.github.onsdigital.zebedee.model.approval.ApprovalEventType.APPROVAL_COMPLETED;
import static com.github.onsdigital.zebedee.model.approval.ApprovalEventType.APPROVAL_STARTED;
import static com.github.onsdigital.zebedee.model.approval.ApprovalEventType.APPROVAL_STATE_SET;
import static com.github.onsdigital.zebedee.model.approval.ApprovalEventType.COMPRESSED_ZIP_FILES;
import static com.github.onsdigital.zebedee.model.approval.ApprovalEventType.CREATED_PUBLISH_NOTIFICATION;
import static com.github.onsdigital.zebedee.model.approval.ApprovalEventType.GENERATED_PDFS;
import static com.github.onsdigital.zebedee.model.approval.ApprovalEventType.GENERATED_TIME_SERIES;
import static com.github.onsdigital.zebedee.model.approval.ApprovalEventType.POPULATED_RELEASE_PAGE;
import static com.github.onsdigital.zebedee.model.approval.ApprovalEventType.RESOLVED_DETAILS;
import static com.github.onsdigital.zebedee.model.approval.ApprovalEventType.SENT_PUBLISH_NOTIFICATION;

public class ApprovalEventLog {

    private String collectionID;
    private String approverEmail;

    private List<ApprovalEvent> eventLog;

    public ApprovalEventLog(String collectionID, String approverEmail) {
        this.collectionID = collectionID;
        this.approverEmail = approverEmail;
        this.eventLog = new ArrayList<>();
        this.eventLog.add(new ApprovalEvent(APPROVAL_STARTED, new Date()));
    }

    public void addEvent(ApprovalEventType event) {
        this.eventLog.add(new ApprovalEvent(event, new Date()));

        info().data("step", event.name()).data("collectionId", collectionID)
                .data("approver", approverEmail)
                .log("collection approval step addEvent");
    }

    public List<ApprovalEvent> logDetails() {
        return eventLog;
    }

    public void approvalStarted() {
        addEvent(APPROVAL_STARTED);
    }

    public void resolvedDetails() {
        addEvent(RESOLVED_DETAILS);
    }

    public void addDatasetDetails() {
        addEvent(ADD_DATASET_DETAILS);
    }

    public void addDatasetVersionDetails() {
        addEvent(ADD_DATASET_VERSION_DETAILS);
    }

    public void populatedResleasePage() {
        addEvent(POPULATED_RELEASE_PAGE);
    }

    public void generatedTimeSeries() {
        addEvent(GENERATED_TIME_SERIES);
    }

    public void generatedPDFs() {
        addEvent(GENERATED_PDFS);
    }

    public void createdPublishNotificaion() {
        addEvent(CREATED_PUBLISH_NOTIFICATION);
    }

    public void compressedZipFiles() {
        addEvent(COMPRESSED_ZIP_FILES);
    }

    public void approvalStateSet() {
        addEvent(APPROVAL_STATE_SET);
    }

    public void sentPublishNotification() {
        addEvent(SENT_PUBLISH_NOTIFICATION);
    }

    public void approvalCompleted() {
        addEvent(APPROVAL_COMPLETED);
    }
}
