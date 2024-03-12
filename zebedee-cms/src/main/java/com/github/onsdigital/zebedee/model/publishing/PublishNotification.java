package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.httpino.*;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.publishing.legacycacheapi.LegacyCacheApiClient;
import com.github.onsdigital.zebedee.model.publishing.legacycacheapi.LegacyCacheApiPayload;
import com.github.onsdigital.zebedee.model.publishing.legacycacheapi.LegacyCacheApiPayloadBuilder;
import com.github.onsdigital.zebedee.util.SlackNotification;
import com.github.onsdigital.zebedee.util.slack.PostMessageField;
import org.joda.time.DateTime;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;
import static com.github.onsdigital.logging.v2.event.SimpleEvent.error;

/**
 * Created by bren on 16/12/15.
 * Publish notification to be sent to the website for caching timing purposes
 */
public class PublishNotification {

    private static final List<Host> websiteHosts;
    private static final Host legacyCacheApiHost;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private NotificationPayload payload;
    private java.util.Collection<LegacyCacheApiPayload> legacyCacheApiPayloads;

    static {
        websiteHosts = Configuration.getWebsiteHosts();
        legacyCacheApiHost = new Host(Configuration.getLegacyCacheApiUrl());
    }

    public PublishNotification(Collection collection, List<String> urisToUpdate, List<ContentDetail> urisToDelete) {
        if (Configuration.isLegacyCacheAPIEnabled()) {
            this.legacyCacheApiPayloads = new LegacyCacheApiPayloadBuilder.Builder().collection(collection).build().getPayloads();
        } else {
            // Delay the clearing of the cache after publish to minimise load on the server while publishing.
            Date clearCacheDate = new DateTime(collection.getDescription().getPublishDate())
                    .plusSeconds(Configuration.getSecondsToCacheAfterScheduledPublish()).toDate();
            this.payload = new NotificationPayload(collection.getDescription().getId(), urisToUpdate, urisToDelete, clearCacheDate);
        }
    }

    public PublishNotification(Collection collection) {
        this(collection, null, null);
    }

    public void sendNotification(EventType eventType) {
        if (Configuration.isLegacyCacheAPIEnabled()) {
            sendRequestToLegacyCacheApi(eventType);
        } else {
            sendNotificationToWebsite(eventType);
        }
    }

    private void sendNotificationToWebsite(EventType eventType) {
        Host host;
        try (Http http = new Http()) {
            for (Host h : websiteHosts) {
                host = h;
                info().data("collectionId", payload.collectionId)
                        .data("websiteHost", host.toString())
                        .data("eventType", eventType.name())
                        .log("sending publish notification to website host");
                try {
                    Endpoint endpoint = new Endpoint(host, getEndPointName(eventType));
                    Response<WebsiteResponse> response = http.postJson(endpoint, payload, WebsiteResponse.class);
                    String responseMessage = response.body == null ? response.statusLine.getReasonPhrase() : response.body.getMessage();
                    if (response.statusLine.getStatusCode() > 302) {
                        info().data("websiteHost", host.toString())
                                .data("responseMessage", responseMessage)
                                .data("collectionId", payload.collectionId)
                                .log("Error response from website for publish notification");
                    } else {
                        info().data("websiteHost", host.toString())
                                .data("responseMessage", responseMessage)
                                .data("collectionId", payload.collectionId)
                                .log("Response from website for publish notification");
                    }
                } catch (Exception e) {
                    error().data("collectionId", payload.collectionId)
                            .data("websiteHost", host.toString())
                            .data("eventType", eventType)
                            .logException(e, "failed sending publish notification to website");
                    String eventName = "";
                    try {
                        eventName = getEndPointName(eventType);
                    } catch (BadRequestException ex) {
                        eventName = "unknown: " + eventType.toString();
                    }
                    // FIXME it might be better to use collectionAlarm rather than alarm
                    // but the NotificationPayload only has the collection ID
                    SlackNotification.alarm(
                            "Failed sending publish notifications to website",
                            new PostMessageField("Event", eventName, true),
                            new PostMessageField("Host", host.toString(), true),
                            new PostMessageField("Collection ID", payload.collectionId, true)
                    );
                }
            }
        }
    }

    private void sendRequestToLegacyCacheApi(EventType eventType) {
        if (eventType.equals(EventType.APPROVED) || eventType.equals(EventType.UNLOCKED)) {
            info().data("eventType", eventType.name()).log("sending request to Legacy Cache API");

            removePublishDateForUnlockedEvents(eventType);

            try (Http http = new Http()) {
                LegacyCacheApiClient.sendPayloads(http, legacyCacheApiHost, legacyCacheApiPayloads);
            } catch (IOException e) {
                String collectionId = legacyCacheApiPayloads.stream().findFirst().map(p -> p.collectionId).orElse("");
                String payloads = Serialiser.serialise(legacyCacheApiPayloads);

                error().data("collectionId", collectionId)
                        .data("payloads", payloads)
                        .data("eventType", eventType)
                        .logException(e, "failed to send request to Legacy Cache API");

                SlackNotification.alarm(
                        "Failed to send request to Legacy Cache API",
                        new PostMessageField("Event", eventType.name(), true),
                        new PostMessageField("Collection ID", collectionId, true),
                        new PostMessageField("Payloads", payloads, false)
                );
            }
        }
    }

    /**
     * Returns website endpoint for sending notification to
     *
     * @return
     */
    private String getEndPointName(EventType eventType) throws BadRequestException {
        switch (eventType) {
            case APPROVED:
                return "upcoming";
            case PUBLISHED:
                return "published";
            case UNLOCKED:
                return "publishcancelled";
            default:
                throw new BadRequestException("Notification can not be send for event type " + eventType);
        }
    }

    public static String format(Date date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMAT.format(date);
    }

    public boolean hasUriToDelete(String uriToDelete) {
        return this.payload.urisToDelete.stream().anyMatch(contentDetail -> contentDetail.uri.equals(uriToDelete));
    }

    void removePublishDateForUnlockedEvents(EventType eventType) {
        if (eventType.equals(EventType.UNLOCKED)) {
            for (LegacyCacheApiPayload legacyCacheApiPayload : legacyCacheApiPayloads) {
                legacyCacheApiPayload.publishDate = null;
            }
        }
    }

    java.util.Collection<LegacyCacheApiPayload> getLegacyCacheApiPayloads() {
        return this.legacyCacheApiPayloads;
    }

    class NotificationPayload {
        public String collectionId;
        public String publishDate;
        public List<String> urisToUpdate;
        public List<ContentDetail> urisToDelete;
        public String key = Configuration.getReindexKey();

        public NotificationPayload(String collectionId, List<String> urisToUpdate, List<ContentDetail> urisToDelete, Date publishDate) {
            this.collectionId = collectionId;
            this.publishDate = format(publishDate);
            this.urisToUpdate = urisToUpdate;
            this.urisToDelete = urisToDelete;
        }
    }
}
