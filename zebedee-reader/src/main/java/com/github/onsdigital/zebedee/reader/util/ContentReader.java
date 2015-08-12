package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.content.dynamic.ContentNodeDetails;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getConfiguration;
import static com.github.onsdigital.zebedee.util.URIUtils.removeLeadingSlash;

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
        if (rootFolder == null) {
            throw new NullPointerException("Root folder can not be null");
        }
        this.ROOT_FOLDER = Paths.get(rootFolder);
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
        Path content = resolveDataFilePath(path);
        Resource resource = getResource(content);
        checkJsonMime(resource, path);
        Page page = deserialize(resource);
        page.setUri(URI.create(path));//Setting uri on the fly, discarding whatever is in the file
        return page;
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
     * get parent contents of given path, directories are skipped, only contents are returned
     *
     * @param path
     * @return uri - node mapping, not in any particular order
     */
    public Map<URI, ContentNode> getParents(String path) throws ZebedeeException, IOException {
        Path node = resolvePath(path);
        return resolveParents(node);
    }

    private Map<URI, ContentNode> resolveParents(Path node) throws IOException, ZebedeeException {
        Map<URI, ContentNode> nodes = new HashMap<>();

        if (isRootFolder(node)) {
            return Collections.emptyMap();
        }

        Page firstParent = getParentContent(node.getParent());
        if (firstParent == null) {
            return Collections.emptyMap();
        }

        nodes.putAll(resolveParents(resolvePath(firstParent.getUri().toString())));//resolve parent's parents first
        nodes.put(firstParent.getUri(), createContentNode(firstParent));
        return nodes;
    }

    //Gets first parent content
    private Page getParentContent(Path path) throws ZebedeeException, IOException {
        if (path == null) {
            return null;
        }

        Page content;
        try {
            content = getContent(toRelativeUri(path).toString());
        } catch (NotFoundException e) {//if parent is just a folder with data.json skips it
            if (isRootFolder(path)) { //if already at root don't go further up
                return null;
            }
            content = getParentContent(path.getParent());
        }

        return content;
    }

    private boolean isRootFolder(Path path) {
        return path.equals(getRootFolder());
    }


    private Map<URI, ContentNode> resolveChildren(Path node) throws IOException, ZebedeeException {
        Map<URI, ContentNode> nodes = new HashMap<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(node)) {
            for (Path child : paths) {
                if (Files.isDirectory(child)) {
                    Path dataFile = child.resolve(getConfiguration().getDataFileName());
                    URI uri = toRelativeUri(child);
                    if (Files.exists(dataFile)) {//data.json
                        try (Resource resource = getResource(dataFile)) {
                            Page content = deserialize(resource);
                            if (content == null) { //Contents without type is null when deserialised.
                                continue;
                            }
                            nodes.put(uri, createContentNode(content));
                        }
                    } else {
                        //directory
                        nodes.put(uri,createContentNodeForFolder(uri, child.getFileName().toString()) );
                    }
                } else {
                    continue;//skip data.json files in current directory
                }
            }
        }
        return nodes;
    }

    private Page resolveLatest(Path path) throws ZebedeeException, IOException {

        //order by foldername, get the latest one

        String latestFolderName = null;
        Path latestFolderPath = null;

        try (DirectoryStream<Path> paths = Files.newDirectoryStream(path)) {
            for (Path child : paths) {
                String name = child.getFileName().toString();
                if (latestFolderName == null || (name.compareTo(latestFolderName) == 1)) {
                    latestFolderName = name;
                    latestFolderPath = child;
                }
            }
        }

        if (latestFolderPath == null) {
            throw new NotFoundException(NOT_FOUND);
        }
        return getContent(toRelativeUri(latestFolderPath).toString());

    }

    //Returns uri of content calculating relative to root folder
    private URI toRelativeUri(Path node) {
        return URI.create("/" + getRootFolder().toUri().relativize(node.toUri()).getPath());
    }

    protected Resource getResource(Path resourcePath) throws ZebedeeException, IOException {
        assertExists(resourcePath);
        assertNotDirectory(resourcePath);
        return buildResource(resourcePath);
    }

    protected Resource buildResource(Path path) throws IOException {
        Resource resource = new Resource();
        resource.setName(path.getFileName().toString());
        resource.setMimeType(Files.probeContentType(path));
        resource.setData(Files.newInputStream(path));
        return resource;
    }

    protected Page deserialize(Resource resource) {
        return ContentUtil.deserialiseContent(resource.getData());
    }


    protected void checkJsonMime(Resource resource, String path) {
        String mimeType = resource.getMimeType();
        if (MediaType.APPLICATION_JSON.equals(mimeType) == false) {
            System.err.println("Warning!!!!! " + path + " mime type is not json, found mime type is :" + mimeType);
        }
        return;
    }

    private void assertExists(Path path) throws ZebedeeException {
        if (!Files.exists(path)) {
            System.err.println("Could not find requested content, path:" + path.toUri().toString());
            throw new NotFoundException(NOT_FOUND);
        }
    }

    private void assertNotDirectory(Path path) throws BadRequestException {
        if (Files.isDirectory(path)) {
            throw new BadRequestException("Requested path is a directory");
        }
    }

    private void assertIsDirectory(Path path) throws BadRequestException {
        if (!Files.isDirectory(path)) {
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

    Path resolvePath(String path) throws BadRequestException {
        if (path == null) {
            throw new NullPointerException("Path can not be null");
        }
        return getRootFolder().resolve(removeLeadingSlash(path));
    }

    Path resolveDataFilePath(String path) throws BadRequestException {
        return resolvePath(path).resolve(getConfiguration().getDataFileName());
    }

    /*Getters * Setters */
    private Path getRootFolder() {
        return ROOT_FOLDER;
    }

    private ContentNode createContentNode(Page page) {
        ContentNode contentNode = new ContentNode();
        contentNode.setUri(page.getUri());
        contentNode.setType(page.getType());
        contentNode.setDescriptions(new ContentNodeDetails(page.getDescription().getTitle(), page.getDescription().getEdition()));
        return contentNode;
    }

    private ContentNode createContentNodeForFolder(URI uri, String title) {
        ContentNode contentNode = new ContentNode();
        contentNode.setUri(uri);
        contentNode.setDescriptions(new ContentNodeDetails(title, null));
        return contentNode;
    }

}
