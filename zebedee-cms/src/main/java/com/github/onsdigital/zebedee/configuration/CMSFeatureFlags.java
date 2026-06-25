package com.github.onsdigital.zebedee.configuration;

import org.apache.commons.lang3.StringUtils;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.warn;

/**
 * Feature flags for Zebedee CMS.
 */
public class CMSFeatureFlags {

    public static final String ENABLE_DATASET_IMPORT = "ENABLE_DATASET_IMPORT";
    public static final String ENABLE_VERIFY_PUBLISH_CONTENT = "ENABLE_VERIFY_PUBLISH_CONTENT";
    public static final String ENABLE_IMAGE_PUBLISHING = "ENABLE_IMAGE_PUBLISHING";
    private static final String ENABLE_JWT_SESSIONS = "ENABLE_JWT_SESSIONS";
    private static final String ENABLE_PERMISSIONS_API = "ENABLE_PERMISSIONS_API";
    public static final String ENABLE_KAFKA = "ENABLE_KAFKA";
    public static final String ENABLE_STATIC_FILES_PUBLISHING = "ENABLE_STATIC_FILES_PUBLISHING";
    public static final String ENABLE_REDIRECT_API = "ENABLE_REDIRECT_API";
    public static final String ENABLE_COLLECTION_WRITE_LOCKING = "ENABLE_COLLECTION_WRITE_LOCKING";

    /**
     * Singleton instance
     **/
    private static CMSFeatureFlags instance = null;
    private final boolean isDatasetImportEnabled;
    private final boolean isVerifyPublishEnabled;
    private final boolean isImagePublishingEnabled;
    private final boolean isJwtSessionsEnabled;
    private final boolean isPermissionsAPIEnabled;
    private final boolean isKafkaEnabled;
    private final boolean isStaticFilesPublishingEnabled;
    private final boolean isRedirectAPIEnabled;
    private final boolean isCollectionWriteLockingEnabled;
    /**
     * Construct a new feature flags instance.
     */
    private CMSFeatureFlags() {
        this.isDatasetImportEnabled = getBooleanConfigValue(ENABLE_DATASET_IMPORT);
        this.isVerifyPublishEnabled = getBooleanConfigValue(ENABLE_VERIFY_PUBLISH_CONTENT);
        this.isImagePublishingEnabled = getBooleanConfigValue(ENABLE_IMAGE_PUBLISHING);
        this.isJwtSessionsEnabled = getBooleanConfigValue(ENABLE_JWT_SESSIONS, false);
        this.isPermissionsAPIEnabled = getBooleanConfigValue(ENABLE_PERMISSIONS_API);
        this.isKafkaEnabled = getBooleanConfigValue(ENABLE_KAFKA);
        this.isStaticFilesPublishingEnabled = getBooleanConfigValue(ENABLE_STATIC_FILES_PUBLISHING);
        this.isRedirectAPIEnabled = getBooleanConfigValue(ENABLE_REDIRECT_API);
        this.isCollectionWriteLockingEnabled = getBooleanConfigValue(ENABLE_COLLECTION_WRITE_LOCKING);

        info().data(ENABLE_DATASET_IMPORT, isDatasetImportEnabled)
                .data(ENABLE_VERIFY_PUBLISH_CONTENT, isVerifyPublishEnabled)
                .data(ENABLE_IMAGE_PUBLISHING, isImagePublishingEnabled)
                .data(ENABLE_PERMISSIONS_API, isPermissionsAPIEnabled)
                .data(ENABLE_KAFKA, isKafkaEnabled)
                .data(ENABLE_STATIC_FILES_PUBLISHING, isStaticFilesPublishingEnabled)
                .data(ENABLE_REDIRECT_API, isRedirectAPIEnabled)
                .data(ENABLE_COLLECTION_WRITE_LOCKING, isCollectionWriteLockingEnabled)
                .log("CMS feature flags configurations");
    }

    public static void reset() {
        warn().log("resetting CMS feature flag configuration - probably should only be done in testing");
        synchronized (CMSFeatureFlags.class) {
            instance = null;
        }
    }

    /**
     * @return true if datasets import has been enabled, false otherwise.
     */
    public boolean isEnableDatasetImport() {
        return this.isDatasetImportEnabled;
    }

    /**
     * @return true if the verify publish content feature has been to enabled false (default) otherwise.
     */
    public boolean isVerifyPublishEnabled() {
        return this.isVerifyPublishEnabled;
    }

    public boolean isImagePublishingEnabled() {
        return isImagePublishingEnabled;
    }

    public boolean isJwtSessionsEnabled() {
        return isJwtSessionsEnabled;
    }
    public boolean isPermissionsAPIEnabled() {
        return isPermissionsAPIEnabled;
    }

    public boolean isKafkaEnabled() {
        return isKafkaEnabled;
    }

    public boolean isStaticFilesPublishingEnabled() {
        return isStaticFilesPublishingEnabled;
    }

    public boolean isRedirectAPIEnabled() {
        return isRedirectAPIEnabled;
    }

    public boolean isCollectionWriteLockingEnabled() {
        return isCollectionWriteLockingEnabled;
    }

    public static boolean getBooleanConfigValue(String name) {
        return getBooleanConfigValue(name, false);
    }

    public static boolean getBooleanConfigValue(String name, boolean defaultValue) {
        String value = System.getProperty(name);
        if (StringUtils.isNoneEmpty(value)) {
            info().data(name, value).log("applying CMS feature flag config value from system.properties");
            return Boolean.parseBoolean(value);
        }

        value = System.getenv(name);
        if (StringUtils.isNoneEmpty(value)) {
            info().data(name, value).log("applying CMS feature flag config value from system.env");
            return Boolean.parseBoolean(value);
        }
        warn().data("name", name).log("CMS config value not found in system.properties or system.env default will be applied");
        return defaultValue;
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
