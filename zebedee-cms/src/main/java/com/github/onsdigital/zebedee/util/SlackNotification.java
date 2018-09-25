package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.httpino.Endpoint;
import com.github.davidcarboni.httpino.Host;
import com.github.davidcarboni.httpino.Http;
import com.github.davidcarboni.httpino.Response;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.chart.Chart;
import com.github.onsdigital.zebedee.json.CollectionBase;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollection;
import com.github.onsdigital.zebedee.json.publishing.Result;
import com.github.onsdigital.zebedee.json.publishing.UriInfo;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.slack.PostMessage;
import com.github.onsdigital.zebedee.util.slack.PostMessageAttachment;
import com.github.onsdigital.zebedee.util.slack.PostMessageField;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import jdk.nashorn.internal.runtime.regexp.joni.Warnings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.http.message.BasicNameValuePair;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    private static final String slackPostMessageURI = "https://slack.com/api/chat.postMessage";
    private static final String slackUpdateMessageURI = "https://slack.com/api/chat.update";

    private static final String slackUsername = "Zebedee";

    // 1 thread will delay slack messages, but multiple threads might cause us to use
    // postMessage instead of update if a collection moves through each stage/status
    // faster than we get and cache the response message timestamp and channel ID
    private static final ExecutorService pool = Executors.newFixedThreadPool(1);

    private static final String DATA_JSON = "data.json";

    private static SimpleDateFormat slackFieldFormatAccurate = new SimpleDateFormat("EEE dd MMM yyyy 'at' HH:mm:ss.SSSS");
    private static SimpleDateFormat slackFieldFormatVague = new SimpleDateFormat("EEE dd MMM yyyy 'at' HH:mm");

    private static Cache<String, AbstractMap.SimpleEntry<String, String>> collectionSlackMessageTimestamps = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    @SafeVarargs
    public static void alarm(String alarm, PostMessageField... args) {
        PostMessage pm = new PostMessage(slackUsername, slackAlarmChannel, PostMessage.Emoji.HeavyExclamationMark);
        PostMessageAttachment attch = new PostMessageAttachment(alarm, "Alarm", PostMessageAttachment.Color.Danger);
        pm.attachments.add(attch);
        attch.fields.addAll(Arrays.asList(args));
        postSlackMessage(pm, null, true);
    }

    private static Future<Exception> postSlackMessage(PostMessage message) {
        return postSlackMessage(message, null, false);
    }

    private static Future<Exception> postSlackMessage(PostMessage message, String collectionID, boolean forceNewMessage) {
        if(slackToken == null || slackToken.length() == 0) {
            logDebug("postSlackMessage slackToken is null").log();
            return null;
        }

        return pool.submit(() -> {
            Exception result = null;
            Response<JsonObject> response;
            try (Http http = new Http()) {
                if(collectionID != null && !forceNewMessage) {
                    // try and update a previous message
                    AbstractMap.SimpleEntry<String,String> entry = collectionSlackMessageTimestamps.getIfPresent(collectionID);
                    if(entry != null) {
                        message.ts = entry.getKey();
                        message.channel = entry.getValue();
                    }
                }

                Endpoint slack = new Endpoint(message.ts == null ? slackPostMessageURI : slackUpdateMessageURI, "");

                response = http.postJson(slack, message, JsonObject.class,
                        new BasicNameValuePair("Authorization", "Bearer " + slackToken),
                        new BasicNameValuePair("Content-Type", "application/json"));

                if(!response.body.get("ok").getAsBoolean()) {
                    logDebug("sendSlackMessage")
                            .addParameter("error", response.body.get("error").getAsString())
                            .addParameter("responseStatusCode", response.statusLine.getStatusCode())
                            .log();
                    return result;
                }

                String messageTs = response.body.get("ts").getAsString();
                String channelID = response.body.get("channel").getAsString();
                // message.ts is null if we're doing the initial postMessage rather than an update
                if(message.ts == null && collectionID != null && messageTs != null && channelID != null) {
                    // chat.update API doesn't support channel names, so we also store the channel ID
                    // from the initial chat.postMessage which we can use later
                    // (there's probably a more elegant way of doing this, or just use the channel ID in config)
                    collectionSlackMessageTimestamps.put(collectionID, new AbstractMap.SimpleEntry<>(messageTs, channelID));
                }

                logDebug("sendSlackMessage")
                        .addParameter("messageTimestamp", messageTs)
                        .addParameter("updateTimestamp", message.ts)
                        .addParameter("responseStatusCode", response.statusLine.getStatusCode())
                        .log();
            } catch (Exception e) {
                result = e;
                logError(e, "sendSlackMessage json error.").log();
            }
            return result;
        });
    }

    @SafeVarargs
    public static void collectionAlarm(Collection c, String alarm, PostMessageField... args) {
        if (c == null) {
            // not enough info to be able to notify anything useful.
            logDebug("collectionAlarm collection is null").log();
            return;
        } else if (c.getDescription() == null) {
            // not enough info to be able to notify anything useful.
            logDebug("collectionAlarm collection description is null").log();
            return;
        }

        PostMessage pm = new PostMessage(slackUsername, slackDefaultChannel, PostMessage.Emoji.HeavyExclamationMark);
        PostMessageAttachment attch = new PostMessageAttachment(alarm, "Alarm", PostMessageAttachment.Color.Danger);
        pm.attachments.add(attch);

        attch.fields.add(new PostMessageField("Collection", "<" + Configuration.getFlorenceUrl() + "/florence/collections/" + c.getDescription().getId() + "|" + c.getDescription().getName()+ ">", true));
        attch.fields.addAll(Arrays.asList(args));

        // always force a new message for a scheduled collection failure so it's obvious
        postSlackMessage(pm, c.getDescription().getId(), true);
    }

    @SafeVarargs
    public static void collectionWarning(Collection c, String warning, PostMessageField... args) {
        if (c == null) {
            // not enough info to be able to notify anything useful.
            logDebug("collectionWarning collection is null").log();
            return;
        } else if (c.getDescription() == null) {
            // not enough info to be able to notify anything useful.
            logDebug("collectionWarning collection description is null").log();
            return;
        }

        PostMessage pm = new PostMessage(slackUsername, slackDefaultChannel, PostMessage.Emoji.HeavyExclamationMark);

        PostMessageAttachment attch = new PostMessageAttachment("", warning, PostMessageAttachment.Color.Warning);
        pm.attachments.add(attch);

        attch.fields.add(new PostMessageField("Collection", "<" + Configuration.getFlorenceUrl() + "/florence/collections/" + c.getDescription().getId() + "|" + c.getDescription().getName()+ ">", true));
        attch.fields.addAll(Arrays.asList(args));

        // always force a new message for a scheduled collection failure so it's obvious
        postSlackMessage(pm, c.getDescription().getId(), true);
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

        ArrayList<PostMessageField> args = new ArrayList<>();

        if (c.getDescription().approvalStatus != null) {
            args.add(new PostMessageField("Approval status", c.getDescription().approvalStatus.toString(), true));
        }

        if (c.getDescription().publishTransactionIds != null
                && !c.getDescription().publishTransactionIds.isEmpty()) {
            args.add(new PostMessageField("Transaction IDs", StringUtils.join(c.getDescription().publishTransactionIds.values(), "\n"), true));
        }

        collectionAlarm(c, "Scheduled collection failed to publish", args.toArray(new PostMessageField[0]));
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

        PostMessage pm = new PostMessage(slackUsername, slackPublishChannel, PostMessage.Emoji.ChartWithUpwardsTrend, "Unknown publish stage or status");

        PostMessageAttachment attch = new PostMessageAttachment("", "", PostMessageAttachment.Color.Good);
        pm.attachments.add(attch);

        attch.fields.add(new PostMessageField("Collection", "<" + Configuration.getFlorenceUrl() + "/florence/collections/" + collection.getId() + "|" + collection.getName() + ">", true));
        if(collection.getPublishDate() == null) {
            attch.fields.add(new PostMessageField("Publish date", "Manual", true));
        } else {
            attch.fields.add(new PostMessageField("Publish date", slackFieldFormatVague.format(collection.getPublishDate()), true));
        }

        boolean forceNewMessage = false;

        switch (stage) {
            case PrePublish:
                switch (status) {
                    case Started:
                        // start a new message (rather than updating) when
                        // a scheduled publish starts (only occurs if a
                        // failed publish is then rescheduled)
                        forceNewMessage = true;
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
                        // start a new message (rather than updating) when
                        // a manual publish starts (only occurs if a
                        // failed publish is then manually published)
                        forceNewMessage = true;
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

                attch.fields.add(new PostMessageField("Files published", String.format("%d", result.transaction.uriInfos.size()), true));

                result.transaction.uriInfos
                        .stream()
                        .filter(info -> info.uri.endsWith(DATA_JSON))
                        .findFirst()
                        .ifPresent(urlInfo -> {
                            attch.fields.add(new PostMessageField("Example page", "https://www.ons.gov.uk" + urlInfo.uri.substring(0, urlInfo.uri.length() - (DATA_JSON).length()), false));
                        });
            }

        }

        postSlackMessage(pm, collection.getId(), forceNewMessage);
    }
}
