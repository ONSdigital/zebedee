package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionOwner;
import com.github.onsdigital.zebedee.reader.CollectionReader;

import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 * Holds a cached instance of the published content tree
 */
public class ContentTree {

    private static ContentDetail publishedContentTree;
    private static ContentDetail publishedDataVisualisationsTree;

    private ContentTree() {
    }

    /**
     * Gets the content tree structure for published content.
     *
     * @return
     * @throws IOException
     */

/*    public static ContentDetail get() throws IOException {
        if (publishedContentTree == null) {
            synchronized (ContentTree.class) {
                if (publishedContentTree == null) {
                    publishedContentTree = Root.zebedee.published.nestedDetails();
                }
            }
        }
        return publishedContentTree;
    }*/

    public static ContentDetail get(CollectionOwner collectionOwner) throws IOException {
        ContentDetail contentTree = getContentTree(collectionOwner);
        if (contentTree == null) {
            synchronized (ContentTree.class) {
                if (contentTree == null) {
                    contentTree = Root.zebedee.published.nestedDetails(collectionOwner);
                }
            }
        }
        return contentTree;
    }

    private static ContentDetail getContentTree(CollectionOwner collectionOwner) {
        switch (collectionOwner) {
            case DATA_VISUALISATION:
                return publishedDataVisualisationsTree;
            default:
                return publishedContentTree;
        }
    }


    /**
     * Returns a content tree overlayed with the files of the given collection.
     *
     * @return
     * @param collection
     */
    public static ContentDetail getOverlayed(Collection collection, CollectionReader reader) throws IOException, ZebedeeException {
        ContentDetail publishedDetails = get(collection.description.collectionOwner).clone();
        publishedDetails.overlayDetails(ContentDetailUtil.resolveDetails(collection.inProgress, reader.getInProgress()));
        publishedDetails.overlayDetails(ContentDetailUtil.resolveDetails(collection.complete, reader.getComplete()));
        publishedDetails.overlayDetails(ContentDetailUtil.resolveDetails(collection.reviewed, reader.getReviewed()));
        return publishedDetails;
    }

    public static void dropCache() {
        logDebug("Clearing browser tree cache.").log();
        publishedContentTree = null;
    }
}
