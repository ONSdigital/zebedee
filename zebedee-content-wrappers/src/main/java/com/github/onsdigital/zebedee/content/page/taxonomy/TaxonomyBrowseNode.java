package com.github.onsdigital.zebedee.content.page.taxonomy;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.link.PageReference;
import com.github.onsdigital.zebedee.content.page.taxonomy.base.TaxonomyNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 04/06/15.
 * <p>
 * Represents taxonomy browse node that holds links to sub taxonomy levels
 */
public class TaxonomyBrowseNode extends TaxonomyNode {

    //Sections is not particularly a good name. Used for compatibility with Alpha website
    private List<PageReference> sections = new ArrayList<>();

    @Override
    public PageType getType() {
        return PageType.taxonomy_landing_page;
    }

    public List<PageReference> getSections() {
        return sections;
    }

    public void setSections(List<PageReference> sections) {
        this.sections = sections;
    }
}
