package com.github.onsdigital.zebedee.model;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.zebedee.json.ContentDetail;
import com.github.onsdigital.zebedee.json.ContentDetailDescription;
import com.github.onsdigital.zebedee.model.content.item.VersionedContentItem;
import com.github.onsdigital.zebedee.util.ZebedeeCmsService;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logError;
import static com.github.onsdigital.zebedee.logging.ZebedeeLogBuilder.logInfo;
import static com.github.onsdigital.zebedee.model.PathUtils.findByCriteria;

public class Content {


    public static final String REDIRECT = "redirect.txt";
    public static final String DATA_VIS_DIR = "visualisations";
    public static final String TIME_SERIES_KEYWORD = "timeseries";

    private static final Predicate<Path> IS_DATA_VIZ_FILE = (p) -> p != null && p.toFile().isDirectory() &&
            DATA_VIS_DIR.equals(p.getFileName().toString());

    public final Path path;
    public final Path dataVisualisationsPath;

    public RedirectTablePartialMatch redirect = null;
    private Path publishedContentPath;

    public Content(Path path) {
        this.path = path;
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Path does not exist: " + path.toAbsolutePath());
        }

        this.dataVisualisationsPath = this.path.resolve(DATA_VIS_DIR);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Path does not exist: "
                    + dataVisualisationsPath.toAbsolutePath());
        }
    }

    /**
     * Create a new instance using an injected publishedContentPath.
     *
     * @param path
     * @param publishedContentPath
     */
    public Content(Path path, Path publishedContentPath) {
        this(path);
        this.publishedContentPath = publishedContentPath;
    }

    private static boolean isTimeseries(Path path) {
        return findByCriteria(path, p -> {
            return p.toFile().isDirectory() && TIME_SERIES_KEYWORD.equals(p.getFileName().toString());
        });
    }

    private static boolean isNotPreviousVersions(Path p) {
        return !VersionedContentItem.isVersionedUri(p.toString());
    }

    private static List<Path> listTimeSeriesDirectories(Path root) throws IOException {
        List<Path> result = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    if (entry.getFileName().toString().equals(TIME_SERIES_KEYWORD)) {
                        result.add(entry);
                        return result;
                    }
                    result.addAll(listTimeSeriesDirectories(entry));
                }
            }
        }

        return result;
    }

    public Path getPath() {
        return this.path;
    }

    boolean exists(URI uri) {
        return exists(uri.getPath());
    }

    boolean exists(URI uri, boolean doRedirect) {
        return exists(uri.getPath(), doRedirect);
    }

    boolean exists(String uri) {
        return exists(uri, true);
    }

    boolean exists(String uri, boolean doRedirect) {

        // Redirect if required
        String finalUri = uri;
        if (doRedirect && redirect != null) {
            finalUri = redirect.get(uri);
        }
        if (finalUri == null) {
            return false;
        }

        // Find whether exists
        Path path = toPath(finalUri);
        return Files.exists(path);
    }

    Path get(URI uri) {
        return get(uri.getPath(), true);
    }

    Path get(URI uri, boolean doRedirect) {
        return get(uri.getPath(), doRedirect);
    }

    /**
     * Returns a path for the specified uri
     * Uses redirect
     *
     * @param uri
     * @return
     */
    public Path get(String uri) {
        return get(uri, true);
    }

    /**
     * Returns a path for the specified uri
     *
     * @param uri        uri requested
     * @param doRedirect redirect if the uri is not found
     * @return
     */
    public Path get(String uri, boolean doRedirect) {

        // Redirect if necessary and possible
        String finalUri = uri;
        if (doRedirect && (redirect != null)) {
            finalUri = redirect.get(uri);
        }

        if (finalUri == null) {
            return null;
        }

        Path path = toPath(finalUri);
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
     * <p>
     * toPath does not redirect uri's
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
        listFilesRecursively(path, files, glob);

        // Convert to URIs:
        List<String> uris = new ArrayList<>();
        for (Path path : files) {
            if (!path.endsWith(REDIRECT)) { // Do not include redirect.txt in list of URIs
                uris.add(PathUtils.toUri(path));
            }
        }

        return uris;
    }

    /**
     * Returns a list of details with the details of child page details nested.
     *
     * @return
     * @throws IOException
     */
    public ContentDetail nestedDetails() throws IOException {
        return nestedDetails(path);
    }

    private ContentDetail nestedDetails(Path contentPath) throws IOException {
        ContentDetail detail = details(contentPath.resolve("data.json"));

        // if the folder is empty put in an empty node with just a name.
        if (detail == null) {
            detail = new ContentDetail();
            detail.description = new ContentDetailDescription(contentPath.getFileName().toString());
            detail.uri = "";
        }

        detail.contentPath = "/" + getPublishedContentPath().relativize(contentPath);
        detail.children = new ArrayList<>();

        // todo: remove timeseries filter once we are caching the browse tree.
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(contentPath)) {
            for (Path entry : stream) {
                if (isVisible(entry)) {
                    ContentDetail child = nestedDetails(entry);
                    if (child != null) {
                        detail.children.add(child);
                    }
                }
            }
        }

        try {
            if (detail.children.size() > 1) {
                java.util.Collections.sort(detail.children, (o1, o2) -> {

                    if ((o1.description == null || o1.description.title == null) && (o2.description == null || o2.description.title == null)) {
                        return 0; // if both are null
                    }
                    if (o1.description == null || o1.description.title == null) {
                        return 1;//nulls last
                    }
                    if (o2.description == null || o2.description.title == null) {
                        return -1;
                    }
                    return o1.description.title.compareTo(o2.description.title);
                });
            }
        } catch (IllegalArgumentException e) {
            logError(e, "Failed to sort content detail items").addParameter("path", contentPath.toString()).log();
        }

        return detail;
    }

    /**
     * Returns an individual {@link ContentDetail} object for the given uri.
     * This method only ever reads published content so assumes no decryption is required.
     *
     * @return
     * @throws IOException
     */
    ContentDetail details(Path path) throws IOException {
        ContentDetail result = null;
        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                result = Serialiser.deserialise(input, ContentDetail.class);
                if (result != null) {
                    result.uri = PathUtils.toUri(this.path.relativize(path.getParent()));
                } else {
                    logInfo("Failed to deserialise content details")
                            .addParameter("path", PathUtils.toUri(this.path.relativize(path.getParent())))
                            .log();
                }
            } catch (JsonSyntaxException exception) {
                logInfo("Failed to deserialise content details")
                        .addParameter("path", PathUtils.toUri(this.path.relativize(path.getParent())))
                        .log();
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
    private void listFilesRecursively(Path path, List<Path> files, String glob) throws IOException {
        files.addAll(listFiles(path, glob));

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    listFilesRecursively(entry, files, glob);
                }
            }
        }
    }

    /**
     * Return a list of files for the given path.
     *
     * @param path
     * @param glob
     * @return
     * @throws IOException
     */
    private List<Path> listFiles(Path path, String glob) throws IOException {

        List<Path> result = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, glob)) {
            for (Path entry : stream) {
                if (!Files.isDirectory(entry)) {
                    Path relative = this.path.relativize(entry);
                    if (!relative.toString().toLowerCase().endsWith(".ds_store")) // issue when in development on Mac's
                        result.add(relative);
                }
            }
        }

        return result;
    }

    /**
     * Return a list of time series folders within the content.
     * Note: this method was made specifically for time series as when it finds
     * a time series folder it does not recurse into it. This saves on unnecessary crawling
     * of the filesystem when we know there will be no time series folders nested under an existing one.
     *
     * @return
     * @throws IOException
     */
    public List<Path> listTimeSeriesDirectories() throws IOException {
        return listTimeSeriesDirectories(this.path);
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
            listFilesRecursively(folder, files, "*");
            if (files.size() == 0) { // If the folder is empty
                FileUtils.deleteDirectory(folder.toFile());
                folder = folder.getParent();
            } else {
                break;
            }
        }
    }

    boolean moveFile(String fromUri, String toUri) throws IOException {
        Path pathFrom = toPath(fromUri);
        Path pathTo = toPath(toUri);

        if (Files.exists(pathFrom)) { // If there is a file to be deleted
            FileUtils.moveDirectory(pathFrom.toFile(), pathTo.toFile());
            //Files.delete(pathFrom);
            deleteEmptyParentDirectories(pathFrom);
            return true;
        }
        return false;
    }

    static boolean isVisible(Path entry) {
        return Files.isDirectory(entry)
                && !isTimeseries(entry)
                && isNotPreviousVersions(entry)
                && !isDataVisSubDir(entry);
    }

    private static boolean isDataVisSubDir(Path path) {
        if (!path.toString().contains(DATA_VIS_DIR)) {
            return false;
        }
        return !DATA_VIS_DIR.equals(path.getFileName().toString().toLowerCase())
                && !DATA_VIS_DIR.equals(path.getParent().getFileName().toString().toLowerCase());
    }

    public Path getPublishedContentPath() {

        if (publishedContentPath == null)
            publishedContentPath = ZebedeeCmsService.getInstance().getZebedee().getPublishedContentPath();

        return publishedContentPath;
    }

    public static boolean isDataVisualisationFile(Path path) {
        return findByCriteria(path, IS_DATA_VIZ_FILE);
    }
}
