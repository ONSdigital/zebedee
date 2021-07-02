package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.slack.Profile;
import com.github.onsdigital.slack.client.SlackClient;
import com.github.onsdigital.slack.messages.Colour;
import com.github.onsdigital.slack.messages.PostMessage;
import com.github.onsdigital.slack.messages.PostMessageAttachment;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.SlackNotification;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static org.apache.commons.lang3.StringUtils.isEmpty;


/**
 * SlackNotifier provides a slack specific Notifier implementation.
 * It wraps the original SlackNotification static class, to enable dependency injection in dependent code.
 */
public class SlackNotifier implements Notifier {

    private SlackClient slackClient;
    private Profile profile;

    public SlackNotifier(SlackClient slackClient) {
        this.slackClient = slackClient;
        this.profile = slackClient.getProfile();
    }

    /**
     * Send a collection specific alarm to Slack.
     *
     * @param c     - the collection the notification relates to.
     * @param alarm - the string message to apply to the notification.
     * @param args  - additional arguments to add to the notification.
     */
    @Override
    public void collectionAlarm(Collection c, String alarm, PostMessageField... args) {
        SlackNotification.collectionAlarm(c, alarm, args);
    }

    /**
     * Send a collection specific alarm to Slack.
     *
     * @param alarm - the string message to apply to the notification.
     * @param args  - additional arguments to add to the notification.
     */
    @Override
    public void alarm(String alarm, PostMessageField... args) {
        SlackNotification.alarm(alarm, args);
    }


    @Override
    public void sendSlackMessage(PostMessage message) throws Exception {
        try {
            slackClient.sendMessage(message);
        } catch (Exception ex) {
            throw new Exception("unexpected error sending slack message", ex);
        }
    }

    @Override
    public PostMessage createPostMessage(String channel, String text) {
        return profile.newPostMessage(channel, text);
    }

    @Override
    public boolean sendCollectionAlarm(Collection collection, String channel, String customMessage, Exception ex) {
        boolean result = validate(collection, channel, customMessage, "alarm");
        if (!result) {
            return false;
        }
        if (isEmpty(ex.getMessage())) {
            return false;
        }
        AttachmentField exField = new AttachmentField("exception", ex.getMessage(), false);
        PostMessage postMessage = createPostMessage(channel, customMessage)
                .addAttachment(createCollectionAttachment("Alert", "Collection Alarm", Colour.DANGER, collection, exField));

        boolean messageOutput = postMessageToSlack(postMessage);
        if (!messageOutput) {
            return false;
        }
        return true;
    }

    @Override
    public boolean sendCollectionWarning(Collection collection, String channel, String customMessage, AttachmentField... fields) {
        boolean result = validate(collection, channel, customMessage, "warning");
        if (!result) {
            return false;
        }
        PostMessage postMessage = createPostMessage(channel, customMessage)
                .addAttachment(createCollectionAttachment("Warning", "Collection Warning", Colour.WARNING, collection, fields));
        boolean messageOutput = postMessageToSlack(postMessage);
        if (!messageOutput) {
            return false;
        }
        return true;
    }

    @Override
    public boolean sendCollectionAlarm(Collection collection, String channel, String customMessage, AttachmentField... fields) {
        boolean result = validate(collection, channel, customMessage, "alarm");
        if (!result) {
            return false;
        }
        PostMessage postMessage = createPostMessage(channel, customMessage)
                .addAttachment(createCollectionAttachment("Alert", "Collection Alarm", Colour.DANGER, collection, fields));
        boolean messageOutput = postMessageToSlack(postMessage);
        if (!messageOutput) {
            return false;
        }
        return true;
    }

    private PostMessageAttachment createCollectionAttachment(String title, String message, Colour colour, Collection collection, AttachmentField... fields) {
        PostMessageAttachment attachment = new PostMessageAttachment(title, message, colour)
                .addField("Publishing Type", collection.getDescription().getType().name(), true)
                .addField("CollectionID", collection.getId(), false)
                .addField("Collection Name", collection.getDescription().getName(), false);

        if (fields != null) {
            for (AttachmentField field : fields) {
                attachment.addField(field.getTitle(), field.getMessage(), field.isShort());
            }
        }

        return attachment;
    }

    private boolean validate(Collection collection, String channel, String customMessage, String type) {
        String message = "error sending collection " + type + ".channel/customMessage/collection was empty";
        String logMessage = "error sending collection " + type + ". Collection description/id/type/name is null";
        if (isEmpty(channel) || isEmpty(customMessage) || collection == null) {
            error().log(message);
            return false;
        }
        if (collection.getDescription() == null || collection.getDescription().getType() == null
                || isEmpty(collection.getId()) || isEmpty(collection.getDescription().getName())) {
            error().log(logMessage);
            return false;
        }
        return true;
    }

    private boolean postMessageToSlack(PostMessage postMessage) {
        try {
            sendSlackMessage(postMessage);
        } catch (Exception e) {
            error().exception(e).log("unexpected error while sending slack notification");
            return false;
        }
        return true;
    }

}
