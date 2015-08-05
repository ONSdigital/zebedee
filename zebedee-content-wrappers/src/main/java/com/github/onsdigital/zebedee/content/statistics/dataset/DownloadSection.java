package com.github.onsdigital.zebedee.content.statistics.dataset;

import java.util.List;

/**
 * Represents a section in a dataset for downloading
 */
public class DownloadSection  {
    private String title;
    private List<String> cdids;
    private String file;
    private String fileDescription; //Markdown

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getCdids() {
        return cdids;
    }

    public void setCdids(List<String> cdids) {
        this.cdids = cdids;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public String getFileDescription() {
        return fileDescription;
    }

    public void setFileDescription(String fileDescription) {
        this.fileDescription = fileDescription;
    }
}
