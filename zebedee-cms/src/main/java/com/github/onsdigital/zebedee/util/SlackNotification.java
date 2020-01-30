package com.github.onsdigital.zebedee.util;

import com.github.davidcarboni.httpino.Endpoint;
import com.github.davidcarboni.httpino.Http;
import com.github.davidcarboni.httpino.Response;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.CollectionBase;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollection;
import com.github.onsdigital.zebedee.json.publishing.Result;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.slack.PostMessage;
import com.github.onsdigital.zebedee.util.slack.PostMessageAttachment;
import com.github.onsdigital.zebedee.util.slack.PostMessageField;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.message.BasicNameValuePair;

import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

/**
 * Sends messages to slack.
 */
public class SlackNotification {

    public enum CollectionStage {
        PRE_PUBLISH,
        PUBLISH,
        POST_PUBLISH
    }

    public enum StageStatus {
        STARTED,
        COMPLETED,
        FAILED
    }

    private static final String slackToken = System.getenv("slack_api_token");
    private static final String slackDefaultChannel = System.getenv("slack_default_channel");
    private static final String slackAlarmChannel = System.getenv("slack_alarm_channel");
    private static final String slackPublishChannel = System.getenv("slack_publish_channel");

    private static final String slackPostMessageURI = "https://slack.com/api/chat.postMessage";
    private static final String slackUpdateMessageURI = "https://slack.com/api/chat.update";

    private static final String slackUsername = Configuration.getSlackUsername();

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

    public static void alarm(String alarm, PostMessageField... args) {
        PostMessage pm = new PostMessage(slackUsername, slackAlarmChannel, PostMessage.Emoji.HEAVY_EXCLAMATION_MARK);
        PostMessageAttachment attch = new PostMessageAttachment(alarm, "Alarm", PostMessageAttachment.Color.DANGER);
        pm.getAttachments().add(attch);
        attch.getFields().addAll(Arrays.asList(args));
        postSlackMessage(pm, null, true);
    }

    private static void addCollectionAttachment(PostMessageAttachment attch, Collection collection) {
        attch.getFields().add(new PostMessageField("Collection", "<" + Configuration.getFlorenceUrl() + "/florence/collections/" + collection.getDescription().getId() + "|" + collection.getDescription().getName() + ">", true));

        if (collection.getDescription().getPublishDate() == null) {
            attch.getFields().add(new PostMessageField("Publish date", "Manual", true));
        } else {
            attch.getFields().add(new PostMessageField("Publish date", slackFieldFormatVague.format(collection.getDescription().getPublishDate()), true));
        }

        if (collection.getDescription().approvalStatus != null) {
            attch.getFields().add(new PostMessageField("Approval status", collection.getDescription().approvalStatus.toString(), true));
        }

        Map<String, String> transactionIdMap = collection.getDescription().getPublishTransactionIds();
        if (transactionIdMap != null && !transactionIdMap.isEmpty()) {
            attch.getFields().add(new PostMessageField("Transaction IDs", StringUtils.join(transactionIdMap.values(), "\n"), true));
        }
    }

