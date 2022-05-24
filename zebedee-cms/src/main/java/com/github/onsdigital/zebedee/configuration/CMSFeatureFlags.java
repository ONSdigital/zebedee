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
    private static final String ENABLE_DATASET_VERSION_VERIFICATION = "ENABLE_DATASET_VERSION_VERIFICATION";
    private static final String ENABLE_SESSIONS_API = "ENABLE_SESSIONS_API";
    private static final String ENABLE_CENTRALISED_KEYRING = "ENABLE_CENTRALISED_KEYRING";
    public static final String ENABLE_IMAGE_PUBLISHING = "ENABLE_IMAGE_PUBLISHING";
    private static final String ENABLE_JWT_SESSIONS = "ENABLE_JWT_SESSIONS";
    public static final String ENABLE_KAFKA = "ENABLE_KAFKA";
    public static final String ENABLE_STATIC_FILES_PUBLISHING = "ENABLE_STATIC_FILES_PUBLISHING";
    public static final String ENABLE_INTERACTIVES_PUBLISHING = "ENABLE_INTERACTIVES_PUBLISHING";

    /**
     * Singleton instance
     **/
    private static CMSFeatureFlags instance = null;
    private final boolean isDatasetImportEnabled;
    private final boolean isVerifyPublishEnabled;
    private final boolean isDatasetVersionVerificationEnabled;
    private final boolean isSessionAPIEnabled;
    private final boolean isCentralisedKeyringEnabled;
    private final boolean isImagePublishingEnabled;
    private final boolean isJwtSessionsEnabled;
    private final boolean isKafkaEnabled;
    private final boolean isStaticFilesPublishingEnabled;
    private final boolean isInteractivesPublishingEnabled;


    /**
     * Construct a new feature flags instance.
     */
    private CMSFeatureFlags() {
        this.isDatasetImportEnabled = Boolean.valueOf(getConfigValue(ENABLE_DATASET_IMPORT));
        this.isVerifyPublishEnabled = Boolean.valueOf(getConfigValue(ENABLE_VERIFY_PUBLISH_CONTENT));
        this.isDatasetVersionVerificationEnabled = Boolean.valueOf(getConfigValue(ENABLE_DATASET_VERSION_VERIFICATION));
        this.isSessionAPIEnabled = Boolean.valueOf(getConfigValue(ENABLE_SESSIONS_API));
        this.isCentralisedKeyringEnabled = Boolean.valueOf(getConfigValue(ENABLE_CENTRALISED_KEYRING));
        this.isImagePublishingEnabled = Boolean.valueOf(getConfigValue(ENABLE_IMAGE_PUBLISHING));
        this.isJwtSessionsEnabled = Boolean.valueOf(getConfigValue(ENABLE_JWT_SESSIONS));
        this.isKafkaEnabled = Boolean.valueOf(getConfigValue(ENABLE_KAFKA));
        this.isStaticFilesPublishingEnabled = Boolean.valueOf(getConfigValue(ENABLE_STATIC_FILES_PUBLISHING));
        this.isInteractivesPublishingEnabled = Boolean.valueOf(getConfigValue(ENABLE_INTERACTIVES_PUBLISHING));

        info().data(ENABLE_DATASET_IMPORT, isDatasetImportEnabled)
                .data(ENABLE_VERIFY_PUBLISH_CONTENT, isVerifyPublishEnabled)
                .data(ENABLE_DATASET_VERSION_VERIFICATION, isDatasetVersionVerificationEnabled)
                .data(ENABLE_SESSIONS_API, isSessionAPIEnabled)
                .data(ENABLE_CENTRALISED_KEYRING, isCentralisedKeyringEnabled)
                .data(ENABLE_IMAGE_PUBLISHING, isImagePublishingEnabled)
                .data(ENABLE_JWT_SESSIONS, isJwtSessionsEnabled)
                .data(ENABLE_KAFKA, isKafkaEnabled)
                .data(ENABLE_STATIC_FILES_PUBLISHING, isStaticFilesPublishingEnabled)
                .data(ENABLE_INTERACTIVES_PUBLISHING, isInteractivesPublishingEnabled)
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

    public boolean isSessionAPIEnabled() {
        return this.isSessionAPIEnabled;
    }

    public boolean isCentralisedKeyringEnabled() {
        return isCentralisedKeyringEnabled;
    }

    public boolean isImagePublishingEnabled() {
        return isImagePublishingEnabled;
    }

    public boolean isJwtSessionsEnabled() {
        return isJwtSessionsEnabled;
    }

    public boolean isKafkaEnabled() {
        return isKafkaEnabled;
    }

    public boolean isStaticFilesPublishingEnabled() {
        return isStaticFilesPublishingEnabled;
    }

    public boolean isInteractivesPublishingEnabled() {
        return isInteractivesPublishingEnabled;
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
