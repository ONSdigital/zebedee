package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.slack.messages.PostMessage;
import com.github.onsdigital.zebedee.model.Collection;


/**
 * Notifier is a generic interface for sending notifications to external systems such as Slack
 */
public interface Notifier {

    /**
     * Create a new collection specific alarm notification.
     *
     * @param c     - the collection the notification relates to.
     * @param alarm - the string message to apply to the notification.
     * @param args  - additional arguments to add to the notification.
     */
    void collectionAlarm(Collection c, String alarm, PostMessageField... args);

    /**
     * Create a new alarm notification.
     *
     * @param alarm - the string message to apply to the notification.
     * @param args  - additional arguments to add to the notification.
     */
    void alarm(String alarm, PostMessageField... args);

    /**
     * Sends a slack message.
     *
     * @param message - the information to apply to the slack message notification.
     * @throws Exception
     */
    void sendSlackMessage(PostMessage message) throws Exception;

    /**
     * Creates a PostMessage and returns a PostMessage object
     *
     * @param channel - the channel to send the notification.
     * @param text    - the information to add to the notification.
     */
    PostMessage createPostMessage(String channel, String text);

    /**
     * Sends a collection alarm to a specific channel with custom message and exception about the collection
     *
     * @param c       - the collection the notification relates to.
     * @param channel - the channel to send the notification.
     * @param message - the custom information to add to the notification alarm.
     * @param ex      - Any exception to add to the notification.
     * @return
     */
    boolean sendCollectionAlarm(Collection c, String channel, String message, Exception ex);

    /**
     * Sends a collection warning to a specific channel with custom message and additional information
     * about the collection
     *
     * @param c           - the collection the notification relates to.
     * @param channel     - the channel to send the notification.
     * @param message     - the custom information to add to the notification alarm.
     * @param attachments - Additional information about the collection to add to the notification.
     * @return
     */
    boolean sendCollectionWarning(Collection c, String channel, String message, AttachmentField... attachments);

    /**
     * Sends a collection alarm to a specific channel with custom message and exception about the collection
     *
     * @param c           - the collection the notification relates to.
     * @param channel     - the channel to send the notification.
     * @param message     - the custom information to add to the notification alarm.
     * @param attachments - Additional information about the collection to add to the notification.
     * @return
     */
    boolean sendCollectionAlarm(Collection c, String channel, String message, AttachmentField... attachments);

}

