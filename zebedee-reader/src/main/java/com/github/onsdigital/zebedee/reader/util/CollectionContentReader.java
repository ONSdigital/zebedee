package com.github.onsdigital.zebedee.reader.util;

import com.github.onsdigital.zebedee.content.collection.Collection;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.content.util.ContentUtil;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by bren on 30/07/15.
 */
public class CollectionContentReader extends ContentReader {

    private Path inprogressPath;
    private Path completePath;
    private Path reviewedPath;

    private final String collectonId;

    /**
     * @param collectionsRootPath path of the collections folder
     */
    public CollectionContentReader(String collectionsRootPath, String collectionId) {
        super(collectionsRootPath);
        this.collectonId = collectionId;
    }

    /**
     * Reads content under a given collection root folder.
     * Tries finding content under in progress, completePath and reviewedPath folders respectively. Throws not found exception if not found
     *
     * @param path path of requested content under requested root folder
     * @return
     * @throws NotFoundException
     * @throws IOException
     */
    @Override
    public Page getContent(String path) throws ZebedeeException, IOException {
        Resource resource = findResource(path);
        checkJsonMime(resource, path);
        return deserialize(resource);
    }


    @Override
    public Resource getResource(String path) throws ZebedeeException, IOException {
        return findResource(path);
    }


    //TODO: If collection folder names were ids or we saved cookie as collection's name we would not have search collection, just read the path
    //Finds collection name with given id
    private String findCollectionName(Path collectionsRoot, String collectionId) throws IOException, NotFoundException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(collectionsRoot)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    continue;
                } else {
                    try (InputStream fileStream = Files.newInputStream(path)) {
                        Collection collection = ContentUtil.deserialise(fileStream, Collection.class);
                        if (StringUtils.equalsIgnoreCase(collection.getId(), this.collectonId)) {
                            return collection.getName();
                        }
                    }
                }
            }
            throw new NotFoundException("Collection with given id not found, id:" + collectionId);
        }
    }

    private Resource findResource(String path) throws IOException, ZebedeeException {
        String collectionName = findCollectionName(getRootFolder(), collectonId);
        resolvePaths(collectionName);
        Resource resource;
        resource = findResourceQuite(inprogressPath, path);
        if (resource == null) {
            //try completePath contents in collection
            resource = findResourceQuite(completePath, path);
            if (resource == null) {
                //throw not found if not found after reviewed path
                resource = super.getResource(reviewedPath, path);
            }
        }
        return resource;
    }

    private void resolvePaths(String collectionName) throws BadRequestException {
        Path collectionRoot = resolvePath(getRootFolder(), collectionName);
        inprogressPath = resolvePath(collectionRoot, ReaderConfiguration.getInstance().getInProgressFolderName());
        completePath = resolvePath(collectionRoot, ReaderConfiguration.getInstance().getCompleteFolderName());
        reviewedPath = resolvePath(collectionRoot, ReaderConfiguration.getInstance().getReviewedFolderName());
    }

    // read content under given folder, if not found return null
    private Resource findResourceQuite(Path root, String path) throws IOException, ZebedeeException {
        try {
            return super.getResource(root, path);
        } catch (NotFoundException e) {
            return null;
        }
    }
}
