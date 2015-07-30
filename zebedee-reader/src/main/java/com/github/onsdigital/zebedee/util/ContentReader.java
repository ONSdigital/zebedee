package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;

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
        checkMimeType(resource);
        return deserialize(resource);
    }

    protected Content deserialize(Resource resource) {
        return ContentUtil.deserialiseContent(resource.getData());
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

    protected Path getRootFolder() {
        return ROOT_FOLDER;
    }

    protected Resource getResource(Path root, String path) throws ZebedeeException, IOException {
        Path content = root.resolve(path);
        checkExists(content);
        return buildResource(content);
    }

    protected Resource buildResource(Path path) throws IOException {
        Resource resource = new Resource();
        resource.setName(path.getFileName().toString());
        resource.setData(Files.newInputStream(path));
        return resource;
    }

    protected void checkMimeType(Resource resource) {
        if("application/json".equals(resource.getMimeType()) == false ) {
            System.err.println("Warning!!!!! Resource requested as content does not seem to be a json file");
        }
        return;
    }

    private void checkExists(Path path) throws ZebedeeException, BadRequestException {
        if (!Files.exists(path)) {
            throw new NotFoundException("Can not find content, path:" + path.toUri().toString());
        } else if(Files.isDirectory(path)) {
            throw new BadRequestException("Requested path is a directory, path:" + path.toUri().toString());
        }
        return;

    }

}
