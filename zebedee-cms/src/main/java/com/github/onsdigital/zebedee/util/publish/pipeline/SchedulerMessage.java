package com.github.onsdigital.zebedee.util.publish.pipeline;


import com.google.gson.Gson;

import java.util.Set;

public class SchedulerMessage {

    public static final String ACTION_SCHEDULE = "schedule";

    public static final String ACTION_CANCEL = "cancel";

    private String collectionId;

    private String collectionPath;

    private String scheduleTime;

    private Set<String> urisToDelete;

    private Set<PublishedFile> files;

    private String action;

    static String createSchedulerMessage(String collectionId, String collectionPath, String publishTime,
                                         Set<String> urisToDelete, Set<PublishedFile> files,
                                         String action) {
        SchedulerMessage message = new SchedulerMessage();
        message.setCollectionId(collectionId);
        message.setCollectionPath(collectionPath);
        message.setScheduleTime(publishTime);
        message.setUrisToDelete(urisToDelete);
        message.setFiles(files);
        message.setAction(action);
        final Gson gson = new Gson();
        return gson.toJson(message);
    }

    static String createCancelSchedulerMessage(String collectionId, String epochTime,
                                         String action) {
        SchedulerMessage message = new SchedulerMessage();
        message.setCollectionId(collectionId);
        message.setScheduleTime(epochTime);
        message.setAction(action);
        final Gson gson = new Gson();
        return gson.toJson(message);
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    public String getScheduleTime() {
        return scheduleTime;
    }

    void setScheduleTime(String publishTime) {
        this.scheduleTime = publishTime;
    }

    public String getCollectionPath() {
        return collectionPath;
    }

    void setCollectionPath(String collectionPath) {
        this.collectionPath = collectionPath;
    }

    public Set<String> getUrisToDelete() {
        return urisToDelete;
    }

    public void setUrisToDelete(Set<String> urisToDelete) {
        this.urisToDelete = urisToDelete;
    }

    public Set<PublishedFile> getFiles() {
        return files;
    }

    public void setFiles(Set<PublishedFile> files) {
        this.files = files;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String acction) {
        this.action = acction;
    }
}
