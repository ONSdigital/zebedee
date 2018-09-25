package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.httpino.Endpoint;
import com.github.davidcarboni.httpino.Host;
import com.github.davidcarboni.httpino.Http;
import com.github.davidcarboni.httpino.Response;
import com.github.onsdigital.zebedee.json.CollectionBase;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollection;
import com.github.onsdigital.zebedee.json.publishing.Result;
import com.github.onsdigital.zebedee.json.publishing.UriInfo;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.slack.PostMessage;
import com.github.onsdigital.zebedee.util.slack.PostMessageAttachment;
import com.github.onsdigital.zebedee.util.slack.PostMessageField;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.http.message.BasicNameValuePair;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logDebug;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logWarn;

/**
 * Sends messages to slack.
 */
public class SlackNotification {

    public enum CollectionStage {
        PrePublish,
        Publish,
        PostPublish
    }

    public enum StageStatus {
        Started,
        Completed,
        Failed
    }

    private static final String slackToken = System.getenv("slack_api_token");
    private static final String slackDefaultChannel = System.getenv("slack_default_channel");
    private static final String slackAlarmChannel = System.getenv("slack_alarm_channel");
    private static final String slackPublishChannel = System.getenv("slack_publish_channel");

    private static final String slackBaseUri = "https://slack.com/api/chat.postMessage";
    private static final Host slackHost = new Host(slackBaseUri);
    private static final ExecutorService pool = Executors.newFixedThreadPool(1);
    private static final String DATA_JSON = "data.json";

    private static FastDateFormat format = FastDateFormat.getInstance("HH:mm", TimeZone.getTimeZone("Europe/London"));
    private static SimpleDateFormat slackFieldFormatAccurate = new SimpleDateFormat("EEE dd MMM yyyy 'at' HH:mm:ss.SSSS");
    private static SimpleDateFormat slackFieldFormatVague = new SimpleDateFormat("EEE dd MMM yyyy 'at' HH:mm");

    @Deprecated
    public static void send(String message) {
        PostMessage pm = new PostMessage();
        pm.icon_emoji = ":chart_with_upwards_trend:";
        pm.username = "Bot";
        pm.channel = slackDefaultChannel;
        pm.text = message;

        postSlackMessage(pm);
    }

    @Deprecated
    public static void alarm(String message) {
        PostMessage pm = new PostMessage();
        pm.icon_emoji = ":heavy_exclamation_mark:";
        pm.username = "Alarm";
        pm.channel = slackAlarmChannel;
        pm.text = message;

        postSlackMessage(pm);
    }

    private static Future<Exception> postSlackMessage(PostMessage message) {
        if(slackToken == null || slackToken.length() == 0) {
            logDebug("postSlackMessage slackToken is null").log();
            return null;
        }

        return pool.submit(() -> {
            Exception result = null;
            try (Http http = new Http()) {
                Endpoint slack = new Endpoint(slackBaseUri, "");

                Response<JsonObject> response = http.postJson(slack, message, JsonObject.class,
                        new BasicNameValuePair("Authorization", "Bearer " + slackToken),
                        new BasicNameValuePair("Content-Type", "application/json"));

                logDebug("sendSlackMessage").addParameter("responseStatusCode", response.statusLine.getStatusCode()).log();
            } catch (Exception e) {
                result = e;
                logError(e, "sendSlackMessage json error.").log();
            }
            return result;
        });
    }

    public static void scheduledPublishFailure(Collection c) {
        if (c == null) {
            // not enough info to be able to notify anything useful.
            logDebug("scheduledPublishFailure collection is null").log();
            return;
        } else if (c.getDescription() == null) {
            // not enough info to be able to notify anything useful.
            logDebug("scheduledPublishFailure collection description is null").log();
            return;
        }

        PostMessage pm = new PostMessage();
        pm.icon_emoji = ":heavy_exclamation_mark:";
        pm.username = "Alarm";
        pm.channel = slackAlarmChannel;
        pm.attachments = new ArrayList<>();

        PostMessageAttachment attch = new PostMessageAttachment();
        attch.title = "Scheduled collection failed to publish";
        attch.color = "danger";
        pm.attachments.add(attch);

        attch.fields = new ArrayList<>();
        attch.fields.add(new PostMessageField("Collection", "<https://some.domain/collections/" + c.getDescription().getId() + "|" + c.getDescription().getName()+ ">", true));

        if (c.getDescription().approvalStatus != null) {
            attch.fields.add(new PostMessageField("Approval status", c.getDescription().approvalStatus.toString(), true));
        }

        if (c.getDescription().publishTransactionIds != null
                && !c.getDescription().publishTransactionIds.isEmpty()) {
            attch.fields.add(new PostMessageField("Transaction IDs", StringUtils.join(c.getDescription().publishTransactionIds.values(), "\n")));
        }

        postSlackMessage(pm);
    }

