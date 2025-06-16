package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.dynamic.ContentNodeDetails;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.chart.Chart;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.image.Image;
import com.github.onsdigital.zebedee.content.page.statistics.document.figure.table.Table;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ResourceDirectoryNotFileException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.content.base.ContentLanguage;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.reader.util.ReleaseDateComparator;
import com.github.onsdigital.zebedee.util.PathUtils;
import com.github.onsdigital.zebedee.util.URIUtils;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.github.onsdigital.zebedee.logging.ReaderLogger.error;
import static com.github.onsdigital.zebedee.util.URIUtils.removeLastSegment;
import static com.github.onsdigital.zebedee.util.URIUtils.removeLeadingSlash;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Files.newInputStream;
import static java.nio.file.Files.size;
import static org.apache.commons.lang3.StringUtils.removeEnd;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;


/**
 * Created by bren on 27/07/15.
 * <p>
 * ContentReader reads content and resource files from file system with given paths under a root content folder.
 * <p>
 * ContentReader will find file relative to root folder. Paths may start with forward slash or not.
 * <p>
 */
public class FileSystemContentReader implements ContentReader {

    private static final Path EMPTY_PATH = Paths.get("");
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    /**
     * Apache TIKA is a library providing helper methods for determining the MIME type of files. Using this in place
     * of {@link Files#probeContentType(Path)} due to know issues and bugs depending on the OS its used on.
     */
    private static Tika tika = new Tika();

    private final Path rootFolder;
    protected ContentLanguage language = ContentLanguage.ENGLISH;
    private Tracer tracer = GlobalOpenTelemetry.getTracer("zebedee-reader", "");

    public FileSystemContentReader(Path rootFolder) {
        if (rootFolder == null || rootFolder.equals(EMPTY_PATH)) { 
            throw new NullPointerException("Root folder can not be null");
        }
        this.rootFolder = rootFolder;
    }

    /**
     * Determine the mime type of the file at the given path.
     *
     * @param path the file path of the content to check.
     * @return the MIME type for the file, (default is application/octet-stream).
     * @throws IOException error determining MIME type.
     */
    protected static String determineMimeType(Path path) throws IOException {
        return StringUtils.defaultIfEmpty(tika.detect(path), DEFAULT_MIME_TYPE);
    }

