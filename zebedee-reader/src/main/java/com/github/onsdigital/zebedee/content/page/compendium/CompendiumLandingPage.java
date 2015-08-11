package com.github.onsdigital.zebedee.content.page.compendium;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.Link;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class CompendiumLandingPage extends Page {

    private List<Link> datasets;
    private List<Link> chapters;
    private List<Link> relatedMethodology;

    @Override
    public PageType getType() {
        return PageType.compendium_landing_page;
    }


    public List<Link> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<Link> datasets) {
        this.datasets = datasets;
    }

    public List<Link> getChapters() {
        return chapters;
    }

    public void setChapters(List<Link> chapters) {
        this.chapters = chapters;
    }

    public List<Link> getRelatedMethodology() {
        return relatedMethodology;
    }

    public void setRelatedMethodology(List<Link> relatedMethodology) {
        this.relatedMethodology = relatedMethodology;
    }
}
