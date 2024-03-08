package com.github.onsdigital.zebedee.model.publishing.legacycacheapi;

import com.github.onsdigital.zebedee.model.publishing.PublishNotification;
import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class LegacyCacheApiPayload {
    @SerializedName("collection_id")
    public String collectionId;
    @SerializedName("release_time")
    public String publishDate;
    @SerializedName("path")
    public String uriToUpdate;

    public LegacyCacheApiPayload(String collectionId, String uriToUpdate, Date publishDate) {
        this.collectionId = collectionId;
        this.publishDate = PublishNotification.format(publishDate);
        this.uriToUpdate = uriToUpdate;
    }

    public LegacyCacheApiPayload(String collectionId, String uriToUpdate) {
        this.collectionId = collectionId;
        this.uriToUpdate = uriToUpdate;
    }
}
