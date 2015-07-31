package com.github.onsdigital.zebedee.content.statistics.dataset;

import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.link.ContentReference;
import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;
import com.github.onsdigital.zebedee.content.statistics.base.Statistics;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class Dataset extends Statistics {

    /*Body*/
    private List<DownloadSection> downloads = new ArrayList<DownloadSection>();
    private List<ContentReference> relatedMethodology;
    private MarkdownSection section;
    private List<MarkdownSection> notes;
    private List<ContentReference> relatedDatasets;
    private List<ContentReference> relatedDocuments;

    @Override
    public ContentType getType() {
        return ContentType.dataset;
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

    public List<ContentReference> getRelatedMethodology() {
        return relatedMethodology;
    }

    public void setRelatedMethodology(List<ContentReference> relatedMethodology) {
        this.relatedMethodology = relatedMethodology;
    }

    public List<ContentReference> getRelatedDatasets() {
        return relatedDatasets;
    }

    public void setRelatedDatasets(List<ContentReference> relatedDatasets) {
        this.relatedDatasets = relatedDatasets;
    }

    public List<ContentReference> getRelatedDocuments() {
        return relatedDocuments;
    }

    public void setRelatedDocuments(List<ContentReference> relatedDocuments) {
        this.relatedDocuments = relatedDocuments;
    }
}
