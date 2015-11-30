package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.Zebedee;
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

    public CollectionContentReader(Zebedee zebedee, Collection collection, Session session, Path rootFolder) {
        super(rootFolder);
        this.zebedee = zebedee;
        this.collection = collection;
        this.session = session;
    }

    @Override
    protected Resource buildResource(Path path) throws IOException {


        Resource resource = new Resource();
        resource.setName(path.getFileName().toString());

        InputStream inputStream;

        if (collection.description.isEncrypted) {
            inputStream = EncryptionUtils.encryptionInputStream(path, zebedee.keyringCache.get(session).get(collection.description.id));
        } else {
            inputStream = Files.newInputStream(path);
        }

        resource.setUri(toRelativeUri(path));
        resource.setData(inputStream);
        resource.setSize(size(path));
        return resource;
    }
}
