package com.github.onsdigital.zebedee.content.statistics.document.figure.table;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.base.ContentType;

public class Table extends Content {

    private String title;
    private String html;

    @Override
    public ContentType getType() {
        return ContentType.table;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }
}