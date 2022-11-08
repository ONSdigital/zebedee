package com.github.onsdigital.zebedee.model.approval;

public enum ApprovalEventType {

    APPROVAL_STARTED("approvalStarted"),

    RESOLVED_DETAILS("resolvedDetails"),

    ADD_INTERACTIVE_DETAILS("addInteractiveDetails"),

    ADD_DATASET_VERSION_DETAILS("addDatasetDetails"),

    POPULATED_RELEASE_PAGE("populatedReleasePage"),

    GENERATED_TIME_SERIES("generatedTimeSeries"),

    GENERATED_PDFS("generatedPDFs"),

    CREATED_PUBLISH_NOTIFICATION("createdPublishNotification"),

    COMPRESSED_ZIP_FILES("compressedZipFiles"),

    APPROVAL_STATE_SET("approvalStateSet"),

    SENT_PUBLISH_NOTIFICATION("sentPublishNotification"),

    APPROVAL_COMPLETED("approvalCompleted");

    private final String description;

    private ApprovalEventType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }


    @Override
    public String toString() {
        return description;
    }
}
