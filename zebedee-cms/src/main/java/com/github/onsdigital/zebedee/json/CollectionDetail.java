package com.github.onsdigital.zebedee.json;

import java.util.List;

public class CollectionDetail extends CollectionBase {
    public List<ContentDetail> inProgress;
    public List<ContentDetail> complete;
    public List<ContentDetail> reviewed;
    public List<String> timeseriesImportFiles;
    public boolean approvedStatus;

    public Events events;
}
