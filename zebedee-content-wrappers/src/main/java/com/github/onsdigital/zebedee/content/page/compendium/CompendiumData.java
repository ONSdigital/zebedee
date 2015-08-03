package com.github.onsdigital.zebedee.content.page.compendium;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.PageReference;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.Dataset;

/**
 * Created by bren on 06/07/15.
 */
public class CompendiumData extends Dataset {

    private PageReference parent;

    @Override
    public PageType getType() {
        return PageType.compendium_data;
    }

    public PageReference getParent() {
        return parent;
    }

    public void setParent(PageReference parent) {
        this.parent = parent;
    }
}
