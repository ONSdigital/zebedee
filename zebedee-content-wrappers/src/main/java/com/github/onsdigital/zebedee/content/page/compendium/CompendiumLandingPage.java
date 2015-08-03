package com.github.onsdigital.zebedee.content.page.compendium;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.PageReference;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class CompendiumLandingPage extends Page {

    private List<PageReference> datasets;
    private List<PageReference> chapters;
    private List<PageReference> relatedMethodology;

    @Override
    public PageType getType() {
        return PageType.compendium_landing_page;
    }


    public List<PageReference> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<PageReference> datasets) {
        this.datasets = datasets;
    }

    public List<PageReference> getChapters() {
        return chapters;
    }

    public void setChapters(List<PageReference> chapters) {
        this.chapters = chapters;
    }

    public List<PageReference> getRelatedMethodology() {
        return relatedMethodology;
    }

    public void setRelatedMethodology(List<PageReference> relatedMethodology) {
        this.relatedMethodology = relatedMethodology;
    }
}
