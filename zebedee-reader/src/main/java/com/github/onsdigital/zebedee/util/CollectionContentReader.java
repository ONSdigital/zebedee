package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.content.base.Content;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.Resource;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.*;

/**
 * Created by bren on 30/07/15.
 */
public class CollectionContentReader extends ContentReader {

    private final Path inprogress;
    private final Path complete;
    private final Path reviewed;

    public CollectionContentReader(String rootFolder) {
        super(rootFolder);
        inprogress = getRootFolder().resolve(getInProgressFolderName());
        complete = getRootFolder().resolve(getCompleteFolderName());
        reviewed = getRootFolder().resolve(getReviewedFolderName());
    }

    /**
     * Reads content under a given collection root folder.
     * Tries finding content under in progress, complete and reviewed folders respectively. Throws not found exception if not found
     *
     * @param path path of requested content under requested root folder
     * @return
     * @throws NotFoundException
     * @throws IOException
     */
    @Override
    public Content getContent(String path) throws ZebedeeException, IOException {
        Resource resource = findResource(path);
        return deserialize(resource);
    }


    @Override
    public Resource getResource(String path) throws ZebedeeException, IOException {
        return findResource(path);
    }

    private Resource findResource(String path) throws IOException, ZebedeeException {
        Resource resource;
        resource = findResourceQuite(inprogress, path);
        if (resource == null) {
            //try complete contents in collection
            resource = findResourceQuite(complete, path);
            if (resource == null) {
                //throw not found if not found after reviewed as well
                resource = super.getResource(reviewed, path);
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
