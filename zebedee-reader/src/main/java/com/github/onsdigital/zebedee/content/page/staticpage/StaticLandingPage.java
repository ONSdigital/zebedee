package com.github.onsdigital.zebedee.content.page.staticpage;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.page.staticpage.base.StaticPageSection;

import java.util.List;

/**
 * Created by bren on 29/06/15.
 * Landing page showing links to other static pages
 *
 */
public class StaticLandingPage extends Page {

    private List<StaticPageSection> sections;
    private List<Link> links;

    @Override
    public PageType getType() {
        return PageType.static_landing_page;
    }

    public List<StaticPageSection> getSections() {
        return sections;
    }

    public void setSections(List<StaticPageSection> sections) {
        this.sections = sections;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }
}
