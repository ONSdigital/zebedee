package com.github.onsdigital.zebedee.reader.file;

import com.github.onsdigital.zebedee.reader.Configuration;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.NotFoundException;
import com.github.onsdigital.zebedee.reader.model.ContentContainer;
import com.github.onsdigital.zebedee.reader.model.Document;
import com.github.onsdigital.zebedee.reader.model.Resource;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by bren on 28/07/15.
 *
 * ContentReader implementation based on file system.
 *
 * Uses local file system with default file system provider
 */
public class FileSystemReader implements ContentReader {

    @Override
    public Document getDocument(URI uri) throws NotFoundException, IOException {
        return null;
    }

    @Override
    public Resource getResource(URI uri) {
        return null;
    }

    @Override
    public List<ContentContainer> listContainers(URI uri) {
        return null;
    }

    private InputStream read(URI uri) throws NotFoundException, IOException {
        Path document = resolve(uri);
        return Files.newInputStream(document);
    }

    private Path resolve(URI uri) throws NotFoundException {
        Path folder = Paths.get(uri);
        Path document = folder.resolve(Configuration.getDataFileName());
        if (!Files.exists(document)) {
            throw new NotFoundException("Content not found, uri: " + uri);
        }
        return document;
    }
}
