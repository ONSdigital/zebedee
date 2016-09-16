package com.github.onsdigital.zebedee.json;

import com.github.onsdigital.zebedee.model.CollectionOwner;

import java.util.List;

public class CollectionDetail extends CollectionBase {
    public List<ContentDetail> inProgress;
    public List<ContentDetail> complete;
    public List<ContentDetail> reviewed;
    public List<String> timeseriesImportFiles;
    public boolean approvedStatus;
    public List<PendingDelete> pendingDeletes;

    public Events events;

    public CollectionOwner collectionOwner; // What team created the collection (eg PST or Data vis)
}
