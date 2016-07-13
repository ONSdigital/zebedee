package com.github.onsdigital.zebedee.content.page.statistics.document.article;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.base.Statistics;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.FigureSection;
import com.github.onsdigital.zebedee.content.partial.Link;

import java.util.ArrayList;
import java.util.List;

public class ArticleDownload extends Statistics {

    private List<DownloadSection> downloads;
    private List<String> markdown;

    private List<Link> relatedData = new ArrayList<>();//Link to data in the article
    private List<Link> relatedDocuments;
    private List<FigureSection> charts = new ArrayList<>();
    private List<FigureSection> tables = new ArrayList<>();
    private List<FigureSection> images = new ArrayList<>();
    private List<FigureSection> equations = new ArrayList<>();
    private List<Link> links;

    public List<DownloadSection> getDownloads() {
        return downloads;
    }

    public void setDownloads(List<DownloadSection> downloads) {
        this.downloads = downloads;
    }

    public List<String> getMarkdown() {
        return markdown;
    }

    public void setMarkdown(List<String> markdown) {
        this.markdown = markdown;
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

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public List<FigureSection> getEquations() {
        return equations;
    }

    public void setEquations(List<FigureSection> equations) {
        this.equations = equations;
    }

    @Override
    public PageType getType() {
        return PageType.article_download;
    }
}
