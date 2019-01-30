package com.github.onsdigital.zebedee.content.page;

import com.github.onsdigital.zebedee.content.page.base.Page;

public class MockPageUpdateHook implements PageUpdateHook<Page> {

    private boolean onPageUpdatedCalled = false;

    @Override
    public void onPageUpdated(Page page, String uri) {
        onPageUpdatedCalled = true;
    }

    public boolean wasOnPageUpdatedCalled() {
        return onPageUpdatedCalled;
    }
}
