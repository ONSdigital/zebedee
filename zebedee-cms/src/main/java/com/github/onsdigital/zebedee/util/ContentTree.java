package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.Collection;

import java.io.IOException;

/**
 * Holds a cached instance of the published content tree
 */
public class ContentTree {

    private static ContentDetail publishedContentTree;

    private ContentTree() {
    }

    /**
     * Gets the content tree structure for published content.
     *
     * @return
     * @throws IOException
     */
    public static ContentDetail get() throws IOException {
        if (publishedContentTree == null) {
            synchronized (ContentTree.class) {
                if (publishedContentTree == null) {
                    publishedContentTree = Root.zebedee.published.nestedDetails();
                }
            }
        }
        return publishedContentTree;
    }

    /**
     * Returns a content tree overlayed with the files of the given collection.
     *
     * @return
     */
    public static ContentDetail getOverlayed(Collection collection) throws IOException {
        ContentDetail publishedDetails = get();
        publishedDetails.overlayDetails(collection.inProgress.details());
        publishedDetails.overlayDetails(collection.complete.details());
        publishedDetails.overlayDetails(collection.reviewed.details());
        return publishedDetails;
    }

    public static void dropCache() {
        Log.print("Clearing browser tree cache.");
        publishedContentTree = null;
    }
}
