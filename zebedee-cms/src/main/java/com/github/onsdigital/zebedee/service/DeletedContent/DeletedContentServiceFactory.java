package com.github.onsdigital.zebedee.service.DeletedContent;

import com.github.onsdigital.zebedee.api.Root;
import com.github.onsdigital.zebedee.configuration.Configuration;
import com.github.onsdigital.zebedee.persistence.dao.impl.DeletedContentEventDaoImpl;
import com.github.onsdigital.zebedee.persistence.dao.impl.DeletedContentFileStoreImpl;

public class DeletedContentServiceFactory {

    /**
     * Creates instances of DeletedContentService, handling feature switches and integration with zebedee.
     * @return
     */
    public static DeletedContentService createInstance() {
        DeletedContentService deletedContentService;

        if (Configuration.isAuditDatabaseEnabled() && Configuration.storeDeletedContent()) {
            deletedContentService = new DeletedContentServiceImpl(
                    new DeletedContentEventDaoImpl(),
                    new DeletedContentFileStoreImpl(Root.zebedee.getPath()));
        } else {
            deletedContentService = new DeletedContentServiceStub();
        }
        return deletedContentService;
    }

}
