package com.github.onsdigital.zebedee.util.slack;

/**
 * Defines a CMS start up alerter.
 */
public interface StartUpAlerter {

    /**
     * Send an alert to inform users the CMS as started up and requires and Admin user to login to unlock the
     * publoshing queue.
     */
    void queueLocked();

    /**
     * Update the pervious alert setting the status to resolved.
     */
    void queueUnlocked();
}
