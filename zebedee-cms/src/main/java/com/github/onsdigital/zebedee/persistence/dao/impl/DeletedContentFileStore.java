package com.github.onsdigital.zebedee.persistence.dao.impl;

import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.deleted.DeletedContentEvent;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;

public interface DeletedContentFileStore {

    /**
     * Store files defined in the given DeletedContentEvent, reading them from the given ContentReader.
     * @param deletedContentEvent
     * @param contentReader
     * @throws ZebedeeException
     * @throws IOException
     */
    void storeFiles(DeletedContentEvent deletedContentEvent, ContentReader contentReader) throws ZebedeeException, IOException;

    /**
     * Retrieves the files deleted as part of the given DeletedContentEvent. Writes the retrieved files to the given
     * ContentWriter.
     * @param deletedContentEvent
     * @param contentWriter
     */
    void retrieveFiles(DeletedContentEvent deletedContentEvent, ContentWriter contentWriter) throws ZebedeeException, IOException;
}
