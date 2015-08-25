package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.content.dynamic.ContentNodeDetails;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageDescription;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import com.github.onsdigital.zebedee.util.URIUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getConfiguration;
import static com.github.onsdigital.zebedee.util.URIUtils.removeLastSegment;
import static com.github.onsdigital.zebedee.util.URIUtils.removeLeadingSlash;
import static java.nio.file.Files.*;

/**
 * Created by bren on 27/07/15.
 * <p>
 * ContentReader reads content and resource files from file system with given paths under a root content folder.
 * <p>
 * ContentReader will find file relative to root folder. Paths might start with forward slash or not.
 * <p>
 */
public class ContentReader {

    private final Path ROOT_FOLDER;
    private final String NOT_FOUND = "404 - Not Found";

    public ContentReader(String rootFolder) {
        this(StringUtils.isEmpty(rootFolder) ? null : Paths.get(rootFolder));
    }


    public ContentReader(Path rootFolder) {
        if (rootFolder == null) {
            throw new NullPointerException("Root folder can not be null");
        }
        this.ROOT_FOLDER = rootFolder;
    }

    /**
     * get json content under given path
     *
     * @param path path of requested content under given root folder
     * @return Wrapper containing actual document data as text
     */
    public Page getContent(String path) throws ZebedeeException, IOException {
        Path contentPath = resolveContentPath(path);
        return getPage(resolvePath(path), contentPath);
    }

    /**
     * @param path Should not have data file name at the end
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    private Page getContent(Path path) throws ZebedeeException, IOException {
        Path dataFile = resolveDataFilePath(path);
        return getPage(path, dataFile);
    }

    private Page getPage(Path path, Path dataFile) throws IOException, ZebedeeException {
        try (Resource resource = getResource(dataFile)) {
            checkJsonMime(resource, path);
            Page page = deserialize(resource);
            if (page != null) { //Contents without type is null when deserialised.
                if (StringUtils.endsWith(resource.getUri().toString(), ReaderConfiguration.getConfiguration().getDataFileName())) {
                    page.setUri(URI.create(removeLastSegment(resource.getUri().toString())));//Setting uri on the fly, discarding whatever is in the file
                } else {
                    page.setUri(URI.create(StringUtils.removeEnd(resource.getUri().toString(), ".json")));//Setting uri on the fly, discarding whatever is in the file
                }
            }
            return page;
        }
    }

    public Page getLatestContent(String path) throws ZebedeeException, IOException {
        Path contentPath = resolvePath(path);
        Path parent = contentPath.getParent();
        assertIsEditionsFolder(parent);
        return resolveLatest(contentPath);
    }

    /**
     * get resource
     *
     * @param path path of resource under root folder
     * @return Wrapper for resource stream
     */
    public Resource getResource(String path) throws ZebedeeException, IOException {
        Path content = resolvePath(path);
        return getResource(content);
    }

    /**
     * get child contents under given path, directories with no data.json are returned with no type and directory name as title
     *
     * @param path
     * @return uri - node mapping
     */
    public Map<URI, ContentNode> getChildren(String path) throws ZebedeeException, IOException {
        Path node = resolvePath(path);
        assertExists(node);
        assertIsDirectory(node);
        return resolveChildren(node);
    }

    /**
     * get parent contents of given path, directories are skipped, only contents upper in the hieararch are returned
     *
     * @param path path of the content or resource file
     * @return uri - node mapping, not in any particular order
     */
    public Map<URI, ContentNode> getParents(String path) throws ZebedeeException, IOException {
        Path node = resolvePath(path);
        if (!isDirectory(node)) { //resolve parents for resource files as well
            node = node.getParent();
        }
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
        Path dataFile = parent.resolve(getConfiguration().getDataFileName());
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

        //order by foldername, get the latest one
        String latestFolderName = null;
        Path latestFolderPath = null;

        try (DirectoryStream<Path> paths = newDirectoryStream(path)) {
            for (Path child : paths) {
                String name = child.getFileName().toString();
                if (latestFolderName == null || (name.compareTo(latestFolderName) > 0)) {
                    latestFolderName = name;
                    latestFolderPath = child;
                }
            }
        } catch (NoSuchFileException exception) {
            throw new NotFoundException(NOT_FOUND);
        }

        if (latestFolderPath == null) {
            throw new NotFoundException(NOT_FOUND);
        }
        return getContent(latestFolderPath);

    }

    //Returns uri of content calculating relative to root folder
    private URI toRelativeUri(Path node) {
        return URI.create("/" + URIUtils.removeTrailingSlash(getRootFolder().toUri().relativize(node.toUri()).getPath()));
    }

    protected Resource getResource(Path resourcePath) throws ZebedeeException, IOException {
        assertExists(resourcePath);
        assertNotDirectory(resourcePath);
        return buildResource(resourcePath);
    }

    protected Resource buildResource(Path path) throws IOException {
        Resource resource = new Resource();
        resource.setName(path.getFileName().toString());
        resource.setMimeType(probeContentType(path));
        resource.setUri(toRelativeUri(path));
        resource.setData(newInputStream(path));
        resource.setSize(size(path));
        return resource;
    }

    protected Page deserialize(Resource resource) {
        return ContentUtil.deserialiseContent(resource.getData());
    }


    protected void checkJsonMime(Resource resource, Path path) {
        String mimeType = resource.getMimeType();
        if (MediaType.APPLICATION_JSON.equals(mimeType) == false) {
            System.err.println("Warning!!!!! " + path + " mime type is not json, found mime type is :" + mimeType);
        }
        return;
    }

    private void assertExists(Path path) throws ZebedeeException {
        if (!exists(path)) {
            System.err.println("Could not find requested content, path:" + path.toUri().toString());
            throw new NotFoundException(NOT_FOUND);
        }
    }

    private void assertNotDirectory(Path path) throws BadRequestException {
        if (isDirectory(path)) {
            throw new BadRequestException("Requested path is a directory");
        }
    }

    private void assertIsDirectory(Path path) throws BadRequestException {
        if (!isDirectory(path)) {
            throw new BadRequestException("Requested uri is a directory");
        }
    }

    private void assertIsEditionsFolder(Path path) throws ZebedeeException {
        assertExists(path);
        assertIsDirectory(path);
        String fileName = path.getFileName().toString();
        if (getConfiguration().getBulletinsFolderName().equals(fileName) || getConfiguration().getArticlesFolderName().equals(fileName)) {
            return;
        }
        throw new BadRequestException("Latest uri can not be resolve for this content type");
    }

    private Path resolvePath(String path) throws BadRequestException {
        if (path == null) {
            throw new NullPointerException("Path can not be null");
        }
        return getRootFolder().resolve(removeLeadingSlash(path));
    }

    Path resolveContentPath(String path) throws BadRequestException {
        String jsonPath = URIUtils.removeTrailingSlash(path) + ".json";
        Path json = resolvePath(jsonPath);
        if (!exists(json)) {
            json = resolveDataFilePath(resolvePath(path));
        }
        return json;
    }

    private Path resolveDataFilePath(Path path) throws BadRequestException {
        return path.resolve(getConfiguration().getDataFileName());
    }

    /*Getters * Setters */
    private Path getRootFolder() {
        return ROOT_FOLDER;
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
        }

        return contentNode;
    }

    private ContentNode createContentNodeForFolder(Path path) {
        ContentNode contentNode = new ContentNode();
        contentNode.setUri(toRelativeUri(path));
        contentNode.setDescription(new ContentNodeDetails(path.getFileName().toString(), null));
        return contentNode;
    }

}
