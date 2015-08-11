package com.github.onsdigital.zebedee.content.page.statistics.document.figure.table;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;

public class Table extends Page {

    private String title;
    private String html;

    @Override
    public PageType getType() {
        return PageType.table;
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