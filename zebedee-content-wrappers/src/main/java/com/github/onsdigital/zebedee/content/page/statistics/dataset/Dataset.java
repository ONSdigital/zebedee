package com.github.onsdigital.zebedee.content.page.statistics.dataset;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.PageReference;
import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;
import com.github.onsdigital.zebedee.content.page.statistics.base.Statistics;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class Dataset extends Statistics {

    /*Body*/
    private List<DownloadSection> downloads = new ArrayList<DownloadSection>();
    private List<PageReference> relatedMethodology;
    private MarkdownSection section;
    private List<MarkdownSection> notes;
    private List<PageReference> relatedDatasets;
    private List<PageReference> relatedDocuments;

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

    public List<MarkdownSection> getNotes() {
        return notes;
    }

    public void setNotes(List<MarkdownSection> notes) {
        this.notes = notes;
    }

    public MarkdownSection getSection() {
        return section;
    }

    public void setSection(MarkdownSection section) {
        this.section = section;
    }

    public List<PageReference> getRelatedMethodology() {
        return relatedMethodology;
    }

    public void setRelatedMethodology(List<PageReference> relatedMethodology) {
        this.relatedMethodology = relatedMethodology;
    }

    public List<PageReference> getRelatedDatasets() {
        return relatedDatasets;
    }

    public void setRelatedDatasets(List<PageReference> relatedDatasets) {
        this.relatedDatasets = relatedDatasets;
    }

    public List<PageReference> getRelatedDocuments() {
        return relatedDocuments;
    }

    public void setRelatedDocuments(List<PageReference> relatedDocuments) {
        this.relatedDocuments = relatedDocuments;
    }
}
