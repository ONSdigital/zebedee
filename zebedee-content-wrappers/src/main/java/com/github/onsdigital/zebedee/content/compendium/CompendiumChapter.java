package com.github.onsdigital.zebedee.content.compendium;

import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.link.ContentReference;
import com.github.onsdigital.zebedee.content.statistics.document.base.StatisticalDocument;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class CompendiumChapter extends StatisticalDocument {

    private List<ContentReference> relatedDocuments;
    private ContentReference parent;

    @Override
    public ContentType getType() {
        return ContentType.compendium_chapter;
    }

    @Override
    public List<ContentReference> getRelatedData() {
        return super.getRelatedData();
    }

    @Override
    public void setRelatedData(List<ContentReference> relatedData) {
        super.setRelatedData(relatedData);
    }

    public ContentReference getParent() {
        return parent;
    }

    public void setParent(ContentReference parent) {
        this.parent = parent;
    }

    public List<ContentReference> getRelatedDocuments() {
        return relatedDocuments;
    }

    public void setRelatedDocuments(List<ContentReference> relatedDocuments) {
        this.relatedDocuments = relatedDocuments;
    }
}
