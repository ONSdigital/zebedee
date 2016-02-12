package com.github.onsdigital.zebedee.model.publishing.preprocess;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.json.publishing.request.Manifest;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.CollectionWriter;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionReader;
import com.github.onsdigital.zebedee.model.ZebedeeCollectionWriter;
import com.github.onsdigital.zebedee.reader.CollectionReader;
import com.github.onsdigital.zebedee.util.Log;

import javax.crypto.SecretKey;
import java.io.IOException;

public class CollectionPublishPreprocessor {

    public static void preProcessCollectionForPublish(Collection collection, SecretKey key) {
        try {
            Manifest manifest = Manifest.create(collection);
            Manifest.save(manifest, collection);

            CollectionReader collectionReader = new ZebedeeCollectionReader(collection, key);
            CollectionWriter collectionWriter = new ZebedeeCollectionWriter(collection, key);
            TimeSeriesCompressor.compressFiles(collectionReader.getReviewed(), collectionWriter.getReviewed(), collection);
        } catch (IOException | ZebedeeException e) {
            Log.print(e);
        }
    }
}
