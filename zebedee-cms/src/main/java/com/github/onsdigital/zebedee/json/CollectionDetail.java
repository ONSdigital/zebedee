package com.github.onsdigital.zebedee.json;

import com.github.onsdigital.zebedee.model.CollectionOwner;

import java.util.List;
import java.util.Set;

public class CollectionDetail extends CollectionBase {
    public List<ContentDetail> inProgress;
    public List<ContentDetail> complete;
    public List<ContentDetail> reviewed;
    public List<String> timeseriesImportFiles;
    public ApprovalStatus approvalStatus;
    public List<PendingDelete> pendingDeletes;
    public Events events;
    public CollectionOwner collectionOwner; // What team created the collection (eg PST or Data vis)
    public Set<CollectionInstance> instances;
}
