package com.github.onsdigital.zebedee.content.page.statistics.data.base;

import com.github.onsdigital.zebedee.content.page.statistics.base.Statistics;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;

import java.util.List;

/**
 * Created by bren on 05/06/15.
 */
public abstract class StatisticalData extends Statistics {

    /*Body*/
    private List<Link> relatedDatasets;
    private MarkdownSection section; //Explanatory section
    private List<String> notes;//Markdown
    private List<Link> relatedDocuments;
    private List<Link> relatedData;

    public List<Link> getRelatedDocuments() {
        return relatedDocuments;
    }

    public void setRelatedDocuments(List<Link> relatedDocuments) {
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

    public List<Link> getRelatedData() {
        return relatedData;
    }

    public void setRelatedData(List<Link> relatedData) {
        this.relatedData = relatedData;
    }

    public List<Link> getRelatedDatasets() {
        return relatedDatasets;
    }

    public void setRelatedDatasets(List<Link> relatedDatasets) {
        this.relatedDatasets = relatedDatasets;
    }

}
