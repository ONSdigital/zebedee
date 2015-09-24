package com.github.onsdigital.zebedee.content.page.staticpage;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.staticpage.base.BaseStaticPage;
import com.github.onsdigital.zebedee.content.page.staticpage.base.StaticPageSection;

import java.util.List;

/**
 * Created by bren on 29/06/15.
 * Landing page showing links to other static pages
 *
 */
public class StaticLandingPage extends BaseStaticPage {

    private List<StaticPageSection> sections;

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
}
