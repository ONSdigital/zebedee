package com.github.onsdigital.zebedee.content.page.compendium;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.link.PageReference;
import com.github.onsdigital.zebedee.content.page.statistics.document.base.StatisticalDocument;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class CompendiumChapter extends StatisticalDocument {

    private List<PageReference> relatedDocuments;
    private PageReference parent;

    @Override
    public PageType getType() {
        return PageType.compendium_chapter;
    }

    @Override
    public List<PageReference> getRelatedData() {
        return super.getRelatedData();
    }

    @Override
    public void setRelatedData(List<PageReference> relatedData) {
        super.setRelatedData(relatedData);
    }

    public PageReference getParent() {
        return parent;
    }

    public void setParent(PageReference parent) {
        this.parent = parent;
    }

    public List<PageReference> getRelatedDocuments() {
        return relatedDocuments;
    }

    public void setRelatedDocuments(List<PageReference> relatedDocuments) {
        this.relatedDocuments = relatedDocuments;
    }
}
