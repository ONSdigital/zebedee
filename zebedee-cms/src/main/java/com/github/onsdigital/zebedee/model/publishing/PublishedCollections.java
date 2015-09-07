package com.github.onsdigital.zebedee.model.publishing;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.Zebedee;
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
 *
 */
public class PublishedCollections {

    public final Path path;
    private final Zebedee zebedee;

    public PublishedCollections(Path path, Zebedee zebedee) {
        this.path = path;
        this.zebedee = zebedee;
    }

    /**
     * Read all existing published collections from file.
     * @return
     * @throws IOException
     */
    public List<PublishedCollection> readFromFile() throws IOException {
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
