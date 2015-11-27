package com.github.onsdigital.zebedee.util;

import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.Resource;
import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;
import com.github.onsdigital.zebedee.reader.util.CollectionReader;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.github.onsdigital.zebedee.reader.configuration.ReaderConfiguration.getConfiguration;

class ZebedeeCollectionReader implements CollectionReader {

    private Zebedee zebedee;
    private ContentReader inProgress;
    private ContentReader complete;
    private ContentReader reviewed;

    public ZebedeeCollectionReader(Zebedee zebedee, Collection collection, ContentLanguage language) {

        inProgress = getContentReader(zebedee.collections.path, getConfiguration().getInProgressFolderName());
        complete = getContentReader(zebedee.collections.path, getConfiguration().getCompleteFolderName());
        reviewed = getContentReader(zebedee.collections.path, getConfiguration().getReviewedFolderName());

        inProgress.setLanguage(language);
        reviewed.setLanguage(language);
        complete.setLanguage(language);
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
        return findContent(path);
    }


    @Override
    public Resource getResource(String path) throws ZebedeeException, IOException {
        return findResource(path);
    }

    /**
     * @param path
     * @return uri-node mapping
     * @throws ZebedeeException
     * @throws IOException
     */
    @Override
    public Map<URI, ContentNode> getChildren(String path) throws ZebedeeException, IOException {
        Map<URI, ContentNode> children = new HashMap<>();
        //TODO: Same document should not be in two different state, it should be safe to overwrite if it appears in multiple places?.
        // Is there a validation mechanism ? Might be needed
        children.putAll(getChildrenQuite(path, reviewed));
        children.putAll(getChildrenQuite(path, complete));//overwrites reviewed content if appears in both places
        children.putAll(getChildrenQuite(path, inProgress));//overwrites complete and reviewed content if appears in both places
        return children;
    }


    /**
     * @param path
     * @return uri-node mapping
     * @throws ZebedeeException
     * @throws IOException
     */
    @Override
    public Map<URI, ContentNode> getParents(String path) throws ZebedeeException, IOException {
        Map<URI, ContentNode> parents = new HashMap<>();
        //TODO: Same document should not be in two different state, it should be safe to overwrite if it appears in multiple places?.
        // Is there a validation mechanism ? Might be needed
        parents.putAll(getParentsQuite(path, reviewed));
        parents.putAll(getParentsQuite(path, complete));//overwrites reviewed content if appears in both places
        parents.putAll(getParentsQuite(path, inProgress));//overwrites complete and reviewed content if appears in both places
        return parents;
    }

    private Page findContent(String path) throws IOException, ZebedeeException {
        Page page = getContentQuite(path, inProgress);
        if (page == null) {
            page = getContentQuite(path, complete);
            if (page == null) {
                page = reviewed.getContent(path);
            }
        }
        return page;
    }


    private Resource findResource(String path) throws IOException, ZebedeeException {
        Resource resource = getQuite(path, inProgress);
        if (resource == null) {
            resource = getQuite(path, complete);
            if (resource == null) {
                resource = reviewed.getResource(path);
            }
        }
        return resource;
    }

    //If content not found with given reader do not shout
    private Resource getQuite(String path, ContentReader contentReader) throws ZebedeeException, IOException {
        try {
            return contentReader.getResource(path);
        } catch (NotFoundException e) {
            return null;
        }
    }


    private Page getContentQuite(String path, ContentReader contentReader) throws ZebedeeException, IOException {
        try {
            return contentReader.getContent(path);
        } catch (NotFoundException e) {
            return null;
        }
    }

    //If content not found with given reader do not shout
    private Page getLatestQuite(String path, ContentReader contentReader) throws ZebedeeException, IOException {
        try {
            return contentReader.getLatestContent(path);
        } catch (NotFoundException e) {
            return null;
        }
    }

    //If content not found with given reader do not shout
    private Map<URI, ContentNode> getChildrenQuite(String path, ContentReader contentReader) throws ZebedeeException, IOException {
        try {
            return contentReader.getChildren(path);
        } catch (NotFoundException e) {
            return Collections.emptyMap();
        }
    }

    //If content not found with given reader do not shout
    private Map<URI, ContentNode> getParentsQuite(String path, ContentReader contentReader) throws ZebedeeException, IOException {
        try {
            return contentReader.getParents(path);
        } catch (NotFoundException e) {
            return Collections.emptyMap();
        }
    }

    @Override
    public Page getLatestContent(String path) throws ZebedeeException, IOException {
        Page content = getLatestQuite(path, inProgress);
        if (content == null) {
            content = getLatestQuite(path, complete);
            if (content == null) {
                content = reviewed.getLatestContent(path);
            }
        }
        return content;
    }


    private ContentReader getContentReader(Path collectionPath, String folderName) {
        return new ContentReader(collectionPath.resolve(folderName));
    }

}
