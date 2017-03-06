package com.github.onsdigital.zebedee.util.publish.pipeline;

public class CompleteMessage {

    private String CollectionId;

    private String ScheduleID;

    public String getCollectionId() {
        return CollectionId;
    }

    public void setCollectionId(String collectionId) {
        CollectionId = collectionId;
    }

    public String getScheduleID() {
        return ScheduleID;
    }

    public void setScheduleID(String scheduleID) {
        ScheduleID = scheduleID;
    }
}
