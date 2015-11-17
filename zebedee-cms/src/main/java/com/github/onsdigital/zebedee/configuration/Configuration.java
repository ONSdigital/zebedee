package com.github.onsdigital.zebedee.configuration;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.ConflictException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.json.User;
import com.github.onsdigital.zebedee.model.Users;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;


public class Configuration {

    private static final String DEFAULT_FLORENCE_URL = "http://localhost:8081";
    private static final String DEFAULT_TRAIN_URL = "http://localhost:8083";
    private static final String DEFAULT_WEBSITE_URL = "http://localhost:8080";
    private static final String CONTENT_DIRECTORY = "zebedee-cms/target/content";

    private static final String DEFAULT_VERIFICATION_URL = "http://localhost:8080";
    private static final int VERIFY_RETRTY_DELAY = 5000; //milliseconds
    private static final int VERIFY_RETRTY_COUNT = 10;

    public static boolean isSchedulingEnabled() {
        return BooleanUtils.toBoolean(StringUtils.defaultIfBlank(getValue("scheduled_publishing_enabled"), "true"));
    }

    public static String getFlorenceUrl() {
        return StringUtils.defaultIfBlank(getValue("FLORENCE_URL"), DEFAULT_FLORENCE_URL);
    }

    public static String getTheTrainUrl() {
        return StringUtils.defaultIfBlank(getValue("publish_url"), DEFAULT_TRAIN_URL);
    }

    public static String getWebsiteUrl() {
        return StringUtils.defaultIfBlank(getValue("website_url"), DEFAULT_WEBSITE_URL);
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

    public static void buildUserAccounts(java.io.File file, Zebedee zebedee, Session superSession) throws IOException, UnauthorizedException, ConflictException, BadRequestException {

        try(InputStream stream = Files.newInputStream(file.toPath())) {
            UserList list = Serialiser.deserialise(stream, UserList.class);
            for (UserObject object: list.users) {
                User user = new User();
                user.email = object.email;
                user.name = object.name;
                Users.createPublisher(zebedee, user, object.password, superSession);
            }
        }
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
