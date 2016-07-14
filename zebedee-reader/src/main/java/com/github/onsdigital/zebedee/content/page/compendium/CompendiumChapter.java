package com.github.onsdigital.zebedee.content.page.compendium;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.content.page.statistics.document.base.StatisticalDocument;
import com.github.onsdigital.zebedee.content.partial.Link;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class CompendiumChapter extends StatisticalDocument {

    private Link parent;

    private List<DownloadSection> pdfTable;

    @Override
    public PageType getType() {
        return PageType.compendium_chapter;
    }

    @Override
    public List<Link> getRelatedData() {
        return super.getRelatedData();
    }

    @Override
    public void setRelatedData(List<Link> relatedData) {
        super.setRelatedData(relatedData);
    }

    public Link getParent() {
        return parent;
    }

    public void setParent(Link parent) {
        this.parent = parent;
    }

    public List<DownloadSection> getPdfTable() {
        return pdfTable;
    }

    public void setPdfTable(List<DownloadSection> pdfTable) {
        this.pdfTable = pdfTable;
    }
}
