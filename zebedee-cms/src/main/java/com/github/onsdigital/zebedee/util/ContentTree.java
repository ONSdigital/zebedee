package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.CollectionReader;

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
     * @param collection
     */
    public static ContentDetail getOverlayed(Collection collection, CollectionReader reader) throws IOException, ZebedeeException {
        ContentDetail publishedDetails = get().clone();
        publishedDetails.overlayDetails(ContentDetailUtil.resolveDetails(collection.inProgress, reader.getInProgress()));
        publishedDetails.overlayDetails(ContentDetailUtil.resolveDetails(collection.complete, reader.getComplete()));
        publishedDetails.overlayDetails(ContentDetailUtil.resolveDetails(collection.reviewed, reader.getReviewed()));
        return publishedDetails;
    }

    public static void dropCache() {
        Log.print("Clearing browser tree cache.");
        publishedContentTree = null;
    }
}
