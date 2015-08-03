package com.github.onsdigital.zebedee.content.page.staticpage;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.staticpage.base.BaseStaticPage;

/**
 * Created by bren on 30/06/15.
 *
 * Simple static page with only markdown content
 */
public class StaticPage extends BaseStaticPage {
    @Override
    public PageType getType() {
        return PageType.static_page;
    }
}
