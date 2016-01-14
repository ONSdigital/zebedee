package com.github.onsdigital.zebedee.content.page.census;

/**
 * Created by bren on 14/01/16.
 */
public class HomeSectionCensus {

    private Integer index; //Used for ordering of sections on homepage
    private String title;
    private String text;
    private String size;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Integer getIndex() {
        return index;
    }
}
