package com.github.onsdigital.zebedee.configuration;

import com.github.davidcarboni.httpino.Host;
import com.github.onsdigital.zebedee.session.model.Session;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

public class Configuration {

    private static final String DEFAULT_WEBSITE_URL = "http://localhost:8080";
    private static final String DEFAULT_LEGACY_CACHE_API_URL = "http://localhost:29100";
    private static final String DEFAULT_SLACK_WARNING_CHANNEL = "slack-client-test";
    private static final String DEFAULT_SLACK_ALARM_CHANNEL = "slack-client-test";
    private static final String DEFAULT_PUBLIC_WEBSITE_URL = "http://localhost:8080";
    private static final String DEFAULT_FLORENCE_URL = "http://localhost:8081";
    private static final String DEFAULT_BRIAN_URL = "http://localhost:8083";
    private static final String DEFAULT_TRAIN_URL = "http://localhost:8084";
    private static final String DEFAULT_REDIRECT_API_URL = "http://localhost:29900";
    private static final String CONTENT_DIRECTORY = "content";
    private static final String MATHJAX_SERVICE_URL = "http://localhost:8888";
    private static final String DATASET_API_URL = "http://localhost:22000";
    private static final String IMAGE_API_URL = "http://localhost:24700";
    private static final String STATIC_FILES_API_URL = "http://localhost:26900";
    private static final String IDENTITY_API_URL = "http://localhost:25600";
    private static final String KAFKA_ADDR = "localhost:9092";
    private static final String KAFKA_CONTENT_UPDATED_TOPIC = "content-updated";
    private static final String DATASET_API_AUTH_TOKEN = "FD0108EA-825D-411C-9B1D-41EF7727F465";
    private static final String SERVICE_AUTH_TOKEN = "15C0E4EE-777F-4C61-8CDB-2898CEB34657";
    private static final String LEGACY_CACHE_API_AUTH_TOKEN = "748896205c3b42b43adb4b22fff11784c5d971187f280ab1b6f142c3d69e64e4";
    private static final String DEFAULT_SLACK_USERNAME = "Zebedee";
    private static final String KEYRING_SECRET_KEY = "KEYRING_SECRET_KEY";
    private static final String KEYRING_INIT_VECTOR = "KEYRING_INIT_VECTOR";
    private static final String DATASET_WHITELIST = "drsi,mm23,mm22,ppi,dataset1,pusf,a01,x09,cla01,pn2,mgdp,diop,ios1,mret,mq10,rtisa";
    private static final String NON_TS_DATASET_WHITELIST = "dataset1,a01,x09,cla01,rtisa";
    private static final String EXPECTED_DATASET1_PATH = "economy/inflationandpriceindices/datasets/growthratesofoutputandinputproducerpriceinflation";

    private static final int VERIFY_RETRY_DELAY = 5000; // milliseconds
    private static final int VERIFY_RETRY_COUNT = 10;

    // Default retry configs to handle fetching jwt keys from identity api failure
    private static final int DEFAULT_INITIAL_RETRY_INTERVAL = 500;
    private static final int DEFAULT_MAX_RETRY_ELAPSED_TIME = 900000;
    private static final int DEFAULT_MAX_RETRY_INTERVAL = 60000;

    // how many seconds before the actual publish time should we run the preprocess
    private static final int DEFAULT_PREPROCESS_SECONDS_BEFORE_PUBLISH = 30;

    // how many additional seconds after the publish
    private static final int DEFAULT_SECONDS_TO_CACHE_AFTER_SCHEDULED_PUBLISH = 30;

    private static final String RESUMABLE_TYPE = "text/plain";
    private static final String IS_PUBLISHABLE = "true";
    private static final String LICENCE = "Open Government Licence v3.0";
    private static final String LICENCE_URL = "https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/";
    private static final String UPLOAD_SERVICE_API_URL = "http://localhost:25100";

