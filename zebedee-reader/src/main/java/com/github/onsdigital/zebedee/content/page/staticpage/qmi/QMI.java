package com.github.onsdigital.zebedee.content.page.staticpage.qmi;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.staticpage.base.BaseStaticPage;
import com.github.onsdigital.zebedee.content.partial.Link;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class QMI extends BaseStaticPage {

    private List<Link> relatedDocuments;
    private List<Link> relatedDatasets;

    @Override
    public PageType getType() {
        return PageType.static_qmi;
    }

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
}