    private static void addCollectionAttachment(PostMessageAttachment attch, CollectionBase collection) {
        attch.getFields().add(new PostMessageField("Collection", "<" + Configuration.getFlorenceUrl() + "/florence/collections/" + collection.getId() + "|" + collection.getName() + ">", true));

        if (collection.getPublishDate() == null) {
            attch.getFields().add(new PostMessageField("Publish date", "Manual", true));
        } else {
            attch.getFields().add(new PostMessageField("Publish date", slackFieldFormatVague.format(collection.getPublishDate()), true));
        }

        if (collection.getClass() == PublishedCollection.class) {
            PublishedCollection publishedCollection = (PublishedCollection) collection;

            if (publishedCollection.publishStartDate != null && publishedCollection.publishEndDate != null) {
                String timeTaken = String.format("%.2f", (publishedCollection.publishEndDate.getTime()
                        - publishedCollection.publishStartDate.getTime()) / 1000.0);

                attch.getFields().add(new PostMessageField("Duration", timeTaken + " seconds", true));
                attch.getFields().add(new PostMessageField("Publish start date", slackFieldFormatAccurate.format(publishedCollection.publishStartDate), true));
                attch.getFields().add(new PostMessageField("Publish end date", slackFieldFormatAccurate.format(publishedCollection.publishEndDate), true));
            } else if (publishedCollection.publishStartDate != null) {
                // publishing in progress?
                attch.getFields().add(new PostMessageField("Publish start date", slackFieldFormatAccurate.format(publishedCollection.publishStartDate), true));
            } else if (publishedCollection.publishEndDate != null) {
                // definitely unexpected
                attch.getFields().add(new PostMessageField("Publish end date", slackFieldFormatAccurate.format(publishedCollection.publishEndDate), true));
            }

            if (publishedCollection.publishResults != null && publishedCollection.publishResults.size() > 0) {
                // FIXME consider reporting on all transactions not just the first one
                Result result = publishedCollection.publishResults.get(0);

                attch.getFields().add(new PostMessageField("Files published", String.format("%d", result.transaction.uriInfos.size()), true));

                result.transaction.uriInfos
                        .stream()
                        .filter(info -> info.uri.endsWith(DATA_JSON))
                        .findFirst()
                        .ifPresent(urlInfo -> {
                            attch.getFields().add(new PostMessageField("Example page", "https://www.ons.gov.uk" + urlInfo.uri.substring(0, urlInfo.uri.length() - (DATA_JSON).length()), false));
                        });
            }

        }
    }

    private static void postSlackMessage(PostMessage message, String collectionID, boolean forceNewMessage) {
        if (slackToken == null || slackToken.length() == 0) {
            info().log("postSlackMessage slackToken is null");
            return;
        }

        pool.submit(() -> {
            Exception result = null;
            Response<JsonObject> response;
            try (Http http = new Http()) {
                if (collectionID != null && !forceNewMessage) {
                    // try and update a previous message
                    AbstractMap.SimpleEntry<String, String> entry = collectionSlackMessageTimestamps.getIfPresent(collectionID);
                    if (entry != null) {
                        message.setTs(entry.getKey());
                        message.setChannel(entry.getValue());
                    }
                }

                Endpoint slack = new Endpoint(message.getTs() == null ? slackPostMessageURI : slackUpdateMessageURI, "");

                response = http.postJson(slack, message, JsonObject.class,
                        new BasicNameValuePair("Authorization", "Bearer " + slackToken),
                        new BasicNameValuePair("Content-Type", "application/json"));

                if (!response.body.get("ok").getAsBoolean()) {
                    info().data("error", response.body.get("error").getAsString())
                            .data("responseStatusCode", response.statusLine.getStatusCode())
                            .log("sendSlackMessage");
                    return result;
                }

                String messageTs = response.body.get("ts").getAsString();
                String channelID = response.body.get("channel").getAsString();
                // message.ts is null if we're doing the initial postMessage rather than an update
                if (message.getTs() == null && collectionID != null && messageTs != null && channelID != null) {
                    // chat.update API doesn't support channel names, so we also store the channel ID
                    // from the initial chat.postMessage which we can use later
                    // (there's probably a more elegant way of doing this, or just use the channel ID in config)
                    collectionSlackMessageTimestamps.put(collectionID, new AbstractMap.SimpleEntry<>(messageTs, channelID));
                }

                info().data("messageTimestamp", messageTs)
                        .data("updateTimestamp", message.getTs())
                        .data("responseStatusCode", response.statusLine.getStatusCode())
                        .log("sendSlackMessage");
            } catch (Exception e) {
                result = e;
                error().logException(e, "sendSlackMessage json error.");
            }
            return result;
        });
    }

    public static void collectionAlarm(Collection c, String alarm, PostMessageField... args) {
        if (c == null) {
            // not enough info to be able to notify anything useful.
            info().log("collectionAlarm collection is null");
            return;
        } else if (c.getDescription() == null) {
            // not enough info to be able to notify anything useful.
            info().log("collectionAlarm collection description is null");
            return;
        }

        PostMessage pm = new PostMessage(slackUsername, slackDefaultChannel, PostMessage.Emoji.HEAVY_EXCLAMATION_MARK);
        PostMessageAttachment attch = new PostMessageAttachment(alarm, "Alarm", PostMessageAttachment.Color.DANGER);
        pm.getAttachments().add(attch);
        addCollectionAttachment(attch, c);
        attch.getFields().addAll(Arrays.asList(args));

        // always force a new message for a scheduled collection failure so it's obvious
        postSlackMessage(pm, c.getDescription().getId(), true);
    }