    public static boolean isLegacyCacheAPIEnabled() {
        return BooleanUtils.toBoolean(StringUtils.defaultIfBlank(getValue("ENABLE_LEGACY_CACHE_API"), "false"));
    }

    public static boolean isSchedulingEnabled() {
        return BooleanUtils.toBoolean(StringUtils.defaultIfBlank(getValue("scheduled_publishing_enabled"), "true"));
    }

    public static String getDefaultSlackWarningChannel() {
        return StringUtils.defaultIfBlank(getValue("slack_default_channel"), DEFAULT_SLACK_WARNING_CHANNEL);
    }

    public static String getDefaultSlackAlarmChannel() {
        return StringUtils.defaultIfBlank(getValue("slack_default_channel"), DEFAULT_SLACK_ALARM_CHANNEL);
    }

    /**
     * how many seconds before the actual publish time should we run the preprocess.
     */
    public static int getPreProcessSecondsBeforePublish() {
        return getIntWithDefault("pre_publish_seconds_before_publish", DEFAULT_PREPROCESS_SECONDS_BEFORE_PUBLISH);
    }

    /**
     * how many additional seconds after the publish should content be cached.
     */
    public static int getSecondsToCacheAfterScheduledPublish() {
        return getIntWithDefault("seconds_to_cache_after_scheduled_publish",
                DEFAULT_SECONDS_TO_CACHE_AFTER_SCHEDULED_PUBLISH);
    }

    public static int getMaxRetryTimeout() {
        return getIntWithDefault("MAX_RETRY_ELAPSED_TIME", DEFAULT_MAX_RETRY_ELAPSED_TIME);
    }

    public static int getInitialRetryInterval() {
        return getIntWithDefault("INITIAL_RETRY_INTERVAL", DEFAULT_INITIAL_RETRY_INTERVAL);
    }

    public static int getMaxRetryInterval() {
        return getIntWithDefault("MAX_RETRY_INTERVAL", DEFAULT_MAX_RETRY_INTERVAL);
    }

    public static String getPublicWebsiteUrl() {
        return StringUtils.defaultIfBlank(getValue("PUBLIC_WEBSITE_URL"), DEFAULT_PUBLIC_WEBSITE_URL);
    }

    public static String getBabbageUrl() {
        return StringUtils.defaultIfBlank(getValue("BABBAGE_URL"), DEFAULT_WEBSITE_URL);
    }

    public static String getFlorenceUrl() {
        return StringUtils.defaultIfBlank(getValue("FLORENCE_URL"), DEFAULT_FLORENCE_URL);
    }

    public static String getLegacyCacheApiUrl() {
        return StringUtils.defaultIfBlank(getValue("LEGACY_CACHE_API_URL"), DEFAULT_LEGACY_CACHE_API_URL);
    }

    public static String getRedirectApiUrl() {
        return StringUtils.defaultIfBlank(getValue("REDIRECT_API_URL"), DEFAULT_REDIRECT_API_URL);
    }

    public static String getSlackUsername() {
        return StringUtils.defaultIfBlank(getValue("SLACK_USERNAME"), DEFAULT_SLACK_USERNAME);
    }

    public static List<String> slackChannelsToNotfiyOnStartUp() {
        String val = getValue("START_UP_NOTIFY_LIST");
        if (StringUtils.isEmpty(val)) {
            return new ArrayList<>();
        }

        return Arrays.asList(val.split(","));
    }

    public static String getSlackSupportChannelID() {
        String channelID = getValue("SLACK_SUPPORT_CHANNEL_ID");
        if (StringUtils.isEmpty(channelID)) {
            return "#publishing-support";
        }

        return format("<#{0}>", channelID);
    }

    public static String getMathjaxServiceUrl() {
        return StringUtils.defaultIfBlank(getValue("MATHJAX_SERVICE_URL"), MATHJAX_SERVICE_URL);
    }

    public static String getDatasetAPIURL() {
        return StringUtils.defaultIfBlank(getValue("DATASET_API_URL"), DATASET_API_URL);
    }

