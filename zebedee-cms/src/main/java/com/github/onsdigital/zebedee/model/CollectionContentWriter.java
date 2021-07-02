package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.util.EncryptionUtils;
import com.github.onsdigital.zebedee.util.slack.AttachmentField;
import org.apache.commons.io.FileUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import static com.github.onsdigital.logging.v2.event.SimpleEvent.info;

/**
 * Collection specific implementation of ContentWriter that is session / encryption aware.
 */
public class CollectionContentWriter extends ContentWriter {
    private Collection collection;
    private SecretKey key;

    /**
     * Create a new instance of ContentWriter to write content from the given root folder.
     *
     * @param rootFolder
     */
    public CollectionContentWriter(Collection collection, SecretKey key, Path rootFolder) throws IOException, UnauthorizedException {
        super(rootFolder);
        this.collection = collection;
        this.key = key;
    }

    @Override
    public OutputStream getOutputStream(String uri) throws IOException, BadRequestException {
        Path path = resolvePath(uri);
        assertNotDirectory(path);
        if (collection.getDescription().isEncrypted()) {
            return EncryptionUtils.encryptionOutputStream(path, key);
        } else {

            String channel = Configuration.getDefaultSlackAlarmChannel();
            AttachmentField uriField = new AttachmentField("uri", uri, false);
            Root.zebedee.getSlackNotifier().sendCollectionWarning(collection, channel, "Writing unencrypted content in collection", uriField);

            info().data("uri", uri).data("collectionId", collection.getDescription().getId()).log("Writing unencrypted content in collection");

            return FileUtils.openOutputStream(path.toFile());
        }
    }
}