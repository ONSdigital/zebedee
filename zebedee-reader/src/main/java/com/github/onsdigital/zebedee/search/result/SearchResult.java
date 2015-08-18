package com.github.onsdigital.zebedee.search.result;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;

import java.net.URI;

public class SearchResult extends Page {

    private PageType type;

    public SearchResult(URI uri, PageType type) {
        this.setUri(uri);
        this.type = type;
    }

    @Override
    public PageType getType() {
        return this.type;
    }
}
