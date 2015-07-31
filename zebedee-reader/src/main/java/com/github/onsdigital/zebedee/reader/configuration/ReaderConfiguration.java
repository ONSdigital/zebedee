package com.github.onsdigital.zebedee.reader.configuration;

import com.github.onsdigital.zebedee.reader.util.VariableUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by bren on 29/07/15.
 * <p>
 * Content reader configuration
 */
public class ReaderConfiguration {
    private final static String ZEBEDEE_ROOT = "zebedee_root";
    private static String defaultZebedeeRoot = "zebedee-reader/target/zebedee";

    private static final String IN_PROGRESS_FOLDER_NAME = "inprogress";
    private static final String COMPLETE_FOLDER_NAME = "complete";
    private static final String REVIEWED_FOLDER_NAME = "reviewed";
    private static final String COLLECTIONS_FOLDER_NAME = "collections";
    private static final String PUBLISHED_FOLDER_NAME = "master";
    private static String collectionsFolder;
    private static String publishedFolder;


    public static String getZebedeeRoot() {
        return StringUtils.defaultIfBlank(VariableUtils.getVariableValue(ZEBEDEE_ROOT), defaultZebedeeRoot);
    }

    static void setDefatultZebedeeRoot(String path) {
        defaultZebedeeRoot = path;
    }

    public static String getInProgressFolderName() {
        return IN_PROGRESS_FOLDER_NAME;
    }

    public static String getCompleteFolderName() {
        return COMPLETE_FOLDER_NAME;
    }

    public static String getReviewedFolderName() {
        return REVIEWED_FOLDER_NAME;
    }

    public static String getPublishedFolderName() {
        if (publishedFolder == null) {
            synchronized (ReaderConfiguration.class) {
                String zebedeeRoot = getZebedeeRoot();
                if (zebedeeRoot != null) {
                    publishedFolder =  zebedeeRoot + (zebedeeRoot.endsWith("/") ? PUBLISHED_FOLDER_NAME : "/" + PUBLISHED_FOLDER_NAME);
                }
            }
        }
        return publishedFolder;
    }

    /**
     * Returns collections folder under zebedee root
     *
     * @return
     */
    public static String getCollectionsFolder() {
        if (collectionsFolder == null) {
            synchronized (ReaderConfiguration.class) {
                String zebedeeRoot = getZebedeeRoot();
                if (zebedeeRoot != null) {
                    collectionsFolder = zebedeeRoot + (zebedeeRoot.endsWith("/") ? COLLECTIONS_FOLDER_NAME : "/" + COLLECTIONS_FOLDER_NAME);
                }
            }
        }

        return collectionsFolder;
    }


}
