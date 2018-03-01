package com.github.onsdigital.zebedee.content.page;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;

import java.io.IOException;
import java.util.Map;

public class PageUpdateHooks implements PageUpdateHook {

    Map<PageType, PageUpdateHook> pageCreationHooks;

    public PageUpdateHooks(Map<PageType, PageUpdateHook> pageCreationHooks) {
        this.pageCreationHooks = pageCreationHooks;
    }

    @Override
    public void OnPageCreate(Page page, String uri) throws IOException {

        if (pageCreationHooks.containsKey(page.getType())) {
            pageCreationHooks.get(page.getType()).OnPageCreate(page, uri);
        }
    }
}
