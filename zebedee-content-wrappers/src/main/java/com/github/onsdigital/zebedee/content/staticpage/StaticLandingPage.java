package com.github.onsdigital.zebedee.content.staticpage;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.link.Link;
import com.github.onsdigital.zebedee.content.staticpage.base.StaticPageSection;

import java.util.List;

/**
 * Created by bren on 29/06/15.
 * Landing page showing links to other static pages
 *
 */
public class StaticLandingPage extends Content {

    private List<StaticPageSection> sections;
    private List<Link> links;

    @Override
    public ContentType getType() {
        return ContentType.static_landing_page;
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
