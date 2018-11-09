package com.github.onsdigital.zebedee.configuration;

import org.apache.commons.lang3.StringUtils;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;

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
        this.isDatasetImportEnabled = Boolean.valueOf(getConfigValue(ENABLE_DATASET_IMPORT));

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

    public static String getConfigValue(String name) {
        String value = System.getProperty(name);
        if (StringUtils.isNoneEmpty(value)) {
            logInfo("applying CMS feature flag config value from system.properties")
                    .addParameter(name, value)
                    .log();
            return value;
        }

        value = System.getenv(name);
        if (StringUtils.isNoneEmpty(value)) {
            logInfo("applying CMS feature flag config value from system.env")
                    .addParameter(name, value)
                    .log();
            return value;
        }
        logWarn("CMS config value not found in system.properties or system.env default will be applied")
                .addParameter("name", name)
                .log();
        return "";
    }

    /**
     * Getter method for singleton instance (Lazy loaded).
     */
    public static CMSFeatureFlags cmsFeatureFlags() {
        if (instance == null) {
            logInfo("loading CMS feature flag configuration").log();
            synchronized (CMSFeatureFlags.class) {
                if (instance == null) {
                    instance = new CMSFeatureFlags();
                }
            }
        }
        return instance;
    }
}
