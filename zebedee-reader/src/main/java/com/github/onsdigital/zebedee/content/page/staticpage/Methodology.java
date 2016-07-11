package com.github.onsdigital.zebedee.content.page.staticpage;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;

import java.util.List;

/**
 * Created by bren on 06/07/15.
 */
public class Methodology extends StaticArticle {
    private List<DownloadSection> pdfTable;

    public List<DownloadSection> getPdfTable() {
        return pdfTable;
    }

    public void setPdfTable(List<DownloadSection> pdfTable) {
        this.pdfTable = pdfTable;
    }

    @Override
    public PageType getType() {
        return PageType.static_methodology;
    }
}
