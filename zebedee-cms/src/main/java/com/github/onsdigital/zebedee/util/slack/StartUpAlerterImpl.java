package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.slack.Profile;
import com.github.onsdigital.slack.client.PostMessageResponse;
import com.github.onsdigital.slack.client.SlackClient;
import com.github.onsdigital.slack.messages.Colour;
import com.github.onsdigital.slack.messages.PostMessage;
import com.github.onsdigital.slack.messages.PostMessageAttachment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.text.MessageFormat.format;

/**
 * Called on CMS start up/first admin login after start up. Sends Slack alerts to the configured channels to notify the
 * dev/publishing teams the CMS has started up and requires an admin user to log in/and to resolve this warning.
 */
public class StartUpAlerterImpl implements StartUpAlerter {

    private static final Object MUTEX = new Object();

    private SlackClient slack;
    private boolean queueLocked;
    private boolean lockedNotificationSent;
    private List<PostMessage> messages;
    private List<String> channels;

    /**
     * Create a new instance of the alerter.
     *
     * @param slack    the {@link SlackClient} to use.
     * @param channels the Slack channels to send the alerts to.
     */
    public StartUpAlerterImpl(SlackClient slack, List<String> channels) {
        this(slack, channels, true, false);
    }

    StartUpAlerterImpl(SlackClient slack, List<String> channels, boolean queueLocked,
                       boolean lockedNotificationSent) {
        this.slack = slack;
        this.channels = channels;
        this.queueLocked = queueLocked;
        this.lockedNotificationSent = lockedNotificationSent;
        this.messages = new ArrayList<>();
    }

    @Override
    public void queueLocked() {
        synchronized (MUTEX) {
            if (queueLocked && !lockedNotificationSent) {
                channels.stream().forEach(c -> messages.add(publishingQueueLocked(c)));
                this.lockedNotificationSent = true;
            }
        }
    }

    @Override
    public void queueUnlocked() {
        synchronized (MUTEX) {
            if (queueLocked && lockedNotificationSent) {
                if (channels == null || channels.isEmpty()) {
                    return;
                }

                if (messages == null || messages.isEmpty()) {
                    throw new RuntimeException("failed to send publish queue unlocked notification original message " +
                            "expected but was null");
                }

                messages.stream()
                        .forEach(msg -> publishingQueueUnlocked(msg));
                this.queueLocked = false;
            }
        }
    }

    private PostMessage publishingQueueLocked(String channel) {
        Profile profile = slack.getProfile();

        PostMessage msg = profile
                .newPostMessage(channel, "Publishing system restart complete")
                .addAttachment(new PostMessageAttachment(
                        "Administrator log in required",
                        "Please log out of any existing session and log in again to unlock the publishing queue.",
                        Colour.WARNING
                ));

        PostMessageResponse response = slack.sendMessage(msg);
        msg.ts(response.getTs()).channel(response.getChannel());
        return msg;
    }

    private void publishingQueueUnlocked(PostMessage messageToUpdate) {
        messageToUpdate.getAttachments().get(0).setColor(Colour.GOOD.getColor());

        String msg = format(("Publishing queue successfully unlocked `{0}`"), new Date().toString());
        messageToUpdate.addAttachment(new PostMessageAttachment("Resolved", msg, Colour.GOOD));

        slack.updateMessage(messageToUpdate);
    }

}
