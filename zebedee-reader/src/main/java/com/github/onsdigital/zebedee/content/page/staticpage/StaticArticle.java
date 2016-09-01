package com.github.onsdigital.zebedee.content.page.staticpage;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.FigureSection;
import com.github.onsdigital.zebedee.content.partial.Alert;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 29/06/15.
 */
public class StaticArticle extends Page {

    private List<Link> relatedData;
    private List<Link> relatedDocuments;
    private List<Link> links;
    private List<DownloadSection> downloads;

    private List<MarkdownSection> sections = new ArrayList<>();
    private List<MarkdownSection> accordion = new ArrayList<>();
    private List<FigureSection> charts = new ArrayList<>();
    private List<FigureSection> tables = new ArrayList<>();
    private List<FigureSection> images = new ArrayList<>();
    private List<FigureSection> equations = new ArrayList<>();

    private List<Alert> alerts;

    @Override
    public PageType getType() {
        return PageType.static_article;
    }

    public List<Link> getRelatedData() {
        return relatedData;
    }

    public void setRelatedData(List<Link> relatedData) {
        this.relatedData = relatedData;
    }

    public List<Link> getRelatedDocuments() {
        return relatedDocuments;
    }

    public void setRelatedDocuments(List<Link> relatedDocuments) {
        this.relatedDocuments = relatedDocuments;
    }

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

    public List<FigureSection> getCharts() {
        return charts;
    }

    public void setCharts(List<FigureSection> charts) {
        this.charts = charts;
    }

    public List<FigureSection> getTables() {
        return tables;
    }

    public void setTables(List<FigureSection> tables) {
        this.tables = tables;
    }

    public List<FigureSection> getImages() {
        return images;
    }

    public void setImages(List<FigureSection> images) {
        this.images = images;
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }

    public List<FigureSection> getEquations() {
        return equations;
    }

    public void setEquations(List<FigureSection> equations) {
        this.equations = equations;
    }

    public List<DownloadSection> getDownloads() {
        return downloads;
    }

    public void setDownloads(List<DownloadSection> downloads) {
        this.downloads = downloads;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }
}