    /**
     * Send a slack message containing collection publication information
     *
     * @param collection
     */
    public static void publishNotification(CollectionBase collection, CollectionStage stage, StageStatus status) {
        if (collection == null) {
            logWarn("failed to send publish slack notification as published collection was null").log();
            return;
        }

        PostMessage pm = new PostMessage();
        pm.channel = slackPublishChannel;
        pm.icon_emoji = ":chart_with_upwards_trend:";
        pm.username = "Bot";
        pm.attachments = new ArrayList<>();
        pm.text = "Unknown publish stage or status";

        PostMessageAttachment attch = new PostMessageAttachment();
        attch.color = "good";
        attch.fields = new ArrayList<>();
        pm.attachments.add(attch);

        attch.fields.add(new PostMessageField("Collection", "<https://some.domain/collections/" + collection.getId() + "|" + collection.getName() + ">", true));
        if(collection.getPublishDate() == null) {
            attch.fields.add(new PostMessageField("Publish date", "Manual", true));
        } else {
            attch.fields.add(new PostMessageField("Publish date", slackFieldFormatVague.format(collection.getPublishDate()), true));
        }

        switch (stage) {
            case PrePublish:
                switch (status) {
                    case Started:
                        pm.text = "Pre-publish started for collection";
                        attch.color = "warning";
                        break;
                    case Completed:
                        pm.text = "Pre-publish completed for collection";
                        attch.color = "good";
                        break;
                    case Failed:
                        pm.text = "Pre-publish *failed* for collection";
                        attch.color = "danger";
                        break;
                }
                break;
            case Publish:
                switch (status) {
                    case Started:
                        pm.text = "Publish started for collection";
                        attch.color = "warning";
                        break;
                    case Completed:
                        pm.text = "Publish completed for collection";
                        attch.color = "good";
                        break;
                    case Failed:
                        pm.text = "Publish *failed* for collection";
                        attch.color = "danger";
                        break;
                }
                break;
            case PostPublish:
                switch (status) {
                    case Started:
                        pm.text = "Post-publish started for collection";
                        attch.color = "warning";
                        break;
                    case Completed:
                        pm.text = "Post-publish completed for collection";
                        attch.color = "good";
                        break;
                    case Failed:
                        pm.text = "Post-publish *failed* for collection";
                        attch.color = "danger";
                        break;
                }
                break;
        }

        if(collection.getClass() == PublishedCollection.class) {
            PublishedCollection publishedCollection = (PublishedCollection)collection;

            if (publishedCollection.publishStartDate != null && publishedCollection.publishEndDate != null) {
                String timeTaken = String.format("%.2f", (publishedCollection.publishEndDate.getTime()
                        - publishedCollection.publishStartDate.getTime()) / 1000.0);

                attch.fields.add(new PostMessageField("Duration", timeTaken + " seconds", true));
                attch.fields.add(new PostMessageField("Publish start date", slackFieldFormatAccurate.format(publishedCollection.publishStartDate), true));
                attch.fields.add(new PostMessageField("Publish end date", slackFieldFormatAccurate.format(publishedCollection.publishEndDate), true));
            } else if (publishedCollection.publishStartDate != null) {
                // publishing in progress?
                attch.fields.add(new PostMessageField("Publish start date", slackFieldFormatAccurate.format(publishedCollection.publishStartDate), true));
            } else if (publishedCollection.publishEndDate != null) {
                // definitely unexpected
                attch.fields.add(new PostMessageField("Publish end date", slackFieldFormatAccurate.format(publishedCollection.publishEndDate), true));
            }

            if(publishedCollection.publishResults != null && publishedCollection.publishResults.size() > 0) {
                // FIXME consider reporting on all transactions not just the first one
                Result result = publishedCollection.publishResults.get(0);

                int fileCount = 0;
                int fileSize = 0;

                for (UriInfo i : result.transaction.uriInfos) {
                    fileCount++;
                    fileSize += i.size;
                }
                attch.fields.add(new PostMessageField("Files published", String.format("%d (%s)", fileCount, FileUtils.byteCountToDisplaySize(fileSize)), true));

                result.transaction.uriInfos
                        .stream()
                        .filter(info -> info.uri.endsWith(DATA_JSON))
                        .findFirst()
                        .ifPresent(urlInfo -> {
                            attch.fields.add(new PostMessageField("Example page", "https://www.ons.gov.uk" + urlInfo.uri.substring(0, urlInfo.uri.length() - (DATA_JSON).length()), false));
                        });
            }

        }

        postSlackMessage(pm);
    }
}
