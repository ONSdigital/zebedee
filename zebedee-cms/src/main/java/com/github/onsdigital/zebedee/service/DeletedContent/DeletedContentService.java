package com.github.onsdigital.zebedee.service.DeletedContent;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.deleted.DeletedContentEvent;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

public interface DeletedContentService {

    /**
     * Store deleted content for the given page.
     * @param deletedPage - The root page that is being deleted.
     * @param dateDeleted - The date the delete took place.
     * @param files - The set of files that are being deleted.
     * @param contentReader - A content reader that provides the files to be deleted.
     * @param collection - the collection that contains the files to delete.
     * @throws ZebedeeException
     * @throws IOException
     */
    void storeDeletedContent(Page deletedPage, Date dateDeleted, Set<String> files, ContentReader contentReader, Collection collection) throws ZebedeeException, IOException;

    /**
     * Return a list of the most recent deleted content events.
     * @return
     */
    List<DeletedContentEvent> listDeletedContent();

    /**
     * Restore deleted content for the given deleted content event Id.
     * @param deletedContentEventId - The ID of the event to restore content for.
     * @param contentWriter - The instance of content writer to write the restored content to.
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    DeletedContentEvent retrieveDeletedContent(long deletedContentEventId, ContentWriter contentWriter) throws ZebedeeException, IOException;
}
