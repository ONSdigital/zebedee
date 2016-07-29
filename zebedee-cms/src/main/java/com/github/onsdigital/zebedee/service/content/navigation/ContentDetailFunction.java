package com.github.onsdigital.zebedee.service.content.navigation;

import com.github.onsdigital.zebedee.json.ContentDetail;

/**
 * Created by dave on 7/27/16.
 */
@FunctionalInterface
public interface ContentDetailFunction {

    void apply(ContentDetail node);
}
