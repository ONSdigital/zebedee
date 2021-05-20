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

    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private static final Object MUTEX = new Object();

    private SlackClient slack;
    private boolean queueLocked;
    private List<Callable<PostMessage>> queueLockedAlerts;
    private List<Callable<Void>> queueUnlockedAlerts;


    /**
     * Create a new instance of the alerter.
     *
     * @param slack    the {@link SlackClient} to use.
     * @param channels the Slack channels to send the alerts to.
     */
    public StartUpAlerterImpl(SlackClient slack, List<String> channels) {
        this(slack, channels, true);
    }

    StartUpAlerterImpl(SlackClient slack, List<String> channels, boolean queueLocked) {
        this.slack = slack;
        this.queueLocked = queueLocked;

        this.queueLockedAlerts = channels.stream()
                .map(c -> newQueueLockedAlertTask(c))
                .collect(Collectors.toList());

        this.queueUnlockedAlerts = new ArrayList<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            info().log("shutting down Slack notification thread executor service");
            executorService.shutdown();
        }));
    }

    @Override
    public void queueLocked() {
        synchronized (MUTEX) {
            if (queueLocked && !queueLockedAlerts.isEmpty() && queueUnlockedAlerts.isEmpty()) {
                List<Future<PostMessage>> futures = null;
                try {
                    futures = executorService.invokeAll(queueLockedAlerts);
                } catch (Exception ex) {
                    error().exception(ex).log("error invoking callable tasks for start up publishing queue locked " +
                            "notification");
                    return;
                }

                for (Future<PostMessage> result : futures) {
                    try {
                        PostMessage msg = result.get();
                        if (msg != null) {
                            this.queueUnlockedAlerts.add(newQueueUnlockedAlertTask(msg));
                        }
                    } catch (Exception ex) {
                        error().exception(ex).log("error sending start up publishing queue locked notification");
                    }
                }
            }
        }
    }

    @Override
    public void queueUnlocked() {
        synchronized (MUTEX) {
            if (queueLocked && queueUnlockedAlerts != null && !queueUnlockedAlerts.isEmpty()) {
                this.queueLocked = false;

                List<Future<Void>> futures;
                try {
                    futures = executorService.invokeAll(queueUnlockedAlerts);
                } catch (Exception ex) {
                    error().exception(ex).log("error invoking callable tasks for start up publishing queue unlocked " +
                            "notification");
                    return;
                }

                for (Future<Void> result : futures) {
                    try {
                        result.get();
                    } catch (Exception ex) {
                        error().exception(ex).log("error sending publishing queue unlocked slack notification");
                    }
                }
                queueUnlockedAlerts.clear();
            }
        }
    }

    /**
     * Create a {@link Callable} task to send a Slack message advising the CMS has restarted and requires an admin
     * user to login in.
     * @param channel the channel to send the message to.
     */
    Callable<PostMessage> newQueueLockedAlertTask(String channel) {
        return () -> {
            Profile profile = slack.getProfile();

            PostMessage msg = profile
                    .newPostMessage(channel, "Publishing system restart complete")
                    .addAttachment(new PostMessageAttachment(
                            "Administrator log in required",
                            "Please log out of any existing session and log in again to unlock the publishing queue.",
                            Colour.WARNING));

            PostMessageResponse response = slack.sendMessage(msg);
            msg.ts(response.getTs()).channel(response.getChannel());

            return msg;
        };
    }

    /**
     * Create a {@link Callable} task for sending a Slack message update informing that the publishing queue
     * has been unlocked
     *
     * @param messageToUpdate the original message to update.
     */
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
