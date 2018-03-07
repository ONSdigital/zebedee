package com.github.onsdigital.zebedee.content.page;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;

import java.io.IOException;
import java.util.Map;

/**
 * Allows different page types to be attached to different page update hooks.
 */
public class PageTypeUpdateHook implements PageUpdateHook<Page> {

    private final Map<PageType, PageUpdateHook> pageUpdateHooks;

    /**
     * Create a new instance of PageTypeUpdateHook
     * @param pageUpdateHooks
     */
    public PageTypeUpdateHook(Map<PageType, PageUpdateHook> pageUpdateHooks) {
        this.pageUpdateHooks = pageUpdateHooks;
    }

    /**
     * onPageUpdated - call any registered page hooks for the give pages type.
     * @param page
     * @param uri
     * @throws IOException
     */
    @Override
    public void onPageUpdated(Page page, String uri) throws IOException {

        if (pageUpdateHooks.containsKey(page.getType())) {
            pageUpdateHooks.get(page.getType()).onPageUpdated(page, uri);
        }
    }
}
