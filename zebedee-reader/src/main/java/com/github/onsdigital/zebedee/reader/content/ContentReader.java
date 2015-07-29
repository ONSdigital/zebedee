package com.github.onsdigital.zebedee.reader.content;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.reader.NotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
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
class ContentReader {

    private final Path ROOT_FOLDER;

    ContentReader(URI rootFolder) {
        if (rootFolder == null) {
            throw new NullPointerException("Root folder can not be null");
        }
        this.ROOT_FOLDER = Paths.get(rootFolder);
    }

    /**
     * get json content under given path
     *
     * @param path path of given
     * @return Wrapper containing actual document data as text
     */
    Content getContent(String path) throws NotFoundException, IOException {
        InputStream stream = getStream(path);
        return ContentUtil.deserialiseContent(stream);
    }

    /**
     * get resource
     *
     * @param path path of resource under root folder
     * @return Wrapper for resource stream
     */
    InputStream getResource(String path) throws NotFoundException, IOException {
        return getStream(path);
    }

    /**
     * get child contents under given path, directories with no data.json are returned as ContentList object with folder name as title of the content
     *
     * @param path
     * @return
     */
    List<Content> listChildContents(String path) {
        return null;
    }


    private InputStream getStream(String path) throws NotFoundException, IOException {
        Path content = ROOT_FOLDER.resolve(path);
        checkExists(content);
        return Files.newInputStream(content);
    }


    private void checkExists(Path path) throws NotFoundException {
        if (!Files.exists(path)) {
            throw new NotFoundException("Can not find content, path:" + path.toUri().toString());
        }
        return;

    }

}
