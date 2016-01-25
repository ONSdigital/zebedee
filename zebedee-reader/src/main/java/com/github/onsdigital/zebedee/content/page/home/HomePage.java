package com.github.onsdigital.zebedee.content.page.home;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.taxonomy.base.TaxonomyNode;
import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;
import com.github.onsdigital.zebedee.content.util.ContentConstants;

import java.util.List;

/**
 * Created by bren on 04/06/15.
 * <p>
 * Object mapping for homepage of the website
 * <p>
 * HomePage is considered as the root of Taxonomy. It also contains links and references to non-statistics contents (Methodology, Release, About Us pages , etc.)
 */
public class HomePage extends TaxonomyNode {

    private MarkdownSection intro;

    private List<HomeSection> sections;

    private String serviceMessage;

    public HomePage() {
        intro = new MarkdownSection();
        intro.setTitle(ContentConstants.HOMEPAGE_INTRO_TITLE);
    }

    @Override
    public PageType getType() {
        return PageType.home_page;
    }

    public List<HomeSection> getSections() {
        return sections;
    }

    public void setSections(List<HomeSection> sections) {
        this.sections = sections;
    }

    public MarkdownSection getIntro() {
        return intro;
    }

    public void setIntro(MarkdownSection intro) {
        this.intro = intro;
    }

    public String getServiceMessage() {
        return serviceMessage;
    }

    public void setServiceMessage(String serviceMessage) {
        this.serviceMessage = serviceMessage;
    }
}
