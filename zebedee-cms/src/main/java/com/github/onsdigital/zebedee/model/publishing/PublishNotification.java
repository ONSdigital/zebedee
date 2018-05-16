package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.httpino.Endpoint;
import com.github.davidcarboni.httpino.Host;
import com.github.davidcarboni.httpino.Http;
import com.github.davidcarboni.httpino.Response;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.EventType;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.util.SlackNotification;
import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

/**
 * Created by bren on 16/12/15.
 * Publish notification to be sent to the website for caching timing purposes
 */
public class PublishNotification {

    private static final List<Host> websiteHosts;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private NotificationPayload payload;

    static {
        websiteHosts = Configuration.getWebsiteHosts();
    }

    public PublishNotification(Collection collection, List<String> urisToUpdate, List<ContentDetail> urisToDelete) {

        // Delay the clearing of the cache after publish to minimise load on the server while publishing.
        Date clearCacheDate = new DateTime(collection.getDescription().getPublishDate())
                .plusSeconds(Configuration.getSecondsToCacheAfterScheduledPublish()).toDate();

        this.payload = new NotificationPayload(collection.getDescription().getId(), urisToUpdate, urisToDelete,
                clearCacheDate);
    }

    public PublishNotification(Collection collection) {
        this(collection, null, null);
    }

    public void sendNotification(EventType eventType) {
        Host host = null;
        try (Http http = new Http()) {
            for (Host h : websiteHosts) {
                host = h;
                logInfo("sending publish notification to website host")
                        .collectionId(payload.collectionId)
                        .addParameter("websiteHost", host.toString())
                        .addParameter("eventType", eventType.name())
                        .log();
                try {
                    Endpoint endpoint = new Endpoint(host, getEndPointName(eventType));
                    Response<WebsiteResponse> response = http.postJson(endpoint, payload, WebsiteResponse.class);
                    String responseMessage = response.body == null ? response.statusLine.getReasonPhrase() : response.body.getMessage();
                    if (response.statusLine.getStatusCode() > 302) {
                        logInfo("Error response from website for publish notification")
                                .addParameter("websiteHost", host.toString())
                                .addParameter("responseMessage", responseMessage)
                                .addParameter("collectionId", payload.collectionId)
                                .log();
                    } else {
                        logInfo("Response from website for publish notification")
                                .addParameter("websiteHost", host.toString())
                                .addParameter("responseMessage", responseMessage)
                                .addParameter("collectionId", payload.collectionId)
                                .log();
                    }
                } catch (Exception e) {
                    logError(e, "failed sending publish notification to website")
                            .collectionId(payload.collectionId)
                            .addParameter("websiteHost", host.toString())
                            .addParameter("eventType", eventType).log();
                    SlackNotification.alarm("Failed sending publish notification to website for " + eventType + " " +
                            "host:" + host.toString() + " collectionID" + payload.collectionId);
                }
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

    private String format(Date date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMAT.format(date);
    }

    public boolean hasUriToDelete(String uriToDelete) {
        return this.payload.urisToDelete.stream()
                .filter(contentDetail -> contentDetail.uri.equals(uriToDelete))
                .findFirst()
                .isPresent();
    }

    /**
     * return true if this PublishNotification has the given URI to update.
     *
     * @param uri - the URI to check.
     * @return - true if the URI is in the list of URI's to update
     */
    public boolean hasUriToUpdate(String uri) {
        return this.payload.urisToUpdate.contains(uri);
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