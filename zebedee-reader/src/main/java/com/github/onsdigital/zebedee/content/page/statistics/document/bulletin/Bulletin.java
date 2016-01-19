package com.github.onsdigital.zebedee.content.page.statistics.document.bulletin;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.page.statistics.document.base.StatisticalDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 04/06/15.
// */
public class Bulletin extends StatisticalDocument {

    private List<Link> relatedBulletins = new ArrayList<>();

    private List<DownloadSection> pdfTable;

    public List<DownloadSection> getPdfTable() {
        return pdfTable;
    }

    public void setPdfTable(List<DownloadSection> pdfTable) {
        this.pdfTable = pdfTable;
    }

    @Override
    public PageType getType() {
        return PageType.bulletin;
    }

    public List<Link> getRelatedBulletins() {
        return relatedBulletins;
    }

    public void setRelatedBulletins(List<Link> relatedBulletins) {
        this.relatedBulletins = relatedBulletins;
    }


    public static void main(String args[]) {
        Bulletin bulletin = new Bulletin();
    }
}
