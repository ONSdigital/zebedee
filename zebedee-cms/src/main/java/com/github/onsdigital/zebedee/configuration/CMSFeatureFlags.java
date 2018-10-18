package com.github.onsdigital.zebedee.configuration;

import static com.github.onsdigital.zebedee.configuration.Configuration.getValue;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Feature flags for Zebedee CMS.
 */
public class CMSFeatureFlags {

    private static final String ENABLE_DATASET_IMPORT = "ENABLE_DATASET_IMPORT";

    /**
     * Singleton instance
     **/
    private static CMSFeatureFlags instance = null;

    private final boolean isDatasetImportEnabled;

    /**
     * Construct a new feature flags instance.
     */
    private CMSFeatureFlags() {
        this.isDatasetImportEnabled = Boolean.valueOf(getValue(ENABLE_DATASET_IMPORT));

        logInfo("CMS feature flags configurations")
                .addParameter(ENABLE_DATASET_IMPORT, isDatasetImportEnabled)
                .log();
    }

    /**
     * @return true if datasets import has been enabled, false otherwise.
     */
    public boolean isEnableDatasetImport() {
        return this.isDatasetImportEnabled;
    }

    /**
     * Getter method for singleton instance (Lazy loaded).
     */
    public static CMSFeatureFlags cmsFeatureFlags() {
        if (instance == null) {
            synchronized (CMSFeatureFlags.class) {
                if (instance == null) {
                    instance = new CMSFeatureFlags();
                }
            }
        }
        return instance;
    }
}
