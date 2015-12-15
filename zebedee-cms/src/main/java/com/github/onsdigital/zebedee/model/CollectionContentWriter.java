package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.util.EncryptionUtils;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Collection specific implementation of ContentWriter that is session / encryption aware.
 */
public class CollectionContentWriter extends ContentWriter {

    private Zebedee zebedee;
    private Collection collection;
    private Session session;

    /**
     * Create a new instance of ContentWriter to write content from the given root folder.
     *
     * @param rootFolder
     */
    public CollectionContentWriter(Zebedee zebedee, Collection collection, Session session, Path rootFolder) throws IOException, UnauthorizedException {
        super(rootFolder);
        this.zebedee = zebedee;
        this.collection = collection;
        this.session = session;

        Keyring keyring = zebedee.keyringCache.get(session);
        if (keyring == null) throw new UnauthorizedException("No keyring is available for " + session.email);
    }

    /**
     * Write the given input stream to file. Ensure the content is encrypted if the collection is marked as encrypted.
     *
     * @param input
     * @param path
     * @throws BadRequestException
     * @throws IOException
     */
    @Override
    protected void write(InputStream input, Path path) throws BadRequestException, IOException {
        if (collection.description.isEncrypted) {
            SecretKey key = zebedee.keyringCache.get(session).get(collection.description.id);
            try (OutputStream output = EncryptionUtils.encryptionOutputStream(path, key)) {
                org.apache.commons.io.IOUtils.copy(input, output);
            }
        } else {
            try (OutputStream output = Files.newOutputStream(path)) {
                org.apache.commons.io.IOUtils.copy(input, output);
            }
        }
    }
}
