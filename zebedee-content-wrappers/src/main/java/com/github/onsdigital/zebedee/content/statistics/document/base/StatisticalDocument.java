package com.github.onsdigital.zebedee.content.statistics.document.base;

import com.github.onsdigital.zebedee.content.link.ContentReference;
import com.github.onsdigital.zebedee.content.statistics.document.figure.FigureSection;
import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;
import com.github.onsdigital.zebedee.content.statistics.base.Statistics;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 06/06/15.
 */
public abstract class StatisticalDocument extends Statistics {

    /*Body*/
    private List<MarkdownSection> sections = new ArrayList<>();
    private List<MarkdownSection> accordion = new ArrayList<>();
    private List<ContentReference> relatedData = new ArrayList<>();//Link to data in the article
    private List<FigureSection> charts = new ArrayList<>();
    private List<FigureSection> tables = new ArrayList<>();

    public List<MarkdownSection> getSections() {
        return sections;
    }

    public void setSections(List<MarkdownSection> sections) {
        this.sections = sections;
    }

    public List<MarkdownSection> getAccordion() {
        return accordion;
    }

    public void setAccordion(List<MarkdownSection> accordion) {
        this.accordion = accordion;
    }

    public List<ContentReference> getRelatedData() {
        return relatedData;
    }

    public void setRelatedData(List<ContentReference> relatedData) {
        this.relatedData = relatedData;
    }

    public List<FigureSection> getCharts() { return charts; }

    public void setCharts(List<FigureSection> charts) { this.charts = charts; }

    public List<FigureSection> getTables() { return tables; }

    public void setTables(List<FigureSection> tables) { this.tables = tables; }

}
