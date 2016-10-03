package com.github.onsdigital.zebedee.json.publishing.request;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.PendingDelete;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * A manifest is a list of files to process for a collection when publishing.
 */
public class Manifest {

    public static final String filename = "manifest.json";

    public Set<FileCopy> filesToCopy = new HashSet<>();
    public Set<String> urisToDelete = new HashSet<>();

    /**
     * Loads the manifest if it exists already for a collection. If no manifest exists it creates a new manifest.
     *
     * @param collection
     * @return
     */
    public static Manifest get(Collection collection) throws IOException {

        Path manifestPath = Manifest.getManifestPath(collection);
        Manifest manifest;

        if (!Files.exists(manifestPath)) {
            manifest = Manifest.create(collection);
            Manifest.save(manifest, collection);
        } else {
            manifest = Manifest.load(collection);
        }

        return manifest;
    }

    /**
     * Create a new manifest for the given collection.
     *
     * @param collection
     * @return
     * @throws IOException
     */
    private static Manifest create(Collection collection) throws IOException {
        Manifest manifest = new Manifest();
        updateManifest(collection, manifest);
        return manifest;
    }

    private static void updateManifest(Collection collection, Manifest manifest) throws IOException {
        for (String uri : collection.reviewed.uris()) {
            if (VersionedContentItem.isVersionedUri(uri)) {
                manifest.addFileCopy(VersionedContentItem.resolveBaseUri(uri), uri);
            }
        }

        for (PendingDelete delete : collection.description.getPendingDeletes()) {
            manifest.addDelete(delete.getRoot().uri);
        }
    }

    /**
     * Load the manifest for the given collection.
     *
     * @param collection
     * @return
     */
    static Manifest load(Collection collection) {

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
    static boolean save(Manifest manifest, Collection collection) throws IOException {
        Path manifestPath = getManifestPath(collection);

        try (OutputStream output = Files.newOutputStream(manifestPath)) {
            Serialiser.serialise(output, manifest);
            return true;
        }
    }

    public static Path getManifestPath(Collection collection) {
        return collection.path.resolve(filename);
    }

    void addDelete(String uri) {
        this.urisToDelete.add(uri);
    }

    void addFileCopy(String from, String to) {
        this.filesToCopy.add(new FileCopy(from, to));
    }
}
