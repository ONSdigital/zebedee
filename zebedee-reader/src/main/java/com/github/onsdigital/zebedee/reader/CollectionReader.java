package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.content.dynamic.browse.ContentNode;
import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.BadRequestException;
import com.github.onsdigital.zebedee.exceptions.NotFoundException;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.reader.data.language.ContentLanguage;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class CollectionReader {

    protected ContentReader inProgress;
    protected ContentReader complete;
    protected ContentReader reviewed;
    protected ContentReader root;

    protected boolean isEncrypted = false;

    public CollectionReader(boolean isEncrypted) {
        this.isEncrypted = isEncrypted;
    }

    public CollectionReader() {
    }

    public ContentReader getInProgress() {
        return inProgress;
    }

    public ContentReader getComplete() {
        return complete;
    }

    public ContentReader getReviewed() {
        return reviewed;
    }

    public ContentReader getRoot() {
        return root;
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
    public Page getContent(String path) throws ZebedeeException, IOException {
        return findContent(path);
    }


    public Resource getResource(String path) throws ZebedeeException, IOException {
        try {
            return findResource(path);
        } catch (BadRequestException e) {
            if(path.startsWith("/visualisations/")) {
                return findResource(path + "/index.html");
            }
            throw e;
        }
    }

    public long getContentLength(String path) throws ZebedeeException, IOException {
        long length = getContentLengthQuiet(path, inProgress);
        if (length == 0) {
            length = getContentLengthQuiet(path, complete);
            if (length == 0) {
                length = reviewed.getContentLength(path);
            }
        }
        return length;
    }

    /**
     * @param path
     * @return uri-node mapping
     * @throws ZebedeeException
     * @throws IOException
     */
    public Map<URI, ContentNode> getChildren(String path) throws ZebedeeException, IOException {
        Map<URI, ContentNode> children = new HashMap<>();
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
    public Map<URI, ContentNode> getParents(String path) throws ZebedeeException, IOException {
        Map<URI, ContentNode> parents = new HashMap<>();
        // Is there a validation mechanism ? Might be needed
        parents.putAll(getParentsQuite(path, reviewed));
        parents.putAll(getParentsQuite(path, complete));//overwrites reviewed content if appears in both places
        parents.putAll(getParentsQuite(path, inProgress));//overwrites complete and reviewed content if appears in both places
        return parents;
    }

    private Page findContent(String path) throws IOException, ZebedeeException {
        Page page = getContentQuiet(path, inProgress);
        if (page == null) {
            page = getContentQuiet(path, complete);
            if (page == null) {
                page = reviewed.getContent(path);
            }
        }
        return page;
    }

    private Resource findResource(String path) throws IOException, ZebedeeException {
        Resource resource = getQuiet(path, inProgress);
        if (resource == null) {
            resource = getQuiet(path, complete);
            if (resource == null) {
                resource = reviewed.getResource(path);
            }
        }
        return resource;
    }

    //If content not found with given reader do not shout
    private Resource getQuiet(String path, ContentReader contentReader) throws ZebedeeException, IOException {
        try {
            return contentReader.getResource(path);
        } catch (NotFoundException e) {
            return null;
        }
    }

    private long getContentLengthQuiet(String path, ContentReader contentReader) throws ZebedeeException, IOException {
        try {
            return contentReader.getContentLength(path);
        } catch (NotFoundException e) {
            return 0;
        }
    }

    private Page getContentQuiet(String path, ContentReader contentReader) throws ZebedeeException, IOException {
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

    public void setLanguage(ContentLanguage language) {
        inProgress.setLanguage(language);
        reviewed.setLanguage(language);
        complete.setLanguage(language);
    }
}