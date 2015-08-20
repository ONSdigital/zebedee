package com.github.onsdigital.zebedee.reader.configuration;

import com.github.onsdigital.zebedee.util.URIUtils;
import com.github.onsdigital.zebedee.util.VariableUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by bren on 29/07/15.
 * <p>
 * Content reader configuration
 */
public class ReaderConfiguration {

    private static ReaderConfiguration instance;

    private final static String ZEBEDEE_ROOT_ENV = "zebedee_root";
    private final static String CONTENT_DIR_ENV = "content_dir";

    /*Zebedee folder layout*/
    private static final String IN_PROGRESS_FOLDER_NAME = "inprogress";
    private static final String COMPLETE_FOLDER_NAME = "complete";
    private static final String REVIEWED_FOLDER_NAME = "reviewed";
    private static final String COLLECTIONS_FOLDER_NAME = "collections";
    private static final String PUBLISHED_FOLDER_NAME = "master";
    private static final String BULLETINS_FOLDER_NAME = "bulletins";
    private static final String ARTICLES_FOLDER_NAME = "articles";

    private static String collectionsFolder;
    private static String contentDir;

    private ReaderConfiguration() {

    }

    public static ReaderConfiguration getConfiguration() {
        if (instance == null) {
            synchronized (ReaderConfiguration.class) {
                if (instance == null) {
                    init();
                }
            }
        }
        return instance;
    }


    /**
     * Returns collections folder under zebedee root
     *
     * @return
     */
    public String getCollectionsFolder() {
        return collectionsFolder;
    }

    public String getContentDir() {
        return contentDir;
    }

    public String getInProgressFolderName() {
        return IN_PROGRESS_FOLDER_NAME;
    }

    public String getCompleteFolderName() {
        return COMPLETE_FOLDER_NAME;
    }

    public String getReviewedFolderName() {
        return REVIEWED_FOLDER_NAME;
    }

    public String getBulletinsFolderName() {
        return BULLETINS_FOLDER_NAME;
    }

    public String getArticlesFolderName() {
        return ARTICLES_FOLDER_NAME;
    }

    /**
     * Initialize configuration with environment variables
     */
    private static void init() {
        if (instance == null) {
            doInit(null);
            instance = new ReaderConfiguration();
        }
    }


    /**
     * Initialize with given zebedee root dir
     *
     * @param zebedeeRoot
     */
    public static void init(String zebedeeRoot) {
        if (instance == null) {
            doInit(zebedeeRoot);
            instance = new ReaderConfiguration();
        }
    }

    private static void doInit(String zebedeeRoot) {
        String zebedeeRootDir = StringUtils.defaultIfBlank(zebedeeRoot, VariableUtils.getVariableValue(ZEBEDEE_ROOT_ENV));
        String contentDirValue = VariableUtils.getVariableValue(CONTENT_DIR_ENV);

        /*Zebedee Root takes precedence over content dir*/
        if (zebedeeRootDir != null) {
            zebedeeRootDir = URIUtils.removeTrailingSlash(zebedeeRootDir);
            collectionsFolder = zebedeeRootDir + "/zebedee/" + COLLECTIONS_FOLDER_NAME;
            contentDir = zebedeeRootDir + "/zebedee/" + PUBLISHED_FOLDER_NAME;
        } else if (contentDirValue != null) {
            contentDir = URIUtils.removeTrailingSlash(contentDirValue) + "/";
        } else {
            //todo:can not prevent server startup if error just yet, need startup order for Restolino
            System.err.println("Please set either zebedee_root or content_dir");
        }

        dumpConfiguration();

    }

    /**
     * Prints configuration into console
     */
    public static void dumpConfiguration() {
        System.out.println("Collections folder:" + collectionsFolder);
        System.out.println("Published content dir:" + contentDir);
    }

}
