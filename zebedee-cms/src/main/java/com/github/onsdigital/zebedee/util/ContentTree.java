package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.service.ContentDeleteService;
import com.github.onsdigital.zebedee.service.ServiceSupplier;

import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;

/**
 * Holds a cached instance of the published content tree
 */
public class ContentTree {

    private static ContentDetail publishedContentTree;

    private static ContentDeleteService contentDeleteService = ContentDeleteService.getInstance();
    private static ServiceSupplier<Zebedee> zebedeeServiceSupplier = () -> Root.zebedee;

    private ContentTree() {
    }

    /**
     * Gets the content tree structure for published content.
     *
     * @return
     * @throws IOException
     */
    public static ContentDetail get() throws IOException {
        ContentDetail contentTree = publishedContentTree;
        if (contentTree == null) {
            synchronized (ContentTree.class) {
                if (contentTree == null) {
                    publishedContentTree = zebedeeServiceSupplier.getService().getPublished().nestedDetails();
                }
            }
        }
        return publishedContentTree;
    }


    /**
     * Returns a content tree overlayed with the files of the given collection.
     *
     * @param collection
     * @return
     */
    public static ContentDetail getOverlayed(Collection collection, CollectionReader reader) throws IOException, ZebedeeException {
        ContentDetail publishedDetails = get().clone();
        publishedDetails.overlayDetails(ContentDetailUtil.resolveDetails(collection.inProgress, reader.getInProgress()));
        publishedDetails.overlayDetails(ContentDetailUtil.resolveDetails(collection.complete, reader.getComplete()));
        publishedDetails.overlayDetails(ContentDetailUtil.resolveDetails(collection.reviewed, reader.getReviewed()));
        contentDeleteService.overlayDeletedNodesInBrowseTree(publishedDetails);
        return publishedDetails;
    }

    public static void dropCache() {
        logDebug("Clearing browser tree cache.").log();
        publishedContentTree = null;
    }
}
