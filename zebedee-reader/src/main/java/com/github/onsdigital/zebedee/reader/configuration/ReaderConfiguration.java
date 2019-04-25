package com.github.onsdigital.zebedee.reader.configuration;

import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static com.github.onsdigital.zebedee.content.page.base.PageType.api_dataset;
import static com.github.onsdigital.zebedee.content.page.base.PageType.api_dataset_landing_page;
import static com.github.onsdigital.zebedee.logging.ReaderLogger.error;
import static com.github.onsdigital.zebedee.logging.ReaderLogger.info;
import static com.github.onsdigital.zebedee.util.VariableUtils.getVariableValue;
import static java.text.MessageFormat.format;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * Content reader configuration
 */
public class ReaderConfiguration {

    private static ReaderConfiguration INSTANCE = null;

    private static final String CMD_CONFIG_MISSING = "cmd feature flag is enabled for zebedee reader but expected " +
            "configuration value is missing: {0}";

    private static final String ENABLE_DATASET_IMPORT = "ENABLE_DATASET_IMPORT";

    private final static String ZEBEDEE_ROOT_ENV = "zebedee_root";
    private final static String CONTENT_DIR_ENV = "content_dir";

    private static final String PUBLISHED_FOLDER_NAME = "master";
    private static final String COLLECTIONS_FOLDER_NAME = "collections";
    private static final String IN_PROGRESS_FOLDER_NAME = "inprogress";
    private static final String COMPLETE_FOLDER_NAME = "complete";
    private static final String REVIEWED_FOLDER_NAME = "reviewed";

    private static final String BULLETINS_FOLDER_NAME = "bulletins";
    private static final String ARTICLES_FOLDER_NAME = "articles";
    private static final String COMPENDIUM_FOLDER_NAME = "compendium";

    private static final String DATASET_API_URL_KEY = "DATASET_API_URL";
    private static final String DATASET_API_AUTH_TOKEN_KEY = "DATASET_API_AUTH_TOKEN";
    private static final String SERVICE_AUTH_TOKEN_KEY = "SERVICE_AUTH_TOKEN";

    private String zebedeeRootDir;
    private String collectionsDir;
    private String contentDir;
    private String inProgressDirName;
    private String completeDirName;
    private String reviewedDirName;
    private String bulletinsDirName;
    private String articlesDirName;
    private String compendiumDirName;
    private String datasetAPIHost;
    private String datasetAPIAuthToken;
    private String serviceAuthToken;
    private boolean datasetImportEnabled;
    private Set<PageType> datasetImportPageTypes;


    public static ReaderConfiguration init(String zebedeeRootDir) {
        synchronized (ReaderConfiguration.class) {
            try {
                info().log("loading reader configuration");
                INSTANCE = new ReaderConfiguration(zebedeeRootDir);
            } catch (UncheckedReaderConfigException ex) {
                throw error().logException(ex, "error loading reader configuration exiting application");
            }
        }
        return INSTANCE;
    }

    public static ReaderConfiguration get() {
        if (INSTANCE == null) {
            synchronized (ReaderConfiguration.class) {
                if (INSTANCE == null) {
                    try {
                        info().log("loading reader configuration");
                        INSTANCE = new ReaderConfiguration();
                    } catch (UncheckedReaderConfigException ex) {
                        throw error().logException(ex, "error loading reader configuration");
                    }
                }
            }
        }
        return INSTANCE;
    }

    /**
     * For unit tests only.
     */
    public static void clear() {
        INSTANCE = null;
    }

