package com.github.onsdigital.zebedee.configuration;

import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;


public class Configuration {

    private static final String DEFAULT_WEBSITE_URL = "http://localhost:8080";
    private static final String DEFAULT_PUBLIC_WEBSITE_URL = "http://localhost:8080";
    private static final String DEFAULT_FLORENCE_URL = "http://localhost:8081";
    private static final String DEFAULT_BRIAN_URL = "http://localhost:8083";
    private static final String DEFAULT_TRAIN_URL = "http://localhost:8084";
    private static final String DEFAULT_DYLAN_URL = "http://localhost:8085";
    private static final String CONTENT_DIRECTORY = "content";
    private static final String INFLUXDB_URL = "http://influxdb:8086";
    private static final String AUDIT_DB_ENABLED_ENV_VAR = "audit_db_enabled";
    private static final String MATHJAX_SERVICE_URL = "http://localhost:8888";
    private static final String DATASET_API_URL = "http://localhost:22000";
    private static final String DATASET_API_AUTH_TOKEN = "FD0108EA-825D-411C-9B1D-41EF7727F465";

    private static final int VERIFY_RETRTY_DELAY = 5000; //milliseconds
    private static final int VERIFY_RETRTY_COUNT = 10;

    // how many seconds before the actual publish time should we run the preprocess
    private static final int DEFAULT_PREPROCESS_SECONDS_BEFORE_PUBLISH = 30;

    // how many additional seconds after the publish
    private static final int DEFAULT_SECONDS_TO_CACHE_AFTER_SCHEDULED_PUBLISH = 30;

    public static boolean isSchedulingEnabled() {
        return BooleanUtils.toBoolean(StringUtils.defaultIfBlank(getValue("scheduled_publishing_enabled"), "true"));
    }

    public static boolean isVerificationEnabled() {
        return BooleanUtils.toBoolean(StringUtils.defaultIfBlank(getValue("publish_verification_enabled"), "false"));
    }

    public static boolean isInfluxReportingEnabled() {
        return BooleanUtils.toBoolean(StringUtils.defaultIfBlank(getValue("enable_influx_reporting"), "false"));
    }

    public static String getInfluxDBHost() {
        return StringUtils.defaultIfBlank(getValue("influxdb_url"), INFLUXDB_URL);
    }

    /**
     * how many seconds before the actual publish time should we run the preprocess.
     */
    public static int getPreProcessSecondsBeforePublish() {
        try {
            return Integer.parseInt(getValue("pre_publish_seconds_before_publish"));
        } catch (Exception e) {
            return DEFAULT_PREPROCESS_SECONDS_BEFORE_PUBLISH;
        }
    }

    /**
     * how many additional seconds after the publish should content be cached.
     */
    public static int getSecondsToCacheAfterScheduledPublish() {
        try {
            return Integer.parseInt(getValue("seconds_to_cache_after_scheduled_publish"));
        } catch (Exception e) {
            return DEFAULT_SECONDS_TO_CACHE_AFTER_SCHEDULED_PUBLISH;
        }
    }

    public static String getPublicWebsiteUrl() {
        return StringUtils.defaultIfBlank(getValue("PUBLIC_WEBSITE_URL"), DEFAULT_PUBLIC_WEBSITE_URL);
    }

    public static String getBabbageUrl() {
        return StringUtils.defaultIfBlank(getValue("BABBAGE_URL"), DEFAULT_WEBSITE_URL);
    }

    public static String getMathjaxServiceUrl() {
        return StringUtils.defaultIfBlank(getValue("MATHJAX_SERVICE_URL"), MATHJAX_SERVICE_URL);
    }

    public static String getDatasetAPIURL() {
        return StringUtils.defaultIfBlank(getValue("DATASET_API_URL"), DATASET_API_URL);
    }

    public static String getDatasetAPIAuthToken() {
        return StringUtils.defaultIfBlank(getValue("DATASET_API_AUTH_TOKEN"), DATASET_API_AUTH_TOKEN);
    }

    public static String[] getTheTrainUrls() {
        return StringUtils.split(StringUtils.defaultIfBlank(getValue("publish_url"), DEFAULT_TRAIN_URL), ",");
    }

    public static String[] getWebsiteUrls() {
        return StringUtils.split(StringUtils.defaultIfBlank(getValue("website_url"), DEFAULT_WEBSITE_URL), ",");
    }

    public static String getBrianUrl() {
        return StringUtils.defaultIfBlank(getValue("brian_url"), DEFAULT_BRIAN_URL);
    }

    public static String getDefaultVerificationUrl() {
        return StringUtils.defaultIfBlank(getValue("verification_url"), DEFAULT_WEBSITE_URL);
    }

    public static String getDylanUrl() {
        return StringUtils.defaultIfBlank(getValue("dylan_url"), DEFAULT_DYLAN_URL);
    }

    public static int getVerifyRetrtyDelay() {
        return VERIFY_RETRTY_DELAY;
    }

    public static int getVerifyRetrtyCount() {
        return VERIFY_RETRTY_COUNT;
    }

    public static String getReindexKey() {
        return StringUtils.defaultIfBlank(getValue("website_reindex_key"), "");
    }

    public static boolean isAuditDatabaseEnabled() {
        return Boolean.valueOf(StringUtils.defaultIfBlank(getValue(AUDIT_DB_ENABLED_ENV_VAR), "true"));
    }

    public static boolean storeDeletedContent() {
        return Boolean.valueOf(StringUtils.defaultIfBlank(getValue("store_deleted_content"), "true"));
    }

    public static String getAuditDBURL() {
        return StringUtils.defaultIfBlank(getValue("db_audit_url"), "");
    }

    /**
     * Gets a configured value for the given key from either the system
     * properties or an environment variable.
     * <p/>
     * Copied from {@link com.github.davidcarboni.restolino.Configuration}.
     *
     * @param key The name of the configuration value.
     * @return The system property corresponding to the given key (e.g.
     * -Dkey=value). If that is blank, the environment variable
     * corresponding to the given key (e.g. EXPORT key=value). If that
     * is blank, {@link org.apache.commons.lang3.StringUtils#EMPTY}.
     */
    static String getValue(String key) {
        return StringUtils.defaultIfBlank(System.getProperty(key), System.getenv(key));
    }

    public static String getContentDirectory() {
        return CONTENT_DIRECTORY;
    }

    public static String getUnauthorizedMessage(Session session) {
        return session == null ? "Please log in" : "You do not have the right permission: " + session;
    }

    public static class UserList   {
        ArrayList<UserObject>   users;
    }

    public static class UserObject {
        public String email;
        public String name;
        public String password;
    }
}
