package com.github.onsdigital.zebedee.model.approval;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

public class ApprovalManifest {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    public enum Step {

        APPOVAL_TASK_STARTED,

        RESOLVE_DETAILS,

        POPULATE_RELEASE_PAGE,

        GENERATE_TIME_SERIES,

        GENERATE_PDFS,

        CREATE_PUBLISH_NOTIFICATION,

        COMPRESS_ZIP_FILES,

        SET_APPROVAL_STATE,

        SEND_PUBLISH_NOTIFICATION,

        COMPLETED;
    }

    private String collectionID;
    private String approverEmail;

    private List<ApprovalEvent> eventLog;

    public ApprovalManifest(String collectionID, String approverEmail) {
        this.collectionID = collectionID;
        this.approverEmail = approverEmail;
        this.eventLog = new ArrayList<>();
        this.eventLog.add(new ApprovalEvent(Step.APPOVAL_TASK_STARTED));
    }

    public void log(Step step) {
        this.eventLog.add(new ApprovalEvent(step));

        logInfo("collection approval step addEvent")
                .addParameter("step", step.name())
                .collectionId(collectionID)
                .addParameter("approver", approverEmail)
                .log();
    }

    public List<ApprovalEvent> logDetails() {
        return eventLog;
    }

    public class ApprovalEvent {
        private String completedAt;
        private Step step;

        public ApprovalEvent(Step step) {
            this.step = step;
            this.completedAt = DATE_FORMAT.format(new Date());
        }

        public String getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(String completedAt) {
            this.completedAt = completedAt;
        }

        public Step getStep() {
            return step;
        }

        public void setStep(Step step) {
            this.step = step;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("completedAt", completedAt)
                    .append("step", step)
                    .toString();
        }
    }
}
