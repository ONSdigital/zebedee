package com.github.onsdigital.zebedee.notification;

/**
 * Defines the behaviour of a CMS start up notifiier.
 */
public interface StartUpNotifier {

    /**
     * Send a notification informing recipients that the CMS has completed start up successfully.
     *
     * @return true if the notifications were successful, false if the notifications were not sent or
     * unsuccessful.
     * @throws NotificationException thrown if there is an error attempting to send notifications.
     */
    boolean notifyStartUpComplete() throws NotificationException;
}
