package com.github.onsdigital.zebedee.content.list;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.link.PageReference;

import java.util.List;

/**
 * Created by bren on 29/07/15.
 *
 * A list of contents pointing to contents
 *
 */
public class PageReferenceList extends Page {

    private List<PageReference> items;

    @Override
    public PageType getType() {
        return PageType.content_list;
    }

    public List<PageReference> getItems() {
        return items;
    }

    public void setItems(List<PageReference> items) {
        this.items = items;
    }
}
