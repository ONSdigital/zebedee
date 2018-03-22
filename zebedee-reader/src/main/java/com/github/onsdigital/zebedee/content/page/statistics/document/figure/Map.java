package com.github.onsdigital.zebedee.content.page.statistics.document.figure;

import com.github.onsdigital.zebedee.content.page.base.PageType;

/**
 * Zebedee feels it ought to know about all content types, but it doesn't really need to know anything about a map,
 * other than filename.
 */
public class Map extends FigureBase {

    private String filename;

    @Override
    public PageType getType() {
        return PageType.map;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}
