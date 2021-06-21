package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.slack.messages.PostMessage;
import com.github.onsdigital.slack.messages.PostMessageAttachment;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.SlackNotification;


/**
 * Notifier is a generic interface for sending notifications to external systems such as Slack
 */
public interface Notifier {

    /**
     * Create a new collection specific alarm notification.
     *
     * @param c - the collection the notification relates to.
     * @param alarm - the string message to apply to the notification.
     * @param args - additional arguments to add to the notification.
     */
    void collectionAlarm(Collection c, String alarm, PostMessageField... args);
    /**
     * Create a new alarm notification.
     *
     * @param alarm - the string message to apply to the notification.
     * @param args - additional arguments to add to the notification.
     */
    void alarm(String alarm, PostMessageField... args);

    void sendSlackMessage(PostMessage message) throws Exception;

    PostMessage createPostMessage(String channel, String text);

    void callCollectionAlarm(Collection c, String channel, String message, Exception ex);

    void callCollectionWarning(Collection c, String channel, String message, AttachmentField... attachments);

    void callCollectionAlarm(Collection c, String channel, String message, AttachmentField... attachments);

}

