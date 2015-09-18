package com.github.onsdigital.zebedee.content.page.taxonomy;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.page.taxonomy.base.TaxonomyNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 04/06/15.
 * <p>
 * Represents taxonomy landing page that holds links to sub taxonomy levels
 */
public class TaxonomyLandingPage extends TaxonomyNode {

    //Sections is not particularly a good name. Used for compatibility with Alpha website
    private List<Link> sections = new ArrayList<>();
    private List<Link> highlightedLinks = new ArrayList<>();

    @Override
    public PageType getType() {
        return PageType.taxonomy_landing_page;
    }

    public List<Link> getSections() {
        return sections;
    }

    public void setSections(List<Link> sections) {
        this.sections = sections;
    }

    public List<Link> getHighlightedLinks() {
        return highlightedLinks;
    }

    public void setHighlightedLinks(List<Link> highlightedLinks) {
        this.highlightedLinks = highlightedLinks;
    }
}
