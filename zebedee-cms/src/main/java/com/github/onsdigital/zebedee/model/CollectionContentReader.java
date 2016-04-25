package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.exceptions.UnauthorizedException;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.util.EncryptionUtils;
import org.apache.commons.io.IOUtils;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A content reader that handles encrypted files.
 */
public class CollectionContentReader extends FileSystemContentReader {

    private Collection collection;
    private SecretKey key;

    public CollectionContentReader(Collection collection, SecretKey key, Path rootFolder) throws UnauthorizedException, IOException {
        super(rootFolder);
        this.collection = collection;
        this.key = key;
    }

    @Override
    protected long calculateContentLength(Path path) throws IOException {
        if (collection.description.isEncrypted) {
            try (InputStream inputStream = EncryptionUtils.encryptionInputStream(path, key);
                 OutputStream outputStream = new ByteArrayOutputStream()) {
                return IOUtils.copy(inputStream, outputStream);
            }
        } else {
            return super.calculateContentLength(path);
        }
    }

    @Override
    protected Resource buildResource(Path path) throws IOException {
        Resource resource = new Resource();
        resource.setName(path.getFileName().toString());
        resource.setMimeType(determineMimeType(path));
        resource.setUri(toRelativeUri(path));
        resource.setData(getInputStream(path));
        return resource;
    }

    private InputStream getInputStream(Path path) throws IOException {
        InputStream inputStream;
        if (collection.description.isEncrypted) {
            inputStream = EncryptionUtils.encryptionInputStream(path, key);
        } else {
            inputStream = Files.newInputStream(path);
        }
        return inputStream;
    }
}
