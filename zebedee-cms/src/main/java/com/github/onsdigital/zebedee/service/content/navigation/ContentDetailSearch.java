package com.github.onsdigital.zebedee.service.content.navigation;

import com.github.onsdigital.zebedee.json.ContentDetail;

/**
 * Created by dave on 7/29/16.
 */
@FunctionalInterface
public interface ContentDetailSearch {

    void search(ContentDetail node);
}