    private ReaderConfiguration(String zebedeeRootDir) {
        zebedeeRootDir = defaultIfBlank(zebedeeRootDir, getVariableValue(ZEBEDEE_ROOT_ENV));
        String contentDir = getVariableValue(CONTENT_DIR_ENV);

        if (isEmpty(zebedeeRootDir) && isEmpty(contentDir)) {
            throw new UncheckedReaderConfigException(format("reader config invalid expected env var for one of " +
                    "{0}/{1} but none found", ZEBEDEE_ROOT_ENV, CONTENT_DIR_ENV));
        }

        if (isNotEmpty(zebedeeRootDir)) {
            this.zebedeeRootDir = URIUtils.removeTrailingSlash(zebedeeRootDir);
            String zebedeeFolderFormat = "{0}/zebedee/{1}";
            this.collectionsDir = format(zebedeeFolderFormat, this.zebedeeRootDir, COLLECTIONS_FOLDER_NAME);
            this.contentDir = format(zebedeeFolderFormat, this.zebedeeRootDir, PUBLISHED_FOLDER_NAME);
        } else {
            this.contentDir = URIUtils.removeTrailingSlash(contentDir) + "/";
        }

        this.inProgressDirName = IN_PROGRESS_FOLDER_NAME;
        this.completeDirName = COMPLETE_FOLDER_NAME;
        this.reviewedDirName = REVIEWED_FOLDER_NAME;
        this.bulletinsDirName = BULLETINS_FOLDER_NAME;
        this.articlesDirName = ARTICLES_FOLDER_NAME;
        this.compendiumDirName = COMPENDIUM_FOLDER_NAME;

        this.datasetImportEnabled = Boolean.valueOf(getVariableValue(ENABLE_DATASET_IMPORT));

        if (datasetImportEnabled) {
            this.serviceAuthToken = validateCMDConfig(SERVICE_AUTH_TOKEN_KEY);
            this.datasetAPIHost = validateCMDConfig(DATASET_API_URL_KEY);
            this.datasetAPIAuthToken = validateCMDConfig(DATASET_API_AUTH_TOKEN_KEY);
            this.datasetImportPageTypes = new HashSet<>(Arrays.asList(api_dataset, api_dataset_landing_page));
            info().data(ENABLE_DATASET_IMPORT, true).log("CMD feature flag enabled for zebedee reader");
        } else {
            this.serviceAuthToken = "";
            this.datasetAPIHost = "";
            this.datasetAPIAuthToken = "";
            this.datasetImportPageTypes = new HashSet<>();
            info().data(ENABLE_DATASET_IMPORT, false).log("CMD feature flag disabled for zebedee reader");
        }

        info().data("zebedee_root_dir", this.zebedeeRootDir)
                .data("collections_dir", collectionsDir)
                .data("content_dir", this.contentDir)
                .data("in_progress_dir", inProgressDirName)
                .data("complete_dir", completeDirName)
                .data("reviewed_dir", reviewedDirName)
                .data("bulletins_dir", bulletinsDirName)
                .data("articles_dir", articlesDirName)
                .data("compendium_dir", compendiumDirName)
                .data("dataset_import_enabled", datasetImportEnabled)
                .log("zebedee reader configuration");
    }

    private ReaderConfiguration() {
        this(null);
    }

    private String validateCMDConfig(String key) {
        String value = getVariableValue(key);
        if (StringUtils.isEmpty(value)) {
            throw new UncheckedReaderConfigException(format(CMD_CONFIG_MISSING, key));
        }
        return value;
    }

    public String getCollectionsDir() {
        return collectionsDir;
    }

    public String getContentDir() {
        return contentDir;
    }

    public String getInProgressFolderName() {
        return inProgressDirName;
    }

    public String getCompleteFolderName() {
        return completeDirName;
    }

    public String getReviewedFolderName() {
        return reviewedDirName;
    }

    public String getBulletinsFolderName() {
        return bulletinsDirName;
    }

    public String getArticlesFolderName() {
        return articlesDirName;
    }

    public String getCompendiumFolderName() {
        return compendiumDirName;
    }

    public boolean isDatasetImportEnabled() {
        return datasetImportEnabled;
    }

    public String getDatasetAPIHost() {
        return datasetAPIHost;
    }

    public String getDatasetAPIAuthToken() {
        return datasetAPIAuthToken;
    }

    public String getServiceAuthToken() {
        return "Bearer " + serviceAuthToken;
    }

    public Set<PageType> getDatasetImportPageTypes() {
        return datasetImportPageTypes;
    }
}
