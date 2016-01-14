package com.github.onsdigital.zebedee.content.page.census;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.taxonomy.base.TaxonomyNode;
import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;

import java.util.List;

/**
 * Created by bren on 14/01/16.
 */
public class HomePageCensus extends TaxonomyNode {

    private MarkdownSection intro = new MarkdownSection();
    private List<HomePageCensus> sections;

    @Override
    public PageType getType() {
        return PageType.home_page_census;
    }

    public MarkdownSection getIntro() {
        return intro;
    }

    public void setIntro(MarkdownSection intro) {
        this.intro = intro;
    }

    public List<HomePageCensus> getSections() {
        return sections;
    }

    public void setSections(List<HomePageCensus> sections) {
        this.sections = sections;
    }
}