    public static String getImageAPIURL() {
        return StringUtils.defaultIfBlank(getValue("IMAGE_API_URL"), IMAGE_API_URL);
    }

    public static String getStaticFilesAPIURL() {
        return StringUtils.defaultIfBlank(getValue("FILES_API_URL"), STATIC_FILES_API_URL);
    }

    public static String getIdentityAPIURL() {
        return StringUtils.defaultIfBlank(getValue("IDENTITY_API_URL"), IDENTITY_API_URL);
    }

    public static String getKafkaURL() {
        return StringUtils.defaultIfBlank(getValue("KAFKA_ADDR"), KAFKA_ADDR);
    }

    public static String getKafkaContentUpdatedTopic() {
        return StringUtils.defaultIfBlank(getValue("KAFKA_CONTENT_UPDATED_TOPIC"), KAFKA_CONTENT_UPDATED_TOPIC);
    }

    public static String getKafkaSecProtocol() {
        return StringUtils.defaultIfBlank(getValue("KAFKA_SEC_PROTO"), "");
    }

    // base64-encoded key in PEM format
    public static String getKafkaSecClientKey() {
        return StringUtils.defaultIfBlank(getValue("KAFKA_SEC_CLIENT_KEY"), "");
    }

    // base64-encoded cert in PEM format
    public static String getKafkaSecClientCert() {
        return StringUtils.defaultIfBlank(getValue("KAFKA_SEC_CLIENT_CERT"), "");
    }

    public static String getDatasetAPIAuthToken() {
        return StringUtils.defaultIfBlank(getValue("DATASET_API_AUTH_TOKEN"), DATASET_API_AUTH_TOKEN);
    }

    public static String getServiceAuthToken() {
        String serviceAuthToken = StringUtils.defaultIfBlank(getValue("SERVICE_AUTH_TOKEN"), SERVICE_AUTH_TOKEN);
        return "Bearer " + serviceAuthToken;
    }

    public static String getLegacyCacheAPIAuthToken() {
        String authToken = StringUtils.defaultIfBlank(getValue("LEGACY_CACHE_API_AUTH_TOKEN"), LEGACY_CACHE_API_AUTH_TOKEN);
        return "Bearer " + authToken;
    }

    public static String[] getTheTrainUrls() {
        return StringUtils.split(StringUtils.defaultIfBlank(getValue("publish_url"), DEFAULT_TRAIN_URL), ",");
    }

    public static List<Host> getTheTrainHosts() {
        return Arrays.asList(StringUtils.split(defaultIfBlank(getValue("publish_url"), DEFAULT_TRAIN_URL), ","))
                .stream()
                .map(url -> new Host(url))
                .collect(Collectors.toList());
    }

    public static List<Host> getWebsiteHosts() {
        return Arrays.asList(StringUtils.split(defaultIfBlank(getValue("website_url"), DEFAULT_WEBSITE_URL), ","))
                .stream()
                .map(url -> new Host(url))
                .collect(Collectors.toList());
    }

    public static String getBrianUrl() {
        return StringUtils.defaultIfBlank(getValue("brian_url"), DEFAULT_BRIAN_URL);
    }

    public static String getDefaultVerificationUrl() {
        return StringUtils.defaultIfBlank(getValue("verification_url"), DEFAULT_WEBSITE_URL);
    }

    public static int getVerifyRetryDelay() {
        return VERIFY_RETRY_DELAY;
    }

    public static int getVerifyRetryCount() {
        return VERIFY_RETRY_COUNT;
    }

    public static String getReindexKey() {
        return StringUtils.defaultIfBlank(getValue("website_reindex_key"), "");
    }

    //Enable upload-new endpoint
    public static boolean isUploadNewEndpointEnabled() {
        return BooleanUtils.toBoolean(StringUtils.defaultIfBlank(getValue("ENABLE_UPLOAD_NEW_ENDPOINT"), "false"));
    }

