package com.github.onsdigital.zebedee.model.publishing.preprocess;

import com.github.onsdigital.zebedee.json.publishing.request.Manifest;
import com.github.onsdigital.zebedee.model.Collection;

import javax.crypto.SecretKey;
import java.io.IOException;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;

public class CollectionPublishPreprocessor {

    public static void preProcessCollectionForPublish(Collection collection, SecretKey key) {
        try {
            logInfo("PRE-PUBLISH: creating manifest").collectionName(collection).log();
            Manifest manifest = Manifest.create(collection);
            Manifest.save(manifest, collection);
        } catch (IOException e) {
            logError(e, "Unexpected error for preProcessCollectionForPublish").log();
        }
    }
}
