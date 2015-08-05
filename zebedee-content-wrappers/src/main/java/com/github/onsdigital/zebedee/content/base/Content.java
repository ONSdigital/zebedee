package com.github.onsdigital.zebedee.content.base;

import com.github.onsdigital.zebedee.content.util.ContentUtil;

/**
 * Created by bren on 10/06/15.
 * <p>
 * This is the generic content object that that has common properties of all content types on the website
 */
public abstract class Content implements Cloneable {

    private ContentType type;

    private ContentDescription description;

    public Content() {
        this.type = getType();
    }

    public abstract ContentType getType();

    public ContentDescription getDescription() {
        return description;
    }

    public void setDescription(ContentDescription description) {
        this.description = description;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return ContentUtil.clone(this);
    }
}
