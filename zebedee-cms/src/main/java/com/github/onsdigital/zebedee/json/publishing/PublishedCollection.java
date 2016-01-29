package com.github.onsdigital.zebedee.json.publishing;

import com.github.onsdigital.zebedee.json.CollectionBase;

import java.util.Date;
import java.util.List;

public class PublishedCollection extends CollectionBase {

    public int verifiedCount;
    public int verifyFailedCount;
    public int verifyInprogressCount;

    public Date publishStartDate; // The date the publish process was actually started
    public Date publishEndDate; // The date the publish process ended.

    /**
     * A list of {@link com.github.onsdigital.zebedee.json.publishing.Result} for
     * each attempt at publishing this collection.
     */
    public List<Result> publishResults;


    public void incrementVerified() {
        synchronized (this) {
            verifiedCount++;
        }
    }

    public void incrementVerifyFailed() {
        synchronized (this) {
            verifyFailedCount++;
        }
    }

    public void incrementVerifyInProgressCount() {
        synchronized (this) {
            verifyInprogressCount++;
        }
    }

    public void decrementVerifyInProgressCount() {
        synchronized (this) {
            verifyInprogressCount--;
        }
    }
}