    /**
     * get json content under given path
     *
     * @param path path of requested content under given root folder
     * @return Wrapper containing actual document data as text
     */
    @Override
    public Page getContent(String path) throws ZebedeeException, IOException {
        Page page = null;
        Span span = tracer.spanBuilder("FileSystemContentReader.getContent()").startSpan();
        span.setAttribute("Path", path.toString());

        try (Scope scope = span.makeCurrent()) {
            //Resolve to see if requested content is latest content, if so return latest, otherwise requested file
            Path contentPath = resolveContentPath(path);
            if (!isRootFolder(contentPath)) {
                String parentPath = URIUtils.removeLastSegment(path);
                try {
                    Page latestContent = getLatestContent(parentPath);
                    if (toRelativeUri(contentPath.getParent()).equals(latestContent.getUri())) {
                        return latestContent;
                    }
                } catch (Exception e) {
                }
            }
            page = getPage(contentPath);
            if (page != null) {
                PageDescription description = page.getDescription();
                if (description != null) {
                    description.setLatestRelease(null); //overwrite existing latest flag if already in the data, might be old
                }
            }
        } catch(Throwable t) {
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
    
        return page;
    }

    /**
     * @param path Should not have data file name at the end
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    private Page getContent(Path path) throws ZebedeeException, IOException {
        Path dataFile = resolveDataFilePath(path);
        return getPage(dataFile);
    }

    private Page getPage(Path dataFile) throws IOException, ZebedeeException {
        Span span = tracer.spanBuilder("FileSystemContentReader.getPage()").startSpan();
        span.setAttribute("Path", dataFile.toString());
        Page page = null;

        try (Scope scope = span.makeCurrent()) {
            try (Resource resource = getResource(dataFile)) {
    //            checkJsonMime(resource, path);
                page = deserialize(resource);
                if (page == null) { //Contents without type is null when deserialised. There should not be no such data
                    return null;
                }
                String uri = resource.getUri().toString();
                page.setUri(resolveUri(uri, page));
             
            }
        } catch(Throwable t) {
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
        
        return page;
    }

    private URI resolveUri(String uriString, Page page) {
        URI uri;
        if (page instanceof Table || page instanceof Chart || page instanceof Image) {
            uri = URI.create(removeEnd(uriString, ".json"));
        } else {
            uri = URI.create(removeLastSegment(uriString));
        }
        return uri;
    }

    @Override
    public Page getLatestContent(String path) throws ZebedeeException, IOException {
        Span span = tracer.spanBuilder("FileSystemContentReader.getLatestContent()").startSpan();
        span.setAttribute("Path", path.toString());
        Page page = null;

        try (Scope scope = span.makeCurrent()) {
            Path contentPath = resolvePath(path);
            Path parent = contentPath.getParent();
            assertIsEditionsFolder(parent);
            page = resolveLatest(contentPath);
            if (StringUtils.isBlank(page.getDescription().getMigrationLink())){
                page.getDescription().setLatestRelease(true);
            }
        } catch(Throwable t) {
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
        return page;
    }

    /**
     * get resource
     *
     * @param path path of resource under root folder
     * @return Wrapper for resource stream
     */
    @Override
    public Resource getResource(String path) throws ZebedeeException, IOException {
        Path content = resolvePath(path);
        return getResource(content);
    }

    /**
     * Determine the content length of the file at the given path.
     *
     * @param path
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    @Override
    public long getContentLength(String path) throws ZebedeeException, IOException {
        Span span = tracer.spanBuilder("FileSystemContentReader.getContentLength()").startSpan();
        span.setAttribute("Path", path.toString());
        long length = 0L;

        try (Scope scope = span.makeCurrent()) {
            Path resourcePath = resolvePath(path);
            assertExists(resourcePath);
            assertNotDirectory(resourcePath);
            length = calculateContentLength(resourcePath);
        }
        catch(Throwable t) {
            span.recordException(t);
        throw t;
        } finally {
            span.end();
        } 
        return length;
    }

    protected long calculateContentLength(Path path) throws IOException {
        return size(path);
    }

    /**
     * get child contents under given path, directories with no data.json are returned with no type and directory name as title
     *
     * @param path
     * @return uri - node mapping
     */
    @Override
    public Map<URI, ContentNode> getChildren(String path) throws ZebedeeException, IOException {
        Path node = resolvePath(path);
        assertExists(node);
        assertIsDirectory(node);
        return resolveChildren(node);
    }

    @Override
    public DirectoryStream<Path> getDirectoryStream(String path) throws BadRequestException, IOException {
        Path node = resolvePath(path);
        return newDirectoryStream(node);
    }

    @Override
    public DirectoryStream<Path> getDirectoryStream(String path, String filter) throws
            BadRequestException, IOException {
        Path node = resolvePath(path);
        return newDirectoryStream(node, filter);
    }

    /**
     * get parent contents of given path, directories are skipped, only contents upper in the hierarchy are returned
     *
     * @param path path of the content or resource file
     * @return uri - node mapping, not in any particular order
     */
    @Override
    public Map<URI, ContentNode> getParents(String path) throws ZebedeeException, IOException {
        Path node = resolvePath(path);
        return resolveParents(node);
    }

    private Map<URI, ContentNode> resolveParents(Path node) throws IOException, ZebedeeException {
        Map<URI, ContentNode> nodes = new HashMap<>();
        if (isRootFolder(node)) {
            return Collections.emptyMap();
        }
        Path firstParent = getFirstParentWithDataFile(node);
        if (firstParent == null) {
            return Collections.emptyMap();
        }

        nodes.putAll(resolveParents(firstParent));//resolve parent's parents first
        ContentNode contentNode = createContentNode(firstParent);
        nodes.put(contentNode.getUri(), contentNode);

        return nodes;
    }

    //Gets first parent content
    private Path getFirstParentWithDataFile(Path path) throws ZebedeeException, IOException {
        if (path == null) {
            return null;
        }

        Path parent = path.getParent();
        Path dataFile = resolveDataFilePath(parent);
        if (exists(dataFile)) {
            return parent;
        } else {
            if (isRootFolder(parent)) { //if already at root don't go further up
                return null;
            }
            return getFirstParentWithDataFile(parent);
        }
    }

    private boolean isRootFolder(Path path) {
        return path.equals(getRootFolder());
    }

    private Map<URI, ContentNode> resolveChildren(Path node) throws IOException, ZebedeeException {
        Map<URI, ContentNode> nodes = new HashMap<>();
        try (DirectoryStream<Path> paths = newDirectoryStream(node)) {
            for (Path child : paths) {
                if (isDirectory(child)) {
                    ContentNode contentNode = createContentNode(child);
                    if (contentNode == null) {
                        continue;
                    }
                    nodes.put(contentNode.getUri(), contentNode);
                } else {
                    continue;//skip all other files in current directory
                }
            }
        }
        return nodes;
    }

    private Page resolveLatest(Path path) throws ZebedeeException, IOException {

        Map<URI, ContentNode> children = resolveChildren(path);
        if (children == null || children.isEmpty()) {
            return null;
        }

        Set<ContentNode> sortedSet = sortByDate(children.values());
        return getPage(resolveDataFilePath(resolvePath(sortedSet.iterator().next().getUri().toString())));
    }

    private Set<ContentNode> sortByDate(Collection<ContentNode> set) {
        Set<ContentNode> valueSet = new TreeSet<>(new ReleaseDateComparator());
        valueSet.addAll(set);
        return valueSet;
    }

    //Returns uri of content calculating relative to root folder
    protected URI toRelativeUri(Path node) {
        Path rootFolder = getRootFolder();
        return PathUtils.toRelativeUri(rootFolder, node);
    }

    protected Resource getResource(Path resourcePath) throws ZebedeeException, IOException {
        assertExists(resourcePath);
        assertNotDirectory(resourcePath);
        return buildResource(resourcePath);
    }

    protected Resource buildResource(Path path) throws IOException {
        Span span = tracer.spanBuilder("FileSystemContentReader.buildResource()").startSpan();
        span.setAttribute("Path", path.toString());
        Resource resource = null;
        try (Scope scope = span.makeCurrent()) {
            resource = new Resource();
            resource.setName(path.getFileName().toString());
            resource.setMimeType(determineMimeType(path));
            resource.setUri(toRelativeUri(path));
            resource.setData(newInputStream(path));
        }
        catch(Throwable t) {
            span.recordException(t);
        throw t;
        } finally {
            span.end();
        }
        return resource;
    }

    protected Page deserialize(Resource resource) {

        Span span = tracer.spanBuilder("FileSystemContentReader.deserialize()").startSpan();
        span.setAttribute("Resource", resource.toString());
        Page page = null;
        try (Scope scope = span.makeCurrent()) {
            try {
                page = ContentUtil.deserialiseContent(resource.getData());
            } catch (JsonSyntaxException e) {
                throw error().data("resource_uri", resource.getUri())
                        .logException(e, "Failed to deserialise resource");
            }
        }
        catch(Throwable t) {
            span.recordException(t);
        throw t;
        } finally {
            span.end();
        }
        return page;

    }

    private void assertExists(Path path) throws ZebedeeException, IOException {
        if (!exists(path) || !isChild(path)) {
            throw new NotFoundException("Could not find requested content, path:" + path.toUri().toString());
        }
    }

    /**
     * Checks whether given path is a child of root folder or not to limit access to files outside content folder using relative paths
     *
     * @return
     */
    private boolean isChild(Path path) throws IOException {
        return path.toFile().getCanonicalPath().startsWith(getRootFolder().toFile().getCanonicalPath());
    }

    private void assertNotDirectory(Path path) throws BadRequestException {
        if (isDirectory(path)) {
            throw new ResourceDirectoryNotFileException("Requested path is a directory");
        }
    }

    private void assertIsDirectory(Path path) throws BadRequestException {
        if (!isDirectory(path)) {
            throw new BadRequestException("Requested uri is a directory");
        }
    }

    private void assertIsEditionsFolder(Path path) throws ZebedeeException, IOException {
        assertExists(path);
        assertIsDirectory(path);
        String fileName = path.getFileName().toString();
        ReaderConfiguration cfg = ReaderConfiguration.get();
        if (cfg.getBulletinsFolderName().equals(fileName) ||
                cfg.getArticlesFolderName().equals(fileName) ||
                cfg.getCompendiumFolderName().equals(fileName)) {
            return;
        }
        throw new BadRequestException("Latest uri can not be resolved for this content type");
    }

    private Path resolvePath(String path) {
        if (path == null) {
            throw new NullPointerException("Path can not be null");
        }
        return getRootFolder().resolve(removeLeadingSlash(path));
    }

    private Path resolveContentPath(String path) {
        String jsonPath = URIUtils.removeTrailingSlash(path) + ".json";
        Path json = resolvePath(jsonPath);
        if (!exists(json)) {
            json = resolveDataFilePath(resolvePath(path));
        }
        return json;
    }

    protected Path resolveDataFilePath(Path path) {
        Path dataFilePath = path.resolve(language.getDataFileName());
        if (!exists(dataFilePath)) {
            dataFilePath = path.resolve(ContentLanguage.ENGLISH.getDataFileName());
        }
        assertRelative(dataFilePath);
        return dataFilePath;
    }

    /**
     * Asserts requested file is under content folder
     *
     * @param dataFilePath
     */
    protected void assertRelative(Path dataFilePath) {
        dataFilePath.normalize();
    }

    /*Getters * Setters */
    @Override
    public Path getRootFolder() {
        return rootFolder;
    }


    //Creates content node from content if data file is available, otherwise creates content node using folder name
    private ContentNode createContentNode(Path path) throws ZebedeeException, IOException {
        ContentNode contentNode = null;
        try {
            Page content = getContent(path);
            if (content != null) {
                contentNode = new ContentNode();
                contentNode.setUri(content.getUri());
                contentNode.setType(content.getType());
                PageDescription description = content.getDescription();
                if (description != null) {
                    contentNode.setDescription(new ContentNodeDetails(description.getTitle(), description.getEdition()));
                    contentNode.getDescription().setReleaseDate(description.getReleaseDate());
                }
            }
        } catch (NotFoundException e) {
            contentNode = createContentNodeForFolder(path);
        } catch (JsonSyntaxException e) {
            error().data("path", path.toString()).logException(e, "Warning!!! Invalid json file encountered");
        }

        return contentNode;
    }

    private ContentNode createContentNodeForFolder(Path path) {
        ContentNode contentNode = new ContentNode();
        contentNode.setUri(toRelativeUri(path));
        contentNode.setDescription(new ContentNodeDetails(path.getFileName().toString(), null));
        return contentNode;
    }

    @Override
    public ContentLanguage getLanguage() {
        return language;
    }

    @Override
    public void setLanguage(ContentLanguage language) {
        if (language != null) {
            this.language = language;
        }
    }

    @Override
    public List<String> listUris() {
        List<String> uris = new ArrayList<>();
        Path root = this.getRootFolder();

        try {
            Files.walkFileTree(this.getRootFolder(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(
                        Path file,
                        BasicFileAttributes attrs
                )
                        throws IOException {
                    uris.add(PathUtils.toRelativeUri(root, file).toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return uris;
    }

    @Override
    public List<Path> listTimeSeriesDirectories() {
        List<Path> directories = new ArrayList<>();

        try {
            Files.walkFileTree(this.getRootFolder(), new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult postVisitDirectory(
                        Path dir,
                        IOException exc
                ) {
                    if (dir.endsWith("timeseries")) {
                        directories.add(dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return directories;
    }
}
