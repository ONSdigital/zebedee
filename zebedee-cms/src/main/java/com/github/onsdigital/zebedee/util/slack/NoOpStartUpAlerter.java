package com.github.onsdigital.zebedee.util.slack;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

/**
 * No Op impl of StartUpAlerter.
 */
public class NoOpStartUpAlerter implements StartUpAlerter {

    @Override
    public void queueLocked() {
        info().log("NoOpStartUpAlerter queueLocked invoked - do nothing");
    }

    @Override
    public void queueUnlocked() {
        info().log("NoOpStartUpAlerter queueUnlocked invoked - - do nothing");
    }
}
