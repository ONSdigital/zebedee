package com.github.onsdigital.zebedee.json;

import com.github.onsdigital.zebedee.teams.model.Team;

import java.util.List;

public class CollectionDetail extends CollectionBase {
    public List<ContentDetail> inProgress;
    public List<ContentDetail> complete;
    public List<ContentDetail> reviewed;
    public List<String> timeseriesImportFiles;
    public ApprovalStatus approvalStatus;
    public List<PendingDelete> pendingDeletes;
    public List<Team> teamsDetails;

    public Events events;
}
