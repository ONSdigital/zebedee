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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static com.github.onsdigital.zebedee.logging.CMSLogEvent.info;
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

    private List<Callable<PostMessage>> queueLockedAlerts;
    private List<Callable<Void>> queueUnlockedAlerts;

    private ExecutorService executorService = Executors.newFixedThreadPool(5);

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
        this.queueLocked = queueLocked;
        this.lockedNotificationSent = lockedNotificationSent;

        this.queueLockedAlerts = channels.stream()
                .map(c -> newQueueLockedAlertTask(c))
                .collect(Collectors.toList());

        this.queueUnlockedAlerts = new ArrayList<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            info().log("shutting down slack thread executor service");
            executorService.shutdown();
        }));
    }

    @Override
    public void queueLocked() {
        synchronized (MUTEX) {
            if (queueLocked && !lockedNotificationSent) {

                List<Future<PostMessage>> futures = null;
                try {
                    futures = executorService.invokeAll(queueLockedAlerts);
                } catch (Exception ex) {
                    error().exception(ex).log("error sending publishing queue locked slack notification");
                    return;
                }

                this.lockedNotificationSent = true;

                for (Future<PostMessage> result : futures) {
                    try {
                        PostMessage msg = result.get();
                        if (msg != null) {
                            this.queueUnlockedAlerts.add(newQueueUnlockedAlertTask(msg));
                        }
                    } catch (Exception ex) {
                        error().exception(ex).log("error sending publishing queue locked slack notification");
                    }
                }
            }
        }
    }

    @Override
    public void queueUnlocked() {
        synchronized (MUTEX) {
            if (queueLocked && lockedNotificationSent) {
                this.queueLocked = false;

                if (queueUnlockedAlerts == null || queueUnlockedAlerts.isEmpty()) {
                    return;
                }

                List<Future<Void>> futures;
                try {
                    futures = executorService.invokeAll(queueUnlockedAlerts);
                } catch (Exception ex) {
                    error().exception(ex).log("error sending publishing queue locked slack notification");
                    return;
                }

                for (Future<Void> result : futures) {
                    try {
                        result.get();
                    } catch (Exception ex) {
                        error().exception(ex).log("error sending publishing queue unlocked slack notification");
                    }
                }
            }
        }
    }

    Callable<PostMessage> newQueueLockedAlertTask(String channel) {
        return () -> {
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
        };
    }


    Callable<Void> newQueueUnlockedAlertTask(PostMessage messageToUpdate) {
        return () -> {
            if (messageToUpdate.getAttachments() != null && !messageToUpdate.getAttachments().isEmpty()) {
                messageToUpdate.getAttachments().get(0).setColor(Colour.GOOD.getColor());
            }

            String msg = format(("Publishing queue successfully unlocked `{0}`"), new Date().toString());
            messageToUpdate.addAttachment(new PostMessageAttachment("Resolved", msg, Colour.GOOD));

            slack.updateMessage(messageToUpdate);
            return null;
        };
    }

}
