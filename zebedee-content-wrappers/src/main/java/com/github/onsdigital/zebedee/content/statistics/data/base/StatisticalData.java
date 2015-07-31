package com.github.onsdigital.zebedee.content.statistics.data.base;

import com.github.onsdigital.zebedee.content.link.ContentReference;
import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;
import com.github.onsdigital.zebedee.content.statistics.base.Statistics;

import java.util.List;

/**
 * Created by bren on 05/06/15.
 */
public abstract class StatisticalData extends Statistics {

    /*Body*/
    private List<ContentReference> relatedDatasets;
    private MarkdownSection section; //Explanatory section
    private List<String> notes;//Markdown
    private List<ContentReference> relatedDocuments;
    private List<ContentReference> relatedMethodology;
    private List<ContentReference> relatedData;

    public List<ContentReference> getRelatedDocuments() {
        return relatedDocuments;
    }

    public void setRelatedDocuments(List<ContentReference> relatedDocuments) {
        this.relatedDocuments = relatedDocuments;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
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

    public void setRelatedMethodology(List<ContentReference> methodology) {
        this.relatedMethodology = methodology;
    }

    public List<ContentReference> getRelatedData() {
        return relatedData;
    }

    public void setRelatedData(List<ContentReference> relatedData) {
        this.relatedData = relatedData;
    }

    public List<ContentReference> getRelatedDatasets() {
        return relatedDatasets;
    }

    public void setRelatedDatasets(List<ContentReference> relatedDatasets) {
        this.relatedDatasets = relatedDatasets;
    }
}
