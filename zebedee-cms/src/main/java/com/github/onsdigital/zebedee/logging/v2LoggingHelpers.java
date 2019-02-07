package com.github.onsdigital.zebedee.logging;

import com.github.onsdigital.zebedee.model.Collection;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.function.BiFunction;

public class v2LoggingHelpers {

    private static final String BLOCKING_PATH = "blockingPath";
    private static final String BLOCKING_COLLECTION = "blockingCollection";
    private static final String TARGET_PATH = "targetPath";
    private static final String TARGET_COLLECTION = "targetCollection";

    private static BiFunction<String, String, String> COLLECTION_CONTENT_PATH = (collectioName, uri) -> {
        uri = uri.startsWith("/") ? uri.substring(1) : uri;
        return Paths.get(collectioName).resolve("inprogress").resolve(uri).toString();
    };

    public static HashMap<String, String> GenerateCollectionSaveConflictMap(Collection targetCollection, Collection blockingCollection, String targetURI) throws IOException {

        HashMap<String, String> ConflictLogMap = new HashMap<String, String>();

        if (targetCollection != null) {
            String name = targetCollection.getDescription().getName();
            ConflictLogMap.put(TARGET_PATH, COLLECTION_CONTENT_PATH.apply(name, targetURI));
            ConflictLogMap.put(TARGET_COLLECTION, name);
        }

        if (blockingCollection != null) {
            String name = blockingCollection.getDescription().getName();
            ConflictLogMap.put(BLOCKING_PATH, COLLECTION_CONTENT_PATH.apply(name, targetURI));
            ConflictLogMap.put(BLOCKING_COLLECTION, name);
        }

        return ConflictLogMap;
    }

}
