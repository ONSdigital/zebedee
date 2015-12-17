package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.httpino.Endpoint;
import com.github.davidcarboni.httpino.Host;
import com.github.davidcarboni.httpino.Response;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.json.CollectionType;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.Http;
import com.github.onsdigital.zebedee.util.Log;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.Date;
import java.util.List;

/**
 * Created by bren on 16/12/15.
 * Publish notification to be sent to the website for caching timing purposes
 */
public class PublishNotification {

    private NotificationPayload payload;
    private Boolean manual;

    public PublishNotification(Collection collection, List<String> uriList) {
        this.payload = new NotificationPayload(collection.description.id, uriList, collection.description.publishDate);
        this.manual = CollectionType.manual.equals(collection.description.type);
    }

    public PublishNotification(Collection collection) {
        this(collection, null);
    }

    public void sendNotification(EventType eventType) {
        if (manual) {
            System.out.println("Skipped sending notification for manual collection, id:" + payload.collectionId);
            return;
        }

        System.out.println("Sending publish notification to website for " + eventType.name());
        try (Http http = new Http()) {
            Endpoint endpoint = new Endpoint(new Host(Configuration.getWebsiteUrl()), getEndPointName(eventType));
            Response<WebsiteResponse> response = http.postJson(endpoint, payload, WebsiteResponse.class);
            System.out.println("Response from website for publish notification: " + response.body.getMessage());
        } catch (Exception e) {
            Log.print("Failed sending publish notification to website fo " + eventType);
            ExceptionUtils.printRootCauseStackTrace(e);
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

    class NotificationPayload {
        public String collectionId;
        public Date publishDate;
        public List<String> uriList;
        public String key = Configuration.getReindexKey();

        NotificationPayload(String collectionId, List<String> uriList, Date publishDate) {
            this.collectionId = collectionId;
            this.uriList = uriList;
            this.publishDate = publishDate;
        }
    }
}