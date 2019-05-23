package com.github.onsdigital.zebedee.configuration;

import org.apache.commons.lang3.StringUtils;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;

/**
 * Feature flags for Zebedee CMS.
 */
public class CMSFeatureFlags {

    public static final String ENABLE_DATASET_IMPORT = "ENABLE_DATASET_IMPORT";

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

        info().data(ENABLE_DATASET_IMPORT, isDatasetImportEnabled).log("CMS feature flags configurations");
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
            info().data(name, value).log("applying CMS feature flag config value from system.properties");
            return value;
        }

        value = System.getenv(name);
        if (StringUtils.isNoneEmpty(value)) {
            info().data(name, value).log("applying CMS feature flag config value from system.env");
            return value;
        }
        warn().data("name", name).log("CMS config value not found in system.properties or system.env default will be applied");
        return "";
    }

    /**
     * Getter method for singleton instance (Lazy loaded).
     */
    public static CMSFeatureFlags cmsFeatureFlags() {
        if (instance == null) {
            info().log("loading CMS feature flag configuration");
            synchronized (CMSFeatureFlags.class) {
                if (instance == null) {
                    instance = new CMSFeatureFlags();
                }
            }
        }
        return instance;
    }
}
