package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.publishing.PublishedCollection;
import com.github.onsdigital.zebedee.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Read published collections from file.
 */
public class FileBasedPublishingReport {

    private Path path;

    public FileBasedPublishingReport(Path path) {
        this.path = path;
    }

    public List<PublishedCollection> get() throws IOException {
        List<PublishedCollection> publishedCollections = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path filePath : stream) {
                if (!Files.isDirectory(filePath)) {
                    try (InputStream input = Files.newInputStream(filePath)) {
                        publishedCollections.add(Serialiser.deserialise(input,
                                PublishedCollection.class));
                    } catch (IOException e) {
                        Log.print("Failed to read published collection with path %s", filePath.toString());
                    }
                }
            }
        }

        return publishedCollections;
    }
}
