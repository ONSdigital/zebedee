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
                                        addPayloadForBulletinLatest(legacyCacheApiPayload.uriToUpdate, collectionId, publishDate);
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

        private void addPayloadForBulletinLatest(String uriToUpdate, String collectionId, Date clearCacheDate) {
            if (PayloadPathUpdater.isPayloadPathBulletinLatest(uriToUpdate)) {
                // first apply updatePath on uriToUpdate
                LegacyCacheApiPayload legacyCacheApiPayloadLatest = new LegacyCacheApiPayload(collectionId, uriToUpdate, clearCacheDate);
                // then add latest when updated path contains a bulletins
                legacyCacheApiPayloadLatest.uriToUpdate = PayloadPathUpdater.getPathForBulletinLatest(legacyCacheApiPayloadLatest.uriToUpdate);
                payloadsByPath.put(legacyCacheApiPayloadLatest.uriToUpdate, legacyCacheApiPayloadLatest);
            }
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
