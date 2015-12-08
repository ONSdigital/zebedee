package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.json.Keyring;
import com.github.onsdigital.zebedee.json.Session;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.EncryptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.Files.size;

/**
 * A content reader that handles encrypted files.
 */
public class CollectionContentReader extends ContentReader {

    private Zebedee zebedee;
    private Collection collection;
    private Session session;

    public CollectionContentReader(Zebedee zebedee, Collection collection, Session session, Path rootFolder) throws UnauthorizedException, IOException {
        super(rootFolder);
        this.zebedee = zebedee;
        this.collection = collection;
        this.session = session;

        Keyring keyring = zebedee.keyringCache.get(session);
        if (keyring == null) throw new UnauthorizedException("No keyring is available for " + session.email);
    }

    @Override
    protected Resource buildResource(Path path) throws IOException {
        Resource resource = new Resource();
        resource.setName(path.getFileName().toString());
        resource.setMimeType(determineMimeType(path));

        InputStream inputStream;

        if (collection.description.isEncrypted) {
            Keyring keyring = zebedee.keyringCache.get(session);
            if (keyring == null) throw new IOException("No keyring is available for " + session.email);
            inputStream = EncryptionUtils.encryptionInputStream(path, keyring.get(collection.description.id));
        } else {
            inputStream = Files.newInputStream(path);
        }

        resource.setUri(toRelativeUri(path));
        resource.setData(inputStream);
        resource.setSize(size(path));
        return resource;
    }
}
