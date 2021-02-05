package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.SlackNotification;


/**
 * SlackNotifier provides a slack specific Notifier implementation.
 * It wraps the original SlackNotification static class, to enable dependency injection in dependent code.
 */
public class SlackNotifier implements Notifier {

    /**
     * @param c - the collection the notification relates to.
     * @param alarm - the string message to apply to the notification.
     * @param args - additional arguments to add to the notification.
     */
    @Override
    public void collectionAlarm(Collection c, String alarm, PostMessageField... args) {
        SlackNotification.collectionAlarm(c, alarm, args);
    }
}
