package com.github.onsdigital.zebedee.notification;

/**
 * Exception thrown by the {@link StartUpNotifier} if there is an error whilst attempting to send the CMS start up
 * notifications.
 */
public class NotificationException extends Exception {

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationException(Throwable cause) {
        super(cause);
    }
}
