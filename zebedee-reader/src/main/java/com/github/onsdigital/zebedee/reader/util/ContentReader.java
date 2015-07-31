package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by bren on 27/07/15.
 * <p>
 * ContentReader reads content and resource files from file system with given paths under a root content folder
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

    /**
     * get json content under given path
     *
     * @param path path of requested content under given root folder
     * @return Wrapper containing actual document data as text
     */
    public Content getContent(String path) throws ZebedeeException, IOException {
        Resource resource = getResource(getRootFolder(), path);
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
       return getResource(getRootFolder(), path);
    }

    /**
     * get child contents under given path, directories with no data.json are returned as ContentList object with folder name as title of the content
     *
     * @param path
     * @return
     */
    public List<Content> listChildContents(String path) {
        return null;
    }

    protected Resource getResource(Path root, String path) throws ZebedeeException, IOException {
        Path content = resolvePath(root, path);
        checkExists(content);
        return buildResource(content);
    }

    protected Resource buildResource(Path path) throws IOException {
        Resource resource = new Resource();
        resource.setName(path.getFileName().toString());
        resource.setMimeType(Files.probeContentType(path));
        resource.setData(Files.newInputStream(path));
        return resource;
    }

    protected Content deserialize(Resource resource) {
        return ContentUtil.deserialiseContent(resource.getData());
    }



    protected void checkJsonMime(Resource resource, String path) {
        String mimeType = resource.getMimeType();
        if(MediaType.APPLICATION_JSON.equals(mimeType) == false ) {
            System.err.println("Warning!!!!! " + path  + " mime type is not json, found mime type is :" + mimeType);
        }
        return;
    }

    private void checkExists(Path path) throws ZebedeeException {
        if (!Files.exists(path) || Files.isDirectory(path)) {
            System.err.println("Could not find requested content, path:" + path.toUri().toString() );
            throw new NotFoundException("404 - Not Found");
        }
    }

    private Path resolvePath(Path root, String path) throws BadRequestException {
        if (path == null) {
            throw new NullPointerException("Path can not be null");
        }
        if (path.startsWith("/")) {
            throw new IllegalArgumentException("Absolute path requested, path must be relative to root folder, remove forward slash at the start?");
        }
        return root.resolve(path);
    }

    /*Getters * Setters */
    protected Path getRootFolder() {
        return ROOT_FOLDER;
    }


}
