package com.github.onsdigital.zebedee.content.page;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.page.statistics.dataset.DatasetLandingPage;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

public class PageTypeUpdateHookTest {

    @Test
    public void testPageUpdateHooks() throws IOException {

        // Given an instance of pageUpdateHooks with a hook registered
        HashMap<PageType, PageUpdateHook> pageCreationHooks = new HashMap<>();
        MockPageUpdateHook mockPageUpdateHook = new MockPageUpdateHook();

        DatasetLandingPage page = new DatasetLandingPage();
        pageCreationHooks.put(page.getType(), mockPageUpdateHook);

        PageTypeUpdateHook hooks = new PageTypeUpdateHook(pageCreationHooks);

        // When onPageCreated is called with a page of the registered type
        hooks.onPageUpdated(page, "uri");

        // The registered page hook for that page type is called
        Assert.assertTrue(mockPageUpdateHook.wasOnPageUpdatedCalled());
    }
}

