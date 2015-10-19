package com.github.onsdigital.zebedee.content.page.compendium;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.partial.Link;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;

/**
 * Created by bren on 06/07/15.
 */
public class CompendiumData extends DatasetLandingPage {

    private Link parent;

    @Override
    public PageType getType() {
        return PageType.compendium_data;
    }

    public Link getParent() {
        return parent;
    }

    public void setParent(Link parent) {
        this.parent = parent;
    }
}
