package com.github.onsdigital.zebedee.util.slack;

import com.github.onsdigital.slack.Profile;
import com.github.onsdigital.slack.client.PostMessageResponse;
import com.github.onsdigital.slack.client.SlackClient;
import com.github.onsdigital.slack.messages.Colour;
import com.github.onsdigital.slack.messages.PostMessage;
import com.github.onsdigital.slack.messages.PostMessageAttachment;
import com.github.onsdigital.zebedee.notification.NotificationException;
import com.github.onsdigital.zebedee.notification.StartUpNotifier;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.github.onsdigital.zebedee.logging.CMSLogEvent.error;
import static java.text.MessageFormat.format;

/**
 * Slack implementation of {@link StartUpNotifier}. Sends Slack messages to the configured channels informing users
 * that the CMS has restarted successfully.
 */
public class SlackStartUpNotifier implements StartUpNotifier {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private static final AtomicBoolean hasRun = new AtomicBoolean(false);

    static final String HELP_HINT_FMT = "Please raise a ticket via the {0} channel and the support team will " +
            "investigate :fix-it-gopher:";

    private SlackClient sClient;
    private List<String> channels;
    private String supportChannelID;

    /**
     * Construct a new instance of the Notifier.
     *
     * @param sClient          the {@link SlackClient} to use.
     * @param channels         a list of Slack channels to send notifications to.
     * @param supportChannelID the Slack channel ID for the publishing support channel.
     */
    public SlackStartUpNotifier(SlackClient sClient, List<String> channels, String supportChannelID) {
        this.sClient = sClient;
        this.channels = channels;
        this.supportChannelID = supportChannelID;
    }


    @Override
    public boolean notifyStartUpComplete() throws NotificationException {
        if (hasRun.compareAndSet(false, true)) {
            List<Future<PostMessageResponse>> futures = sendNotificactions();
            return checkResults(futures);
        }

        return false;
    }

    private List<Future<PostMessageResponse>> sendNotificactions() throws NotificationException {
        List<Callable<PostMessageResponse>> notifications = channels.stream()
                .map(chan -> newStartUpCompleteNotifyTask(chan))
                .collect(Collectors.toList());

        List<Future<PostMessageResponse>> futures = new ArrayList<>();
        try {
            futures = executorService.invokeAll(notifications);
        } catch (Exception ex) {
            throw new NotificationException("error invoking publishing system start up notification tasks", ex);
        }

        return futures;
    }

    private boolean checkResults(List<Future<PostMessageResponse>> futures) throws NotificationException {
        boolean isSuccess = true;

        for (Future<PostMessageResponse> f : futures) {
            PostMessageResponse response;
            try {
                // Call get to check the result of the future. If unsuccessfull it will throw an exception with
                // details of the failure.
                response = f.get();
            } catch (Exception ex) {
                throw new NotificationException("error sending publishing system start up notification", ex.getCause());
            }

            if (!response.isOk()) {
                isSuccess = false;
                error().data("channel", response.getChannel()).log("slack start up notification was unsuccessful");
                break;
            }
        }
        return isSuccess;
    }

    private Callable<PostMessageResponse> newStartUpCompleteNotifyTask(String channel) {
        return () -> {
            Profile profile = sClient.getProfile();

            PostMessage msg = profile
                    .newPostMessage(channel, "Publishing system restart completed successfully :tada:")
                    .addAttachment(new PostMessageAttachment("Time", new Date().toString(), Colour.GOOD))
                    .addAttachment(new PostMessageAttachment("Not expecting this alert?",
                            format(HELP_HINT_FMT, supportChannelID),
                            Colour.GOOD));

            try {
                return sClient.sendMessage(msg);
            } catch (Exception ex) {
                String errorMsg = format("error sending start up notification to slack channel: {0}", channel);
                throw new NotificationException(errorMsg, ex);
            }
        };
    }
}
