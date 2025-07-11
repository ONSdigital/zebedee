package com.github.onsdigital.zebedee.model.publishing.legacycacheapi;

import com.github.onsdigital.zebedee.model.Collection;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.*;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.*;

public class LegacyCacheApiPayloadBuilder {
    java.util.Collection<LegacyCacheApiPayload> payloads;

    private LegacyCacheApiPayloadBuilder(java.util.Collection<LegacyCacheApiPayload> payloads) {
        this.payloads = payloads;
    }

    public java.util.Collection<LegacyCacheApiPayload> getPayloads() {
        return payloads;
    }

    public static class Builder {
        private Collection collection;
        private final Map<String, LegacyCacheApiPayload> payloadsByPath = new HashMap<>();
        private enum PayloadType {
            BULLETIN, ARTICLE, COMPENDIA, NONE
        }

        public Builder() {}

        public LegacyCacheApiPayloadBuilder.Builder collection(Collection collection) {
            this.collection = collection;
            return this;
        }

        public LegacyCacheApiPayloadBuilder build() {
            Objects.requireNonNull(this.collection);
            String collectionId = collection.getDescription().getId();
            Date publishDate = collection.getDescription().getPublishDate();
            try {
                collection.reviewedUris()
                        .forEach(uri -> {
                                    String pagePath = PayloadPathUpdater.getCanonicalPagePath(uri, collectionId);
                                    LegacyCacheApiPayload legacyCacheApiPayload = new LegacyCacheApiPayload(collectionId, pagePath, publishDate);
                                    if (isValid(legacyCacheApiPayload)) {
                                        payloadsByPath.put(legacyCacheApiPayload.uriToUpdate, legacyCacheApiPayload);
                                        addPayloadForLatest(legacyCacheApiPayload.uriToUpdate, collectionId, publishDate);
                                    }
                                }
                        );
                collection.getDescription().getPendingDeletes()
                        .forEach(pendingDelete -> {
                            String uriToDelete = pendingDelete.getRoot().getUri();
                            uriToDelete = PayloadPathUpdater.getCanonicalPagePath(uriToDelete, collectionId);
                            LegacyCacheApiPayload legacyCacheApiPayload = new LegacyCacheApiPayload(collectionId, uriToDelete);
                            if (isValid(legacyCacheApiPayload)) {
                                payloadsByPath.put(legacyCacheApiPayload.uriToUpdate, legacyCacheApiPayload);
                            }
                        });
            } catch (IOException e) {
                error().data("collectionId", collectionId)
                        .logException(e, "failed initialising PublishNotification when reading collection reviewedUris");
            }

            return new LegacyCacheApiPayloadBuilder(this.payloadsByPath.values());
        }

        private PayloadType getPayloadType(String uriToUpdate) {
            if (PayloadPathUpdater.isPayloadPathBulletinLatest(uriToUpdate))
                return PayloadType.BULLETIN;
            if (PayloadPathUpdater.isPayloadPathArticleLatest(uriToUpdate))
                return PayloadType.ARTICLE;
            if (PayloadPathUpdater.isPayloadPathCompendiaLatest(uriToUpdate))
                return PayloadType.COMPENDIA;
            return PayloadType.NONE;
        }

        /**
         * Adds a payload for the 'latest' version if the URI matches a known type.
         */
        private void addPayloadForLatest(String uriToUpdate, String collectionId, Date clearCacheDate) {
            PayloadType type = getPayloadType(uriToUpdate);
            if (type == PayloadType.NONE)
                return;
            addLatestPayload(uriToUpdate, collectionId, clearCacheDate);
        }

        private void addLatestPayload(String uriToUpdate, String collectionId, Date clearCacheDate) {
            LegacyCacheApiPayload payloadLatest = new LegacyCacheApiPayload(collectionId, uriToUpdate, clearCacheDate);
            payloadLatest.uriToUpdate = PayloadPathUpdater.getPathForLatest(payloadLatest.uriToUpdate);
            payloadsByPath.put(payloadLatest.uriToUpdate, payloadLatest);
        }

        private boolean isValid(LegacyCacheApiPayload payload) {
            if (StringUtils.isEmpty(payload.uriToUpdate)) {
                warn().data("payload", payload).log("invalid payload: URI was empty or null");
                return false;
            }
            return true;
        }
    }
}
