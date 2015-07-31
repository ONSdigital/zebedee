package com.github.onsdigital.zebedee.content.compendium;

import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.link.ContentReference;
import com.github.onsdigital.zebedee.content.statistics.dataset.Dataset;

/**
 * Created by bren on 06/07/15.
 */
public class CompendiumData extends Dataset {

    private ContentReference parent;

    @Override
    public ContentType getType() {
        return ContentType.compendium_data;
    }

    public ContentReference getParent() {
        return parent;
    }

    public void setParent(ContentReference parent) {
        this.parent = parent;
    }
}
