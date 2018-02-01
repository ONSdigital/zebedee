package com.github.onsdigital.zebedee.content.page.statistics.document.article;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DownloadSection;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.page.statistics.document.base.StatisticalDocument;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class Article extends StatisticalDocument {

    /*Body*/
    private List<Link> relatedArticles;
    private List<DownloadSection> pdfTable;
    private Boolean isPrototypeArticle;
    private String imageUri;

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public List<DownloadSection> getPdfTable() {
        return pdfTable;
    }

    public void setPdfTable(List<DownloadSection> pdfTable) {
        this.pdfTable = pdfTable;
    }

    @Override
    public PageType getType() {
        return PageType.article;
    }

    public void setRelatedArticles(List<Link> relatedArticles) {
        this.relatedArticles = relatedArticles;
    }

    public List<Link> getRelatedArticles() {
        return relatedArticles;
    }

    public Boolean getPrototypeArticle() {
        return isPrototypeArticle;
    }

    public void setPrototypeArticle(Boolean prototypeArticle) {
        isPrototypeArticle = prototypeArticle;
    }
}