    // Get whitelist from dp-configs
    public static String getDatasetWhitelist() {
        return StringUtils.defaultIfBlank(getValue("DATASET_WHITELIST"), DATASET_WHITELIST);
    }

    // Get non timeseries whitelist from dp-configs
    public static String getNonTsDatasetWhitelist() {
        return StringUtils.defaultIfBlank(getValue("NON_TS_DATASET_WHITELIST"), NON_TS_DATASET_WHITELIST);
    }
    
    // Get dataset1 path from dp-configs
    public static String getDataset1ExpectedPath() {
        return StringUtils.defaultIfBlank(getValue("EXPECTED_DATASET1_PATH"), EXPECTED_DATASET1_PATH);
    }

    // Changeable upload-new endpoint parameters
    public static String getResumableType() {
        return StringUtils.defaultIfBlank(getValue("RESUMABLE_TYPE"), RESUMABLE_TYPE);
    }

    public static String getIsPublishable() {
        return StringUtils.defaultIfBlank(getValue("IS_PUBLISHABLE"), IS_PUBLISHABLE);
    }
 
    public static String getLicence() {
        return StringUtils.defaultIfBlank(getValue("LICENCE"), LICENCE);
    }

    public static String getLicenceURL() {
        return StringUtils.defaultIfBlank(getValue("LICENCE_URL"), LICENCE_URL);
    }

    public static String getUploadServiceApiUrl() {
        return StringUtils.defaultIfBlank(getValue("UPLOAD_SERVICE_API_URL"), UPLOAD_SERVICE_API_URL);
    }

    /**
     * Get collection keyring encryption key
     */
    public static SecretKey getKeyringSecretKey() {
        String keyStr = getValue(KEYRING_SECRET_KEY);
        if (StringUtils.isEmpty(keyStr)) {
            throw new RuntimeException("expected keyring secret key in environment variable but was empty");
        }

        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");

        Arrays.fill(keyBytes, (byte) 0);

        return secretKey;
    }

    /**
     * Get collection keyring vector key
     */
    public static IvParameterSpec getKeyringInitVector() {
        String vectorStr = getValue(KEYRING_INIT_VECTOR);
        if (StringUtils.isEmpty(vectorStr)) {
            throw new RuntimeException("expected keyring init vector in environment variable but was empty");
        }

        byte[] vectorBytes = Base64.getDecoder().decode(vectorStr);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(vectorBytes);

        Arrays.fill(vectorBytes, (byte) 0);

        return ivParameterSpec;
    }

    /**
     * Gets a configured value for the given key from either the system
     * properties or an environment variable.
     * <p/>
     * Copied from {@link com.github.davidcarboni.restolino.Configuration}.
     *
     * @param key The name of the configuration value.
     * @return The system property corresponding to the given key (e.g.
     *         -Dkey=value). If that is blank, the environment variable
     *         corresponding to the given key (e.g. EXPORT key=value). If that
     *         is blank, {@link org.apache.commons.lang3.StringUtils#EMPTY}.
     */
    static String getValue(String key) {
        return StringUtils.defaultIfBlank(System.getProperty(key), System.getenv(key));
    }

    /**
     * Gets a configured int value for the given key from either the system
     * properties or an environment variable.
     * <p/>
     *
     * @param key          The name of the configuration value.
     * @param defaultValue The default value to be used if configuration is missing.
     * @return The system property corresponding to the given key (e.g.
     *         -Dkey=value). If that is blank, the environment variable
     *         corresponding to the given key (e.g. EXPORT key=value). If that
     *         is blank, defaultValue passed to the method.
     */
    static int getIntWithDefault(String key, int defaultValue) {
        try {
            return Integer.parseInt(getValue(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String getContentDirectory() {
        return CONTENT_DIRECTORY;
    }

    public static String getUnauthorizedMessage(Session session) {
        return session == null ? "Please log in" : "You do not have the right permission: " + session;
    }

    public static class UserList {
        ArrayList<UserObject> users;
    }

    public static class UserObject {
        public String email;
        public String name;
        public String password;
    }
}
