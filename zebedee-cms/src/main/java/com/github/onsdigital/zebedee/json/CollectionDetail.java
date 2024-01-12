package com.github.onsdigital.zebedee.json;

import com.github.onsdigital.zebedee.teams.model.Team;

import java.util.List;
import java.util.Set;

public class CollectionDetail extends CollectionBase {
    public Set<ContentDetail> inProgress;
    public Set<ContentDetail> complete;
    public Set<ContentDetail> reviewed;
    public List<String> timeseriesImportFiles;
    public ApprovalStatus approvalStatus;
    public List<PendingDelete> pendingDeletes;
    public Events events;
    public Set<CollectionDataset> datasets;
    public Set<CollectionDatasetVersion> datasetVersions;

    /**
     * The following is deprecated and must be removed after the migration to JWT sessions is complete.
     */
    @Deprecated
    public List<Team> teamsDetails;

}
