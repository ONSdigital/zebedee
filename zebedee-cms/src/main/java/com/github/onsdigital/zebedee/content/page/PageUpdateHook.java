package com.github.onsdigital.zebedee.content.page;

import java.io.IOException;

/**
 * Generic interface for creating hooks when pages are updated.
 * @param <T>
 */
public interface PageUpdateHook <T> {

    /**
     * The method that gets run when a page is updated
     * @param page - the page that has been updated
     * @param uri - the URI of the updated page.
     * @throws IOException
     */
    void onPageUpdated(T page, String uri) throws IOException;
}
