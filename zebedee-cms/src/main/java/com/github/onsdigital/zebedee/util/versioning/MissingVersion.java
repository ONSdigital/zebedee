package com.github.onsdigital.zebedee.util.versioning;

import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Version;

public class MissingVersion {

    private String datasetTitle;
    private String currentURI;
    private String versionURI;

    public MissingVersion(Dataset dataset, Version version) {
        this.datasetTitle = dataset.getDescription().getTitle();
        this.currentURI = dataset.getUri().toString();
        this.versionURI = version.getUri().toString();
    }

    public String getDatasetTitle() {
        return this.datasetTitle;
    }

    public String getVersionURI() {
        return this.versionURI;
    }

    public String getCurrentURI() {
        return this.currentURI;
    }

    @Override
    public String toString() {
        return versionURI;
    }
}
