package com.github.onsdigital.zebedee.content.list;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.link.ContentReference;

import java.util.List;

/**
 * Created by bren on 29/07/15.
 *
 * A list of contents pointing to contents
 *
 */
public class ContentList extends Content {

    private List<ContentReference> items;

    @Override
    public ContentType getType() {
        return ContentType.content_list;
    }

    public List<ContentReference> getItems() {
        return items;
    }

    public void setItems(List<ContentReference> items) {
        this.items = items;
    }
}
