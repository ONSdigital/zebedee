package com.github.onsdigital.zebedee.content.home;

import com.github.onsdigital.zebedee.content.base.ContentDescription;
import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.partial.markdown.MarkdownSection;
import com.github.onsdigital.zebedee.content.taxonomy.base.TaxonomyNode;
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

    public HomePage() {
        ContentDescription description = new ContentDescription();
        description.setTitle(ContentConstants.HOME_TITLE);
        intro = new MarkdownSection();
        intro.setTitle(ContentConstants.HOMEPAGE_INTRO_TITLE);
        intro.setMarkdown(ContentConstants.HOME_PAGE_INTRO_BODY);
        description.setSummary(ContentConstants.HOME_PAGE_INTRO_BODY);
        setDescription(description);
    }

    @Override
    public ContentType getType() {
        return ContentType.home_page;
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
}
