package com.github.onsdigital.zebedee.json.publishing.request;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A manifest is a list of files to process for a collection when publishing.
 */
public class Manifest {

    public static final String filename = "manifest.json";

    public List<FileCopy> filesToCopy = new ArrayList<>();

    /**
     * Create a new manifest for the given collection.
     *
     * @param collection
     * @return
     * @throws IOException
     */
    public static Manifest create(Collection collection) throws IOException {
        Manifest manifest = new Manifest();

        for (String uri : collection.reviewed.uris()) {
            if (VersionedContentItem.isVersionedUri(uri)) {
                manifest.addFileCopy(VersionedContentItem.resolveBaseUri(uri), uri);
            }
        }

        return manifest;
    }

    /**
     * Load the manifest for the given collection.
     *
     * @param collection
     * @return
     */
    public static Manifest load(Collection collection) {

        Path manifestPath = getManifestPath(collection);

        try (InputStream inputStream = Files.newInputStream(manifestPath)) {
            Manifest manifest = Serialiser.deserialise(inputStream, Manifest.class);
            return manifest;
        } catch (IOException e) {
            return new Manifest();
        }
    }

    /**
     * Save the manifest to file.
     *
     * @param manifest
     * @param collection
     * @return
     * @throws IOException
     */
    public static boolean save(Manifest manifest, Collection collection) throws IOException {
        Path manifestPath = getManifestPath(collection);

        try (OutputStream output = Files.newOutputStream(manifestPath)) {
            Serialiser.serialise(output, manifest);
            return true;
        }
    }

    public static Path getManifestPath(Collection collection) {
        return collection.path.resolve(filename);
    }

    public void addFileCopy(String from, String to) {
        this.filesToCopy.add(new FileCopy(from, to));
    }
}
