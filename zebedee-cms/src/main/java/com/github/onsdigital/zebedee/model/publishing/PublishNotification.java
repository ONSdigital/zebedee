package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.httpino.Endpoint;
import com.github.davidcarboni.httpino.Host;
import com.github.davidcarboni.httpino.Http;
import com.github.davidcarboni.httpino.Response;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.Log;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by bren on 16/12/15.
 * Publish notification to be sent to the website for caching timing purposes
 */
public class PublishNotification {

    private static final List<Host> websiteHosts;

    static {
        String[] websiteUrls = Configuration.getWebsiteUrls();
        websiteHosts = new ArrayList<>();
        for (String websiteUrl : websiteUrls) {
            websiteHosts.add(new Host(websiteUrl));
        }
    }

    private NotificationPayload payload;

    public PublishNotification(Collection collection, List<String> uriList) {
        this.payload = new NotificationPayload(collection.description.id, uriList, collection.description.publishDate);
    }

    public PublishNotification(Collection collection) {
        this(collection, null);
    }

    public void sendNotification(EventType eventType) {

        System.out.println("Sending publish notification to website for " + eventType.name());
        try (Http http = new Http()) {
            for (Host host : websiteHosts) {
                Endpoint endpoint = new Endpoint(host, getEndPointName(eventType));
                Response<WebsiteResponse> response = http.postJson(endpoint, payload, WebsiteResponse.class);
                String responseMessage = response.body == null ? response.statusLine.getReasonPhrase() : response.body.getMessage();
                if (response.statusLine.getStatusCode() > 302) {
                    System.err.println("Error response from website for publish notification: " + responseMessage + " for collection id:" + payload.collectionId);
                } else {
                    System.out.println("Response from website for publish notification: " + responseMessage + " for collection id:" + payload.collectionId);
                }
            }
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

    private String format(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(date);
    }

    class NotificationPayload {
        public String collectionId;
        public String publishDate;
        public List<String> uriList;
        public String key = Configuration.getReindexKey();

        NotificationPayload(String collectionId, List<String> uriList, Date publishDate) {
            this.collectionId = collectionId;
            this.uriList = uriList;
            this.publishDate = format(publishDate);
        }
    }
}