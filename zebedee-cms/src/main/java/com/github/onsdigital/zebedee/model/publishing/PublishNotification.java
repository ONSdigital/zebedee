package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.httpino.*;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.publishing.legacycacheapi.LegacyCacheApiClient;
import com.github.onsdigital.zebedee.model.publishing.legacycacheapi.LegacyCacheApiPayload;
import com.github.onsdigital.zebedee.model.publishing.legacycacheapi.LegacyCacheApiPayloadBuilder;
import com.github.onsdigital.zebedee.util.SlackNotification;
import com.github.onsdigital.zebedee.util.slack.PostMessageField;

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

    private static final Host legacyCacheApiHost;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private java.util.Collection<LegacyCacheApiPayload> legacyCacheApiPayloads;

    static {
        legacyCacheApiHost = new Host(Configuration.getLegacyCacheApiUrl());
    }

    public PublishNotification(Collection collection) {
        this.legacyCacheApiPayloads = new
        LegacyCacheApiPayloadBuilder.Builder().collection(collection).build().getPayloads();
    }

    public void sendNotification(EventType eventType) {
            sendRequestToLegacyCacheApi(eventType);
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

    public static String format(Date date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMAT.format(date);
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
}
