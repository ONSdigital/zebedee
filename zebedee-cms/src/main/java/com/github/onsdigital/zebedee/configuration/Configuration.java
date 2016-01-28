package com.github.onsdigital.zebedee.configuration;

import com.github.onsdigital.zebedee.json.Session;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;


public class Configuration {

    private static final String DEFAULT_FLORENCE_URL = "http://localhost:8081";
    private static final String DEFAULT_TRAIN_URL = "http://localhost:8084";
    private static final String DEFAULT_BRIAN_URL = "http://localhost:8083";
    private static final String DEFAULT_WEBSITE_URL = "http://localhost:8080";
    private static final String CONTENT_DIRECTORY = "zebedee-cms/target/content";

    private static final int VERIFY_RETRTY_DELAY = 5000; //milliseconds
    private static final int VERIFY_RETRTY_COUNT = 10;
    private static final int DEFAULT_PREPROCESS_SECONDS_BEFORE_PUBLISH = 60;

    public static boolean isSchedulingEnabled() {
        return BooleanUtils.toBoolean(StringUtils.defaultIfBlank(getValue("scheduled_publishing_enabled"), "true"));
    }

    public static boolean isVerificationEnabled() {
        return BooleanUtils.toBoolean(StringUtils.defaultIfBlank(getValue("publish_verification_enabled"), "false"));
    }

    public static boolean isOptimisedPublishingEnabled() {
        return BooleanUtils.toBoolean(StringUtils.defaultIfBlank(getValue("optimised_publish_enabled"), "true"));
    }

    public static int getPreProcessSecondsBeforePublish() {
        try {
            return Integer.parseInt(getValue("pre_publish_seconds_before_publish"));
        } catch (Exception e) {
            return DEFAULT_PREPROCESS_SECONDS_BEFORE_PUBLISH;
        }
    }

    public static String getFlorenceUrl() {
        return StringUtils.defaultIfBlank(getValue("FLORENCE_URL"), DEFAULT_FLORENCE_URL);
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

    public static int getVerifyRetrtyDelay() {
        return VERIFY_RETRTY_DELAY;
    }

    public static int getVerifyRetrtyCount() {
        return VERIFY_RETRTY_COUNT;
    }

    public static String getReindexKey() {
        return StringUtils.defaultIfBlank(getValue("website_reindex_key"), "");
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
