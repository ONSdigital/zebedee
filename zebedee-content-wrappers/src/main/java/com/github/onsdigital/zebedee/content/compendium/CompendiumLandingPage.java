package com.github.onsdigital.zebedee.content.compendium;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.link.ContentReference;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 */
public class CompendiumLandingPage extends Content {

    private List<ContentReference> datasets;
    private List<ContentReference> chapters;
    private List<ContentReference> relatedMethodology;

    @Override
    public ContentType getType() {
        return ContentType.compendium_landing_page;
    }


    public List<ContentReference> getDatasets() {
        return datasets;
    }

    public void setDatasets(List<ContentReference> datasets) {
        this.datasets = datasets;
    }

    public List<ContentReference> getChapters() {
        return chapters;
    }

    public void setChapters(List<ContentReference> chapters) {
        this.chapters = chapters;
    }

    public List<ContentReference> getRelatedMethodology() {
        return relatedMethodology;
    }

    public void setRelatedMethodology(List<ContentReference> relatedMethodology) {
        this.relatedMethodology = relatedMethodology;
    }
}
