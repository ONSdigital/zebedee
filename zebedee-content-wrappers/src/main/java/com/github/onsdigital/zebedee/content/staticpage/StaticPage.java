package com.github.onsdigital.zebedee.content.staticpage;

import com.github.onsdigital.zebedee.content.base.ContentType;
import com.github.onsdigital.zebedee.content.staticpage.base.BaseStaticPage;

/**
 * Created by bren on 30/06/15.
 *
 * Simple static page with only markdown content
 */
public class StaticPage extends BaseStaticPage {
    @Override
    public ContentType getType() {
        return ContentType.static_page;
    }
}
