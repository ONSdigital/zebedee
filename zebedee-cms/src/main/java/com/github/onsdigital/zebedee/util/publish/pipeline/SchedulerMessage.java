package com.github.onsdigital.zebedee.util.publish.pipeline;


import com.google.gson.Gson;

public class SchedulerMessage {

    private String collectionId;

    private String scheduleTime;

    private String encryptionKey;


    public static String createSchedulerMessage(String collectionId, String publishTime, String encryptionKey) {
        SchedulerMessage message = new SchedulerMessage();
        message.setCollectionId(collectionId);
        message.setScheduleTime(publishTime);
        message.setEncryptionKey(encryptionKey);
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

    public void setScheduleTime(String publishTime) {
        this.scheduleTime = publishTime;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
}
