package com.github.onsdigital.zebedee.content.page.statistics.dataset;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;

import java.util.List;

/**
 * Created by bren on 19/10/15.
 */
public class Dataset extends Page {

    private List<DownloadSection> downloads;
    private List<DownloadSection> supplementaryFiles;
    private List<Version> versions;

    @Override
    public PageType getType() {
        return PageType.dataset;
    }

    public List<DownloadSection> getDownloads() {
        return downloads;
    }

    public void setDownloads(List<DownloadSection> downloads) {
        this.downloads = downloads;
    }

    public List<DownloadSection> getSupplementaryFiles() {
        return supplementaryFiles;
    }

    public void setSupplementaryFiles(List<DownloadSection> supplementaryFiles) {
        this.supplementaryFiles = supplementaryFiles;
    }

    public List<Version> getVersions() {
        return versions;
    }

    public void setVersions(List<Version> versions) {
        this.versions = versions;
    }
}
