package com.github.onsdigital.zebedee.reader;

import com.github.onsdigital.zebedee.reader.model.ContentContainer;
import com.github.onsdigital.zebedee.reader.model.Resource;
import com.github.onsdigital.zebedee.reader.model.Document;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Created by bren on 27/07/15.
 * <p>
 * ContentReader provides interface to read documents and resource files accompanying these documents from various possible storage systems ( e.g. file system, no-sql db ,etc ).
 * Generic design allows possible changes on underlying system.
 * <p>
 * Implementation should be able to find documents using uri as unique key and handle taxonomy hierarchy and linking for documents
 * <p>
 */
public interface ContentReader {
    /**
     * get content identified with given uri
     *
     * @param uri
     * @return Wrapper containing actual document data as text
     */
    Document getDocument(URI uri) throws NotFoundException, IOException;

    /**
     *
     * get resource
     *
     * @param uri identifier for resource
     * @return Wrapper for resource stream
     */
    Resource getResource(URI uri);

    /**
     * get child containers under given uri
     *
     * @param uri
     * @return
     */
    List<ContentContainer> listContainers(URI uri);

}
