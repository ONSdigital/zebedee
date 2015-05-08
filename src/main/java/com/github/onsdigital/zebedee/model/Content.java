package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.ContentDetail;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Content {

    public final Path path;

    public Content(Path path) {
        this.path = path;
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Path does not exist: "
                    + path.toAbsolutePath());
        }
    }

    private static boolean isDirEmpty(final Path directory) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }

    boolean exists(URI uri) {
        return exists(uri.getPath());
    }

    boolean exists(String uri) {
        Path path = toPath(uri);
        return Files.exists(path);
    }

    Path get(URI uri) {
        return get(uri.getPath());
    }

    public Path get(String uri) {
        Path path = toPath(uri);
        Path result = null;
        if (Files.exists(path)) {
            result = path;
        }
        return result;
    }

    /**
     * Generates a {@link Path} that represents the given URI within this
     * {@link Content}. The {@link Path} is generated whether or not a file
     * actually exists, so this method is suitable for use when creating new
     * content.
     *
     * @param uri The URI of the item.
     * @return A {@link Path} to the [potential] location of the specified item.
     */
    public Path toPath(String uri) {
        String relative = uri;
        if (StringUtils.startsWith(uri, "/")) {
            relative = StringUtils.substring(uri, 1);
        }
        return path.resolve(relative);
    }

    /**
     * Returns a list of uri's for each file within this {@link Content}
     *
     * @return
     * @throws IOException
     */
    public List<String> uris() throws IOException {
        return uris("*");
    }

    /**
     * Returns a list of uri's for each file within this {@link Content}
     *
     * @param glob The filter glob to apply to the list.
     * @return
     * @throws IOException
     */
    public List<String> uris(String glob) throws IOException {

        // Get a list of files:
        List<Path> files = new ArrayList<>();
        listFiles(path, files, glob);

        // Convert to URIs:
        List<String> uris = new ArrayList<>();
        String uri;
        for (Path path : files) {
            uri = path.toString();
            if (!uri.startsWith("/")) {
                uri = "/" + uri;
            }
            uris.add(uri);
        }

        return uris;
    }

    /**
     * Returns a list of {@link ContentDetail} objects for each file within this {@link Content}
     *
     * @return
     * @throws IOException
     */
    public List<ContentDetail> details() throws IOException {
        List<ContentDetail> details = new ArrayList<>();
        for (String uri : this.uris("*data.json")) {
            details.add(details(uri));
        }
        return details;
    }

    /**
     * Returns an individual {@link ContentDetail} object for the given uri.
     *
     * @return
     * @throws IOException
     */
    public ContentDetail details(String uri) throws IOException {
        ContentDetail result = null;
        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(new File(path.toFile(), uri).toPath())) {
                result = Serialiser.deserialise(input, ContentDetail.class);
                result.uri = uri;
            }
        }
        return result;
    }

    /**
     * Recursively lists all files within this {@link Content}.
     *
     * @param path  The path to start from. This method calls itself recursively.
     * @param files The list to which results will be added.
     * @param glob  The filter glob to apply to the list.
     * @throws IOException
     */
    private void listFiles(Path path, List<Path> files, String glob) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, glob)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) {
                    Path relative = this.path.relativize(entry);
                    if (!relative.endsWith(".DS_Store")) // issue when in development on Mac's
                        files.add(relative);
                }
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    listFiles(entry, files, glob);
                }
            }
        }
    }

    public boolean delete(String uri) throws IOException {
        Path path = toPath(uri);

        if (Files.exists(path)) { // If there is a file to be deleted
            Files.delete(path);
            deleteEmptyParentDirectories(path);
            return true;
        }
        return false;
    }

    /**
     * Delete any empty directories left in the folder tree by walking up the folder structure
     *
     * @param path
     * @throws IOException
     */
    private void deleteEmptyParentDirectories(Path path) throws IOException {
        Path folder = path.getParent();
        while (!Files.isSameFile(this.path, folder)) { // Go no further than the Content root
            List<Path> files = new ArrayList<>();
            listFiles(folder, files, "*");
            if (files.size() == 0) { // If the folder is empty
                FileUtils.deleteDirectory(folder.toFile());
                folder = folder.getParent();
            } else {
                break;
            }
        }
    }
}
