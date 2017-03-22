package com.github.onsdigital.zebedee.util.publish.pipeline;


import com.google.gson.Gson;

import java.util.HashSet;
import java.util.Set;

public class SchedulerMessage {

    private String collectionId;

    private String collectionPath;

    private String scheduleTime;

    private String encryptionKey;

    private Set<String> urisToDelete;

    private Set<PublishedFile> files;

    static String createSchedulerMessage(String collectionId, String collectionPath, String publishTime,
                                         String encryptionKey, Set<String> urisToDelete, Set<PublishedFile> files) {
        SchedulerMessage message = new SchedulerMessage();
        message.setCollectionId(collectionId);
        message.setCollectionPath(collectionPath);
        message.setScheduleTime(publishTime);
        message.setEncryptionKey(encryptionKey);
        message.setUrisToDelete(urisToDelete);
        message.setFiles(files);
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

    public String getEncryptionKey() {
        return encryptionKey;
    }

    void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
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

}
