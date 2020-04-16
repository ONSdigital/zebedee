package com.github.onsdigital.zebedee.configuration;

import org.apache.commons.lang3.StringUtils;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;

/**
 * Feature flags for Zebedee CMS.
 */
public class CMSFeatureFlags {

    public static final String ENABLE_DATASET_IMPORT = "ENABLE_DATASET_IMPORT";
    public static final String ENABLE_PERMISSIONS_AUTH = "ENABLE_PERMISSIONS_AUTH";
    private static final String ENABLE_VERIFY_PUBLISH_CONTENT = "ENABLE_VERIFY_PUBLISH_CONTENT";
    private static final String ENABLE_DATASET_VERSION_VERIFICATION = "ENABLE_DATASET_VERSION_VERIFICATION";

    /**
     * Singleton instance
     **/
    private static CMSFeatureFlags instance = null;

    private final boolean isDatasetImportEnabled;

    private final boolean isPermissionsAuthEnabled;

    private final boolean isVerifyPublishEnabled;

    private final boolean isDatasetVersionVerificationEnabled;

    /**
     * Construct a new feature flags instance.
     */
    private CMSFeatureFlags() {
        this.isDatasetImportEnabled = Boolean.valueOf(getConfigValue(ENABLE_DATASET_IMPORT));
        this.isPermissionsAuthEnabled = Boolean.valueOf(getConfigValue(ENABLE_PERMISSIONS_AUTH));
        this.isVerifyPublishEnabled = Boolean.valueOf(getConfigValue(ENABLE_VERIFY_PUBLISH_CONTENT));
        this.isDatasetVersionVerificationEnabled = Boolean.valueOf(getConfigValue(ENABLE_DATASET_VERSION_VERIFICATION));

        info().data(ENABLE_DATASET_IMPORT, isDatasetImportEnabled)
                .data(ENABLE_PERMISSIONS_AUTH, isPermissionsAuthEnabled)
                .data(ENABLE_VERIFY_PUBLISH_CONTENT, isVerifyPublishEnabled)
                .data(ENABLE_DATASET_VERSION_VERIFICATION, isDatasetVersionVerificationEnabled)
                .log("CMS feature flags configurations");
    }

    /**
     * @return true if datasets import has been enabled, false otherwise.
     */
    public boolean isEnableDatasetImport() {
        return this.isDatasetImportEnabled;
    }

    /**
     * If true enables API endpoints {@link com.github.onsdigital.zebedee.api.cmd.UserDatasetPermissions},
     * {@link com.github.onsdigital.zebedee.api.cmd.ServiceDatasetPermissions}.
     *
     * @return true if configured to be enabled false otherwise.
     */
    public boolean isPermissionsAuthEnabled() {
        return isPermissionsAuthEnabled;
    }

    /**
     * @return true if the verify publish content feature has been to enabled false (default) otherwise.
     */
    public boolean isVerifyPublishEnabled() {
        return this.isVerifyPublishEnabled;
    }

    /**
     * If true collection approval requests will verify each version of dataset page exists in either the collection
     * reviewed dir or in the published content. If not the approval with be prevented. This is a temp fix to help
     * identify the cause of Trello #4687.
     *
     * @return true if enabled false otherwise.
     */
    public boolean isDatasetVersionVerificationEnabled() {
        return this.isDatasetVersionVerificationEnabled;
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
