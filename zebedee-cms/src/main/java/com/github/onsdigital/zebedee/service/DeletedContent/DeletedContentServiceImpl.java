package com.github.onsdigital.zebedee.service.DeletedContent;

import com.github.onsdigital.zebedee.content.page.base.Page;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.Collection;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.deleted.DeletedContentEvent;
import com.github.onsdigital.zebedee.persistence.dao.DeletedContentEventDao;
import com.github.onsdigital.zebedee.persistence.dao.impl.DeletedContentFileStore;
import com.github.onsdigital.zebedee.reader.ContentReader;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Handles the storing and retrieving of deleted content.
 */
public class DeletedContentServiceImpl implements DeletedContentService {

    private final DeletedContentEventDao deletedContentEventDao;
    private final DeletedContentFileStore deletedContentFileStore;

    public DeletedContentServiceImpl(DeletedContentEventDao deletedContentEventDao, DeletedContentFileStore deletedContentFileStore) {
        this.deletedContentEventDao = deletedContentEventDao;
        this.deletedContentFileStore = deletedContentFileStore;
    }

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
    @Override
    public void storeDeletedContent(Page deletedPage, Date dateDeleted, Set<String> files, ContentReader contentReader, Collection collection) throws ZebedeeException, IOException {

        // Create a new deleted content event.
        DeletedContentEvent deletedContentEvent = new DeletedContentEvent(
                collection.getDescription().getId(),
                collection.getDescription().getName(),
                dateDeleted,
                deletedPage.getUri().toString(),
                deletedPage.getDescription().getTitle(),
                files);

        // Store all the files that were deleted.
        deletedContentFileStore.storeFiles(deletedContentEvent, contentReader);

        // Persist the metadata for the deletions.
        deletedContentEventDao.saveDeletedContentEvent(deletedContentEvent);
    }

    /**
     * Return a list of the most recent deleted content events.
     * @return
     */
    @Override
    public List<DeletedContentEvent> listDeletedContent() {
        return deletedContentEventDao.listDeletedContent();
    }

    /**
     * Restore deleted content for the given deleted content event Id.
     * @param deletedContentEventId - The ID of the event to restore content for.
     * @param contentWriter - The instance of content writer to write the restored content to.
     * @return
     * @throws ZebedeeException
     * @throws IOException
     */
    @Override
    public DeletedContentEvent retrieveDeletedContent(long deletedContentEventId, ContentWriter contentWriter) throws ZebedeeException, IOException {

        // query the db to determine all uris to be restored.
        DeletedContentEvent deletedContentEvent = deletedContentEventDao.retrieveDeletedContent(deletedContentEventId);

        // read all the URIs from the filestore and write them to the content writer.
        deletedContentFileStore.retrieveFiles(deletedContentEvent, contentWriter);

        return deletedContentEvent;
    }
}
