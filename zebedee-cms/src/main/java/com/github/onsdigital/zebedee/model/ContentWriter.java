package com.github.onsdigital.zebedee.model;

import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.util.URIUtils.removeLeadingSlash;
import static java.nio.file.Files.isDirectory;

/**
 * Basic abstraction for writing content to file.
 */
public class ContentWriter {

    private final Path ROOT_FOLDER;

    /**
     * Create a new instance of ContentWriter to write content from the given root folder.
     *
     * @param rootFolder
     */
    public ContentWriter(Path rootFolder) {
        if (rootFolder == null) {
            throw new NullPointerException("Root folder can not be null");
        }
        this.ROOT_FOLDER = rootFolder;
    }

    /**
     * Write the given input stream to the given URI.
     *
     * @param input
     * @param uri
     * @throws BadRequestException
     * @throws IOException
     */
    public void write(InputStream input, String uri) throws IOException, BadRequestException {
        Path path = resolvePath(uri);
        write(input, path);
    }

    protected void write(InputStream input, Path path) throws BadRequestException, IOException {
        assertNotDirectory(path);

        try (OutputStream output = FileUtils.openOutputStream(path.toFile())) {
            org.apache.commons.io.IOUtils.copy(input, output);
        }
    }

    private void assertNotDirectory(Path path) throws BadRequestException {
        if (isDirectory(path)) {
            throw new BadRequestException("Requested path is a directory");
        }
    }

    private Path resolvePath(String path) {
        if (path == null) {
            throw new NullPointerException("Path can not be null");
        }
        return getRootFolder().resolve(removeLeadingSlash(path));
    }

    private Path getRootFolder() {
        return ROOT_FOLDER;
    }

}
