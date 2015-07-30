package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by bren on 30/07/15.
 */
public class CollectionContentReader extends ContentReader {

    private final Path inprogressPath;
    private final Path completePath;
    private final Path reviewedPath;

    /**
     * @param collectionRoot path of collection including collection name
     */
    public CollectionContentReader(String collectionRoot) {
        super(collectionRoot);
        inprogressPath = getRootFolder().resolve(ReaderConfiguration.getInProgressFolderName());
        completePath = getRootFolder().resolve(ReaderConfiguration.getCompleteFolderName());
        reviewedPath = getRootFolder().resolve(ReaderConfiguration.getReviewedFolderName());
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
    public Content getContent(String path) throws ZebedeeException, IOException {
        Resource resource = findResource(path);
        checkJsonMime(resource, path);
        return deserialize(resource);
    }


    @Override
    public Resource getResource(String path) throws ZebedeeException, IOException {
        return findResource(path);
    }

    private Resource findResource(String path) throws IOException, ZebedeeException {
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

    // read content under given folder, if not found return null
    private Resource findResourceQuite(Path root, String path) throws IOException, ZebedeeException {
        try {
            return super.getResource(root, path);
        } catch (NotFoundException e) {
            return null;
        }
    }
}
