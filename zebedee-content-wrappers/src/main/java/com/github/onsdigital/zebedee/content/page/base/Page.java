package com.github.onsdigital.zebedee.content.page.base;

import com.github.onsdigital.zebedee.content.base.Content;

/**
 * Created by bren on 10/06/15.
 * <p>
 * This is the generic content object that that has common properties of all content types on the website
 */
public abstract class Page extends Content {

    private PageType type;

    private PageDescription description;

    public Page() {
        this.type = getType();
    }

    public abstract PageType getType();

    public PageDescription getDescription() {
        return description;
    }

    public void setDescription(PageDescription description) {
        this.description = description;
    }

}
