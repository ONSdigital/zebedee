package com.github.onsdigital.zebedee.content.page.compendium;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.Alert;
import com.github.onsdigital.zebedee.content.partial.Link;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class CompendiumLandingPage extends Page {

    private List<Link> datasets;
    private List<Link> chapters;
    private List<Link> relatedDocuments;
    private List<Link> relatedData;
    private List<Link> relatedMethodology;
    private List<Link> relatedMethodologyArticle;
    private List<Alert> alerts;

    @Override
    public PageType getType() {
        return PageType.compendium_landing_page;
    }

    public List<Link> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<Link> datasets) {
        this.datasets = datasets;
    }

    public List<Link> getChapters() {
        return chapters;
    }

    public void setChapters(List<Link> chapters) {
        this.chapters = chapters;
    }

    public List<Link> getRelatedMethodology() {
        return relatedMethodology;
    }

    public void setRelatedMethodology(List<Link> relatedMethodology) {
        this.relatedMethodology = relatedMethodology;
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }

    public List<Link> getRelatedMethodologyArticle() {
        return relatedMethodologyArticle;
    }

    public void setRelatedMethodologyArticle(List<Link> relatedMethodologyArticle) {
        this.relatedMethodologyArticle = relatedMethodologyArticle;
    }

    public List<Link> getRelatedDocuments() {
        return relatedDocuments;
    }

    public void setRelatedDocuments(List<Link> relatedDocuments) {
        this.relatedDocuments = relatedDocuments;
    }

    public List<Link> getRelatedData() {
        return relatedData;
    }

    public void setRelatedData(List<Link> relatedData) {
        this.relatedData = relatedData;
    }
}
