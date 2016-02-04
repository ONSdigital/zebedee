package com.github.onsdigital.zebedee.content.page.staticpage;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.staticpage.base.BaseStaticPage;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.content.partial.Link;

import java.util.List;

public class MethodologyDownload extends BaseStaticPage {

    private List<Link> relatedDocuments;
    private List<Link> relatedDatasets;
    private List<DownloadSection> pdfDownloads;

    public List<Link> getRelatedDocuments() {
        return relatedDocuments;
    }

    public void setRelatedDocuments(List<Link> relatedDocuments) {
        this.relatedDocuments = relatedDocuments;
    }

    public List<Link> getRelatedDatasets() {
        return relatedDatasets;
    }

    public void setRelatedDatasets(List<Link> relatedDatasets) {
        this.relatedDatasets = relatedDatasets;
    }

    public List<DownloadSection> getPdfDownloads() {
        return pdfDownloads;
    }

    public void setPdfDownloads(List<DownloadSection> pdfDownloads) {
        this.pdfDownloads = pdfDownloads;
    }

    @Override
    public PageType getType() {
        return PageType.static_methodology_download;
    }
}
