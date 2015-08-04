package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.page.base.PageType;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.*;

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
        Path content = resolveDataFilePath(getRootFolder(), path);
        Resource resource = getResource(content);
        checkJsonMime(resource, path);
        return deserialize(resource);
    }

    /**
     * get resource
     *
     * @param path path of resource under root folder
     * @return Wrapper for resource stream
     */
    public Resource getResource(String path) throws ZebedeeException, IOException {
        Path content = resolvePath(getRootFolder(), path);
        return getResource(content);
    }

    /**
     * get child contents under given path, directories with no data.json are returned with no type and directory name as title
     *
     * @param path
     * @return uri - node mapping
     */
    public Map<URI, ContentNode> getChildren(String path) throws ZebedeeException, IOException {
        Path node = resolvePath(getRootFolder(), path);
        assertExists(node);
        assertIsDirectory(node);
        return resolveChildren(node);
    }

    /**
     * get parent contents under of path, directories are skipped, only contents are returned
     *
     * @param path
     * @return uri - node mapping
     */
    public Map<URI, ContentNode> getParents(String path) throws ZebedeeException, IOException {
        Path node = resolvePath(getRootFolder(), path);
        assertExists(node);
        assertIsDirectory(node);
        return resolveParents(node);
    }

    private Map<URI, ContentNode> resolveParents(Path node) throws IOException, ZebedeeException {
        Map<URI, ContentNode> nodes = new HashMap<>();
        if (node.equals(getRootFolder())) {
            return Collections.emptyMap();
        }

        nodes.putAll(resolveParents(node.getParent()));

        Page parent = getParentContent(node);
        if (parent == null) {
            return Collections.emptyMap();
        }
        return nodes;
    }

    //Gets first parent content
    private Page getParentContent(Path node) throws ZebedeeException, IOException {
        Path parentNode = node.getParent();
        if (parentNode == null) {
            return null;
        }
        URI uri = toRelativeUri(parentNode);
        Page content;
        try {
            content = getContent(uri.toString());
        } catch (NotFoundException e) {//if parent is just a folder with data.json skips it
            content = getParentContent(parentNode);
        }

        return content;
    }


    private Map<URI, ContentNode> resolveChildren(Path node) throws IOException, ZebedeeException {
        Map<URI, ContentNode> nodes = new HashMap<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(node)) {
            for (Path child : paths) {
                if (Files.isDirectory(child)) {
                    Path dataFile = child.resolve(ReaderConfiguration.getConfiguration().getDataFileName());
                    URI uri = toRelativeUri(child);
                    if (Files.exists(dataFile)) {//data.json
                        try (Resource resource = getResource(dataFile)) {
                            Page content = deserialize(resource);
                            nodes.put(uri, new ContentNode(uri, content.getDescription().getTitle(), content.getType()));
                        }
                    } else {
                        //directory
                        nodes.put(uri, new ContentNode(uri, child.getFileName().toString(), null));
                    }
                } else {
                    continue;//skip data.json files in current directory
                }
            }
        }
        return nodes;
    }

    //Returns uri of content calculating relative to root folder
    private URI toRelativeUri(Path node) {
        return URI.create("/" + getRootFolder().toUri().relativize(node.toUri()).getPath());
    }

    protected Resource getResource(Path resource) throws ZebedeeException, IOException {
        assertExists(resource);
        return buildResource(resource);
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
            throw new NotFoundException("404 - Not Found");
        }
    }

    private void assertNotDirectory(Path path) throws BadRequestException {
        if (Files.isDirectory(path)) {
            throw new BadRequestException("Requested path is a not directory, not a content file");
        }
    }

    private void assertIsDirectory(Path path) throws BadRequestException {
        if (!Files.isDirectory(path)) {
            throw new BadRequestException("Requested uri is not a content uri");
        }
    }

    Path resolvePath(Path root, String path) throws BadRequestException {
        if (path == null) {
            throw new NullPointerException("Path can not be null");
        }
        return root.resolve(removeLeadingSlash(path));
    }

    Path resolveDataFilePath(Path root, String path) throws BadRequestException {
        return resolvePath(root, path).resolve(ReaderConfiguration.getConfiguration().getDataFileName());
    }

    /*Getters * Setters */
    private Path getRootFolder() {
        return ROOT_FOLDER;
    }

    private boolean isRoot(Path path) {
        return getRootFolder().equals(path);
    }

}
