package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.zebedee.notification.NotificationException;
import com.github.onsdigital.zebedee.notification.StartUpNotifier;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;

/**
 * Nop implementation of {@link StartUpNotifier}.
 */
public class NopStartUpNotifier implements StartUpNotifier {

    @Override
    public boolean notifyStartUpComplete() throws NotificationException {
        info().log("NopStartUpNotifier: sending notifyStartUpComplete message");
        return true;
    }
}
