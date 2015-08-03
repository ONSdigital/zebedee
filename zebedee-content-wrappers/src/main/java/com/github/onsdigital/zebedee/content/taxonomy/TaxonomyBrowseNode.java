package com.github.onsdigital.zebedee.content.taxonomy;

import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.link.ContentReference;
import com.github.onsdigital.zebedee.content.taxonomy.base.TaxonomyNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bren on 04/06/15.
 * <p>
 * Represents taxonomy browse node that holds links to sub taxonomy levels
 */
public class TaxonomyBrowseNode extends TaxonomyNode {

    //Sections is not particularly a good name. Used for compatibility with Alpha website
    private List<ContentReference> sections = new ArrayList<>();

    @Override
    public ContentType getType() {
        return ContentType.taxonomy_landing_page;
    }

    public List<ContentReference> getSections() {
        return sections;
    }

    public void setSections(List<ContentReference> sections) {
        this.sections = sections;
    }
}
