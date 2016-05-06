package com.github.onsdigital.zebedee.logging.events;

/**
 * Created by dave on 5/4/16.
 */
public enum ZebedeeLogEvent {
    /**
     * Zebedee start up.
     */
    ZEBEDEE_STARTUP("Zebedee started successfully."),

    /**
     * User successfully logged in to Florence.
     **/
    LOGIN_SUCCESS("Florence login success."),

    /**
     * User failed authentication when attempting to login to Florence.
     */
    LOGIN_AUTH_FAILURE("Login authentication failure."),

    /**
     * User password requires changing.
     */
    PASSWORD_CHANGE_REQUIRED("Florence password change required");

    private final String description;

    ZebedeeLogEvent(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
