package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.httpino.Endpoint;
import com.github.davidcarboni.httpino.Host;
import com.github.davidcarboni.httpino.Http;
import com.github.davidcarboni.httpino.Response;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollection;
import com.github.onsdigital.zebedee.json.publishing.Result;
import com.github.onsdigital.zebedee.json.publishing.Transaction;
import com.github.onsdigital.zebedee.json.publishing.UriInfo;
import com.github.onsdigital.zebedee.model.Collection;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.http.message.BasicNameValuePair;

import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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

    private static final String slackToken = System.getenv("slack_api_token");
    private static final String slackDefaultChannel = System.getenv("slack_default_channel");
    private static final String slackAlarmChannel = System.getenv("slack_alarm_channel");
    private static final String slackPublishChannel = System.getenv("slack_publish_channel");

    private static final String slackBaseUri = "https://slack.com/api/chat.postMessage";
    private static final Host slackHost = new Host(slackBaseUri);
    private static final ExecutorService pool = Executors.newFixedThreadPool(1);
    private static final String DATA_JSON = "data.json";

    private static FastDateFormat format = FastDateFormat.getInstance("HH:mm", TimeZone.getTimeZone("Europe/London"));

    public static void scheduledPublishFailire(Collection c) {
        if (c == null || c.getDescription() == null) {
            // not enough info to be able to notify anything useful.
            return;
        }
        StringBuilder msg = new StringBuilder("WARNING! scheduled collection failed to publish:\n")
                .append("Collection ID: " + c.getDescription().getId())
                .append("\n")
                .append("Collection name: " + c.getDescription().getName());

        if (c.getDescription().approvalStatus != null) {
            msg.append("\nApproval status: " + c.getDescription().approvalStatus.toString());
        }

        if (c.getDescription().publishTransactionIds != null
                && !c.getDescription().publishTransactionIds.isEmpty()) {

            msg.append("\nTransaction IDs:");
            c.getDescription().publishTransactionIds.entrySet()
                    .stream().forEach(e -> msg.append("\n - " + e.getValue()));
        }
        alarm(msg.toString());
    }

    public static void alarm(String message) {
        String slackUsername = "Alarm";
        String slackEmoji = ":heavy_exclamation_mark:";
        send(message, slackToken, slackAlarmChannel, slackUsername, slackEmoji);
    }

    public static void send(String message) {
        String slackUsername = "Bot";
        String slackEmoji = ":chart_with_upwards_trend:";
        send(message, slackToken, slackDefaultChannel, slackUsername, slackEmoji);
    }

    public static void send(String message, String slackToken, String slackChannel, String slackUsername, String slackEmoji) {

        if (slackToken == null || slackChannel == null) {
            return;
        }

        // send the message
        Future<Exception> exceptionFuture = sendSlackMessage(slackHost, slackToken, slackChannel, slackUsername, slackEmoji, message, pool);
    }

    private static Future<Exception> sendSlackMessage(
            final Host host,
            final String token, final String channel,
            final String userName, final String emoji,
            final String text,
            ExecutorService pool
    ) {
        return pool.submit(() -> {
            Exception result = null;
            try (Http http = new Http()) {
                Endpoint slack = new Endpoint(host, "");

                PostMessage pm = new PostMessage();
                pm.channel = channel;
                pm.icon_emoji = emoji;
                pm.username = userName;
                pm.text = text;

                Response<JsonObject> response = http.postJson(slack, pm, JsonObject.class,
                        new BasicNameValuePair("Authorization", "Bearer " + token),
                        new BasicNameValuePair("Content-Type", "application/json"));

                logDebug("sendSlackMessage").addParameter("responseStatusCode", response.statusLine.getStatusCode()).log();
            } catch (Exception e) {
                result = e;
                logError(e, "sendSlackMessage json error.").log();
            }
            return result;
        });
    }

    private static Future<Exception> sendSlackPublishNotification(
            final Host host,
            final String token, final String channel,
            final String userName, final String emoji,
            final String collectionName, final String publishDate,
            final String publishDuration, final String filesPublished,
            ExecutorService pool
    ) {
        return pool.submit(() -> {
            Exception result = null;
            try (Http http = new Http()) {
                Endpoint slack = new Endpoint(host, "");

                PostMessage pm = new PostMessage();
                pm.channel = channel;
                pm.icon_emoji = emoji;
                pm.username = userName;
//                pm.text = text;
                pm.attachments = new ArrayList<>();

                PostMessageAttachment attch = new PostMessageAttachment();
//                attch.pretext = "pretext";
//                attch.text = "text";
                attch.title = "Publish completed for collection";
                attch.color = "good";
                pm.attachments.add(attch);

                attch.fields = new ArrayList<>();
                attch.fields.add(new PostMessageField("Collection", "<https://some.domain/collections/collection-id|" + collectionName + ">", true));
                attch.fields.add(new PostMessageField("Publish date", publishDate, true));
                attch.fields.add(new PostMessageField("Publish start date", publishDate, true));
                attch.fields.add(new PostMessageField("Publish end date", publishDate, true));
                attch.fields.add(new PostMessageField("Duration", publishDuration + " seconds", true));
                attch.fields.add(new PostMessageField("Files published", filesPublished, true));

                        Response<JsonObject> response = http.postJson(slack, pm, JsonObject.class,
                                new BasicNameValuePair("Authorization", "Bearer " + token),
                                new BasicNameValuePair("Content-Type", "application/json"));

                logDebug("sendSlackMessage").addParameter("responseStatusCode", response.statusLine.getStatusCode()).log();
            } catch (Exception e) {
                result = e;
                logError(e, "sendSlackMessage json error.").log();
            }
            return result;
        });
    }

    public static void main(String[] args) {
//        publishNotification(getUnpublished(), false);
        //publishNotification(getUnpublished(), true);
//        publishNotification(getPublished(), false);
        publishNotification(getPublished(), true);
    }

    public static PublishedCollection getUnpublished() {
        PublishedCollection coll = new PublishedCollection();
        coll.setName("unpublished collection");
        return coll;
    }

    public static PublishedCollection getPublished() {
        PublishedCollection coll = new PublishedCollection();
        coll.setName("published collection");
        coll.setPublishDate(new Date());

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.SECOND, -10);
        coll.publishStartDate = c.getTime();

        coll.publishEndDate = new Date();
        coll.publishResults = new ArrayList<>();
        Result r = new Result();
        r.error = false;
        r.message = "moo";
        r.transaction = new Transaction();
        coll.publishResults.add(r);
        return coll;
    }

    /**
     * Send a slack message containing collection publication information
     *
     * @param publishedCollection
     */
    public static void publishNotification(PublishedCollection publishedCollection, boolean publishComplete) {
        if (publishedCollection == null) {
            logWarn("failed to send publish slack notification as published collection was null").log();
            return;
        }

        // default to unsuccessful message.
        String slackMessage = "Collection " + publishedCollection.getName() + " did not publish";
        String timeTaken = "";

        int fileCount = 0;
        int fileSize = 0;

        if (publishComplete) {
            Result result = publishedCollection.publishResults.get(0);
            StringBuilder msg = new StringBuilder("Published collection: " + publishedCollection.getName());


            if (publishedCollection.publishStartDate != null && publishedCollection.publishEndDate != null) {
                msg.append(" publish time : " + format.format(publishedCollection.publishStartDate));

                timeTaken = String.format("%.2f", (publishedCollection.publishEndDate.getTime()
                        - publishedCollection.publishStartDate.getTime()) / 1000.0);

                msg.append(" time taken: " + timeTaken + " ");
            }

            result.transaction.uriInfos
                    .stream()
                    .filter(info -> info.uri.endsWith(DATA_JSON))
                    .findFirst()
                    .ifPresent(urlInfo -> {
                        msg.append("Example Uri: http://www.ons.gov.uk")
                                .append(urlInfo.uri.substring(0, urlInfo.uri.length() - (DATA_JSON).length()));
                    });

            for(UriInfo i : result.transaction.uriInfos) {
                fileCount++;
                fileSize += i.size;
            }

            slackMessage = msg.toString();
        }

        try {
            //sendPublishNotification(slackMessage);
            SimpleDateFormat dt1 = new SimpleDateFormat("EEE dd MMM yyyy 'at' HH:mm");
            sendSlackPublishNotification(slackHost, slackToken, slackPublishChannel,
                    "Bot", ":chart_with_upwards_trend:",
                    publishedCollection.getName(),
                    dt1.format(publishedCollection.getPublishDate()),
                    timeTaken,
                    String.format("%d (%s)", fileCount, FileUtils.byteCountToDisplaySize(fileSize)),
                    pool
            );
        } catch (Exception e) {
            logError(e, "Slack publish notification error")
                    .collectionName(publishedCollection.getName())
                    .log();
        }
    }
}