    public static void collectionWarning(Collection c, String warning, PostMessageField... args) {
        if (c == null) {
            // not enough info to be able to notify anything useful.
            info().log("collectionWarning collection is null");
            return;
        } else if (c.getDescription() == null) {
            // not enough info to be able to notify anything useful.
            info().log("collectionWarning collection description is null");
            return;
        }

        PostMessage pm = new PostMessage(slackUsername, slackDefaultChannel, PostMessage.Emoji.HEAVY_EXCLAMATION_MARK);
        PostMessageAttachment attch = new PostMessageAttachment("", warning, PostMessageAttachment.Color.WARNING);
        pm.getAttachments().add(attch);
        addCollectionAttachment(attch, c);
        attch.getFields().addAll(Arrays.asList(args));

        // always force a new message for a scheduled collection failure so it's obvious
        postSlackMessage(pm, c.getDescription().getId(), true);
    }

    public static void scheduledPublishFailure(Collection c) {
        if (c == null) {
            // not enough info to be able to notify anything useful.
            info().log("scheduledPublishFailure collection is null");
            return;
        } else if (c.getDescription() == null) {
            // not enough info to be able to notify anything useful.
            info().log("scheduledPublishFailure collection description is null");
            return;
        }

        collectionAlarm(c, "Scheduled collection failed to publish");
    }

    /**
     * Send a slack message containing collection publication information
     *
     * @param collection
     */
    public static void publishNotification(CollectionBase collection, CollectionStage stage, StageStatus status) {
        if (collection == null) {
            info().log("failed to send publish slack notification as published collection was null");
            return;
        }

        PostMessage pm = new PostMessage(slackUsername, slackPublishChannel, PostMessage.Emoji.CHART_WITH_UPWARDS_TREND, "Unknown publish stage or status");
        PostMessageAttachment attch = new PostMessageAttachment("", "", PostMessageAttachment.Color.GOOD);
        pm.getAttachments().add(attch);
        addCollectionAttachment(attch, collection);

        boolean forceNewMessage = false;

        switch (stage) {
            case PRE_PUBLISH:
                switch (status) {
                    case STARTED:
                        // start a new message (rather than updating) when
                        // a scheduled publish starts (only occurs if a
                        // failed publish is then rescheduled)
                        forceNewMessage = true;
                        pm.setText("Pre-publish started for collection");
                        attch.setColor("warning");
                        break;
                    case COMPLETED:
                        pm.setText("Pre-publish completed for collection");
                        attch.setColor("good");
                        break;
                    case FAILED:
                        pm.setText("Pre-publish *failed* for collection");
                        attch.setColor("danger");
                        break;
                }
                break;
            case PUBLISH:
                switch (status) {
                    case STARTED:
                        // start a new message (rather than updating) when
                        // a manual publish starts (only occurs if a
                        // failed publish is then manually published)
                        forceNewMessage = true;
                        pm.setText("Publish started for collection");
                        attch.setColor("warning");
                        break;
                    case COMPLETED:
                        pm.setText("Publish completed for collection");
                        attch.setColor("good");
                        break;
                    case FAILED:
                        pm.setText("Publish *failed* for collection");
                        attch.setColor("danger");
                        break;
                }
                break;
            case POST_PUBLISH:
                switch (status) {
                    case STARTED:
                        pm.setText("Post-publish started for collection");
                        attch.setColor("warning");
                        break;
                    case COMPLETED:
                        pm.setText("Post-publish completed for collection");
                        attch.setColor("good");
                        break;
                    case FAILED:
                        pm.setText("Post-publish *failed* for collection");
                        attch.setColor("danger");
                        break;
                }
                break;
        }

        postSlackMessage(pm, collection.getId(), forceNewMessage);
    }
}
